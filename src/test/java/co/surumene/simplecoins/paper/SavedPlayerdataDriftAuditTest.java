package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedPlayerdataDriftAuditTest {
    @TempDir Path temp;

    private Path world() throws IOException {
        Path world = temp.resolve("world");
        Files.createDirectories(world);
        return world;
    }

    private Path save(UUID player, GrantReceiptState state) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(output)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            byte[] receipt = PaperGrantReceiptPdc.toSavedBytes(state);
            out.writeByte(7); out.writeUTF("simplecoins:grant_receipt_v1");
            out.writeInt(receipt.length); out.write(receipt);
            out.writeByte(0);
            out.writeByte(0);
        }
        Path saved = world().resolve("playerdata/" + player + ".dat");
        Files.createDirectories(saved.getParent());
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(saved))) {
            gzip.write(output.toByteArray());
        }
        return saved;
    }

    private WorldStorageDiscovery.Manifest discover() throws IOException {
        return WorldStorageDiscovery.discover(
                Map.of("world", world()), new WorldScope(true, Set.of(), Set.of()), 256);
    }

    @Test void sameSizeAndRestoredMtimeCannotMaskSavedPlayerdataReplacement() throws Exception {
        UUID player = UUID.randomUUID();
        Path source = save(player, GrantReceiptState.empty());
        var manifest = discover();
        var witness = SavedPlayerdataContentWitness.capture(manifest, 65536);
        byte[] original = Files.readAllBytes(source);
        FileTime timestamp = Files.getLastModifiedTime(source);
        byte[] changed = original.clone();
        changed[changed.length / 2] ^= 1;
        Files.write(source, changed);
        Files.setLastModifiedTime(source, timestamp);
        assertEquals(original.length, Files.size(source));
        assertEquals(timestamp, Files.getLastModifiedTime(source));
        assertThrows(IOException.class, witness::requireUnchanged);
    }


    @Test void restoredOriginalBytesAndMtimeMustStillInvalidateSavedPlayerdataWitness()
            throws Exception {
        UUID player = UUID.randomUUID();
        Path source = save(player, GrantReceiptState.empty());
        var witness = SavedPlayerdataContentWitness.capture(discover(), 65536);
        byte[] original = Files.readAllBytes(source);
        FileTime timestamp = Files.getLastModifiedTime(source);
        byte[] changed = original.clone();
        changed[changed.length / 2] ^= 1;
        Files.write(source, changed);
        Files.write(source, original);
        Files.setLastModifiedTime(source, timestamp);
        assertArrayEquals(original, Files.readAllBytes(source));
        assertEquals(timestamp, Files.getLastModifiedTime(source));
        // Original SHA-256, length, fileKey and mtime all agree. Unix ctime
        // must reveal the intervening rewrite; this is not a global freeze.
        assertThrows(IOException.class, witness::requireUnchanged);
    }

    @Test void auditedReceiptMustNotPassAfterSamePathFileRollback() throws Exception {
        var ledger = new GrantLedger(temp.resolve("grants"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        ledger.acknowledge(grant, grant.sequence());
        save(player, new GrantReceiptState(grant.sequence(), 0, null, 0));
        var manifest = discover();

        assertThrows(IOException.class, () -> SavedGrantReceiptUniverseAudit.scan(
                manifest, ledger, 65536, 65536, observedPlayer -> {
                    assertEquals(player, observedPlayer);
                    save(player, GrantReceiptState.empty());
                }));
        // The journal is immutable, but the persisted receipt was replaced.
        assertTrue(ledger.pending(player).isEmpty());
    }

    @Test void unchangedSavedReceiptSucceedsAndManualManifestCannotClaimClosure()
            throws Exception {
        var ledger = new GrantLedger(temp.resolve("stable"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 100, "console");
        ledger.acknowledge(grant, grant.sequence());
        Path file = save(player, new GrantReceiptState(grant.sequence(), 0, null, 0));
        var manifest = discover();
        var report = SavedGrantReceiptUniverseAudit.scan(manifest, ledger, 65536, 65536);
        assertEquals(1, report.playersRead());
        assertEquals(1, report.acknowledgedGrants());

        var manuallyConstructed = new WorldStorageDiscovery.Manifest(
                java.util.List.of(), Map.of(player, file));
        assertThrows(IOException.class,
                () -> SavedPlayerdataContentWitness.capture(manuallyConstructed, 65536));
    }

    @Test void missingOrOversizedSavedPlayerdataDoesNotBecomeAnEmptyWitness() throws Exception {
        UUID player = UUID.randomUUID();
        Path source = save(player, GrantReceiptState.empty());
        var manifest = discover();
        assertThrows(IOException.class, () ->
                SavedPlayerdataContentWitness.capture(manifest, 8));
        var witness = SavedPlayerdataContentWitness.capture(manifest, 65536);
        Files.delete(source);
        assertThrows(IOException.class, witness::requireUnchanged);
    }
}
