package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.SavedPlayerAssets;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A positive, LIMITED technology experiment on one recipient's cooperating
 * writer epoch, using actual production GrantLedger and saved-NBT readback.
 *
 * This test class owns the experimental gate, not the production plugin.
 * All-writer completeness, Paper saveData, external plugin writers and
 * operating-system power-loss semantics are outside this proof.
 */
final class CooperativeWriterDrainReadbackTest {
    @TempDir Path temp;

    /**
     * No lock is held while waiting for existing permits. A coordinator
     * could await this Future on a separate worker without freezing Paper's
     * main thread; only the drain-complete result allows the exclusive phase.
     */
    private static final class ExperimentalWriterEpoch {
        private boolean closing;
        private int inFlight;
        private CompletableFuture<Void> drained;

        synchronized Permit enterWriter() {
            if (closing) return null;
            inFlight++;
            return new Permit(this);
        }

        synchronized CompletableFuture<Void> closeAndDrain() {
            if (closing) throw new IllegalStateException("An epoch is already closed");
            closing = true;
            drained = new CompletableFuture<>();
            if (inFlight == 0) drained.complete(null);
            return drained;
        }

        private synchronized void finishWriter() {
            if (inFlight <= 0) throw new IllegalStateException("No writer to exit");
            inFlight--;
            if (closing && inFlight == 0) drained.complete(null);
        }

        synchronized void reopen() {
            if (!closing || inFlight != 0 || !drained.isDone())
                throw new IllegalStateException("Cannot reopen before writer drain");
            closing = false;
            drained = null;
        }

        private static final class Permit implements AutoCloseable {
            private final ExperimentalWriterEpoch parent;
            private boolean released;
            Permit(ExperimentalWriterEpoch parent) { this.parent = parent; }

            @Override public synchronized void close() {
                if (released) throw new IllegalStateException("Writer permit released twice");
                released = true;
                parent.finishWriter();
            }
        }
    }

    private Path dataFile(UUID player) throws IOException {
        Path file = temp.resolve("world/playerdata").resolve(player + ".dat");
        Files.createDirectories(file.getParent());
        return file;
    }

    /** Real compressed NBT bytes; same material shape as production Paper. */
    private void write(UUID player, GrantReceiptState receipt, boolean includeIssuedCoin)
            throws IOException {
        Path file = dataFile(player);
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(file));
             var out = new DataOutputStream(gzip)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            byte[] receiptBytes = PaperGrantReceiptPdc.toSavedBytes(receipt);
            out.writeByte(7); out.writeUTF("simplecoins:grant_receipt_v1");
            out.writeInt(receiptBytes.length); out.write(receiptBytes);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("Inventory");
            out.writeByte(10); out.writeInt(includeIssuedCoin ? 2 : 1);
            if (includeIssuedCoin) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(1);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(8); out.writeUTF("minecraft:item_model");
                out.writeUTF("simplecoins:coin_5000");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:coin_denomination");
                out.writeInt(5000);
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            // Existing unrelated item must be preserved by the grant.
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(9);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:stone");
            out.writeByte(3); out.writeUTF("count"); out.writeInt(2);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("EnderItems");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
    }

    private WorldStorageDiscovery.Manifest discover() throws IOException {
        Path world = temp.resolve("world");
        return WorldStorageDiscovery.discover(Map.of("world", world),
                new WorldScope(true, Set.of(), Set.of()), 256);
    }

    private static PaperGrantInventorySlotPlan plan(GrantLedger ledger,
            UUID player, GrantLedger.Grant grant) {
        List<String> slots = new ArrayList<>(Collections.nCopies(36, "occupied"));
        slots.set(0, null);
        return PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(slots)).orElseThrow();
    }

    private void forceFileAndParent(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            channel.force(true);
        }
        try (FileChannel parent = FileChannel.open(file.getParent(),
                StandardOpenOption.READ)) {
            parent.force(true);
        }
    }

    @Test void pendingWriterMustDrainBeforeAuthoritativeReadbackAck() throws Exception {
        UUID player = UUID.randomUUID();
        Path journalFolder = temp.resolve("ledger");
        GrantLedger journal = new GrantLedger(journalFolder);
        GrantLedger.Grant grant = journal.reserve(player, 5000, "a02-test");
        PaperGrantInventorySlotPlan placement = plan(journal, player, grant);
        ExperimentalWriterEpoch epoch = new ExperimentalWriterEpoch();
        CountDownLatch admitted = new CountDownLatch(1);
        CountDownLatch mayFinish = new CountDownLatch(1);

        try (ExecutorService writers = Executors.newSingleThreadExecutor()) {
            Future<?> olderWriter = writers.submit(() -> {
                try (var writerPermit = epoch.enterWriter()) {
                    if (writerPermit == null)
                        throw new IllegalStateException("Old writer should have a permit");
                    admitted.countDown();
                    if (!mayFinish.await(10, TimeUnit.SECONDS))
                        throw new IllegalStateException("Timed out before writing old bytes");
                    write(player, GrantReceiptState.empty(), false);
                } catch (Exception error) {
                    throw new RuntimeException(error);
                }
            });

            assertTrue(admitted.await(10, TimeUnit.SECONDS));
            // This is BEFORE any grant file/receipt publication, and an old
            // writer has crossed the entry point. Freezing NEW admissions alone
            // does NOT imply the recipient's saved source has stopped changing.
            CompletableFuture<Void> drained = epoch.closeAndDrain();
            assertFalse(drained.isDone(), "In-flight writer was not drained");
            assertNull(epoch.enterWriter(), "New cooperative writer must be denied");
            assertEquals(1, journal.pending(player).size());
            assertFalse(journal.historySnapshot().getFirst().delivered());

            // The old writer is allowed to finish BEFORE proof/ACK.
            mayFinish.countDown();
            olderWriter.get(10, TimeUnit.SECONDS);
            drained.get(10, TimeUnit.SECONDS);
            assertNull(epoch.enterWriter(), "Fence must remain closed through ACK");

            Path file = dataFile(player);
            Map<String, Object> before =
                    PlayerDataReader.read(file, 65536, 65536);
            write(player, placement.nextReceipt(), true);
            forceFileAndParent(file);
            byte[] savedBeforeAck = Files.readAllBytes(file);
            var proof = SavedGrantDeliveryFileReadback.verify(
                    discover(), journal, player, placement, before, 65536, 65536);
            assertEquals(BigInteger.valueOf(5000), proof.matchedMonetaryValue());
            assertEquals(placement.nextReceipt(), proof.receipt());
            assertEquals(1, journal.pending(player).size());
            assertNull(epoch.enterWriter(), "No cooperating writer may start after readback");

            journal.acknowledge(grant, proof.receipt().completedSequence());
            GrantLedger reopened = new GrantLedger(journalFolder);
            assertTrue(reopened.pending(player).isEmpty());
            assertEquals(1, reopened.historySnapshot().size());
            assertTrue(reopened.historySnapshot().getFirst().delivered());
            var physical = SavedPlayerAssets.extract(
                    PlayerDataReader.read(file, 65536, 65536));
            assertEquals(BigInteger.valueOf(5000), physical.get("inventory:0"));
            assertArrayEquals(savedBeforeAck, Files.readAllBytes(file));

            journal.acknowledge(grant, proof.receipt().completedSequence());
            assertArrayEquals(savedBeforeAck, Files.readAllBytes(file));
            assertTrue(new GrantLedger(journalFolder).pending(player).isEmpty());
            epoch.reopen();
        }
    }

    @Test void multipleInFlightMustDrainAndReopenOnlyAfterBarrierRelease() throws Exception {
        ExperimentalWriterEpoch epoch = new ExperimentalWriterEpoch();
        var first = epoch.enterWriter();
        var second = epoch.enterWriter();
        assertNotNull(first);
        assertNotNull(second);
        var closing = epoch.closeAndDrain();
        assertFalse(closing.isDone());
        assertNull(epoch.enterWriter());
        assertThrows(IllegalStateException.class, epoch::reopen);

        first.close();
        assertFalse(closing.isDone(), "One active writer remains");
        assertNull(epoch.enterWriter());
        second.close();
        assertTrue(closing.isDone());
        assertNull(epoch.enterWriter(), "Even drained fence stays closed until ACK");
        epoch.reopen();
        try (var futureWriter = epoch.enterWriter()) {
            assertNotNull(futureWriter, "After ACK, regular transfers may resume");
        }
        assertThrows(IllegalStateException.class, second::close,
                "Each cooperative permit is single-use");
    }
}
