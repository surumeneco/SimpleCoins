package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WriterEpochFence;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Genuine on-disk NBT/GrantLedger receipt assembly, NOT complete server writer coverage. */
final class SavedGrantProtectedAckStageTest {
    @TempDir Path temp;

    private Path world() throws IOException {
        Path root = temp.resolve("world");
        Files.createDirectories(root);
        return root;
    }

    private Path playerFile(UUID player) throws IOException {
        Path file = world().resolve("playerdata").resolve(player + ".dat");
        Files.createDirectories(file.getParent());
        return file;
    }

    private void write(UUID player, GrantReceiptState receipt, boolean hasCoin,
                       String unrelatedItem) throws IOException {
        write(player, receipt, hasCoin ? 1 : 0, unrelatedItem);
    }

    /** Save the real canonical 64-coin partial stack, not a receipt-only claim. */
    private void write(UUID player, GrantReceiptState receipt, int coinCount,
                       String unrelatedItem) throws IOException {
        write(player, receipt, coinCount, 0, unrelatedItem);
    }

    private void write(UUID player, GrantReceiptState receipt, int coinCount,
                       int extraCoinCount, String unrelatedItem) throws IOException {
        if (extraCoinCount < 0 || extraCoinCount > 64)
            throw new IllegalArgumentException("Invalid test-only final coin stack");
        if (coinCount < 0 || coinCount > 64)
            throw new IllegalArgumentException("Invalid test-only saved coin stack size");
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(playerFile(player)));
             var out = new DataOutputStream(gzip)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("BukkitValues");
            byte[] savedReceipt = PaperGrantReceiptPdc.toSavedBytes(receipt);
            out.writeByte(7); out.writeUTF("simplecoins:grant_receipt_v1");
            out.writeInt(savedReceipt.length); out.write(savedReceipt);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("Inventory");
            out.writeByte(10); out.writeInt((coinCount > 0 ? 1 : 0)
                    + (extraCoinCount > 0 ? 1 : 0) + 1);
            if (coinCount > 0) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(coinCount);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(8); out.writeUTF("minecraft:item_model");
                out.writeUTF("simplecoins:coin_5000");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:coin_denomination");
                out.writeInt(5000);
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            if (extraCoinCount > 0) {
                out.writeByte(1); out.writeUTF("Slot"); out.writeByte(1);
                out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:paper");
                out.writeByte(3); out.writeUTF("count"); out.writeInt(extraCoinCount);
                out.writeByte(10); out.writeUTF("components");
                out.writeByte(8); out.writeUTF("minecraft:item_model");
                out.writeUTF("simplecoins:coin_5000");
                out.writeByte(10); out.writeUTF("minecraft:custom_data");
                out.writeByte(10); out.writeUTF("PublicBukkitValues");
                out.writeByte(3); out.writeUTF("simplecoins:coin_denomination");
                out.writeInt(5000);
                out.writeByte(0); out.writeByte(0); out.writeByte(0); out.writeByte(0);
            }
            out.writeByte(1); out.writeUTF("Slot"); out.writeByte(9);
            out.writeByte(8); out.writeUTF("id"); out.writeUTF(unrelatedItem);
            out.writeByte(3); out.writeUTF("count"); out.writeInt(2);
            out.writeByte(0);
            out.writeByte(9); out.writeUTF("EnderItems");
            out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
    }

    private WorldStorageDiscovery.Manifest discovery() throws IOException {
        return WorldStorageDiscovery.discover(Map.of("world", world()),
                new WorldScope(true, Set.of(), Set.of()), 256);
    }

    private static PaperGrantInventorySlotPlan fullPlan(
            GrantLedger ledger, UUID player, GrantLedger.Grant grant) {
        var slots = new ArrayList<>(Collections.nCopies(36, "stone"));
        slots.set(0, null);
        return PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(slots)).orElseThrow();
    }

    /** Reflection makes absent product assembly an expected-reason JUnit Red, not a build failure. */
    private static SavedGrantDeliveryReadback.Proof execute(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore) throws Throwable {
        Class<?> stage = Class.forName(
                "co.surumene.simplecoins.paper.SavedGrantProtectedAckStage");
        var operation = stage.getDeclaredMethod("finish",
                WriterEpochFence.class, WriterEpochFence.Seal.class,
                WorldStorageDiscovery.Manifest.class, GrantLedger.class, UUID.class,
                GrantLedger.Grant.class, PaperGrantInventorySlotPlan.class,
                Map.class, int.class, int.class);
        operation.setAccessible(true);
        try {
            return (SavedGrantDeliveryReadback.Proof) operation.invoke(null, fence, seal,
                    manifest, ledger, player, grant, plan, savedBefore, 65536, 65536);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }


    /** Real 65-coin grant, first 64 already saved with an active PDC cursor. */
    private record Resumed(GrantLedger ledger, GrantLedger.Grant grant,
                           PaperGrantInventorySlotPlan first,
                           PaperGrantInventorySlotPlan last) {}

    private static Resumed resumedPlan(GrantLedger ledger, UUID player) throws IOException {
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 65, "console");
        var firstSlots = new ArrayList<>(Collections.nCopies(36, "stone"));
        firstSlots.set(0, null);
        var first = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(firstSlots)).orElseThrow();
        assertTrue(first.nextReceipt().active());
        assertEquals(320_000, first.nextReceipt().partialValue());
        var lastSlots = new ArrayList<>(Collections.nCopies(36, "stone"));
        lastSlots.set(1, null);
        var last = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                first.nextReceipt(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(lastSlots)).orElseThrow();
        assertFalse(last.nextReceipt().active());
        assertEquals(grant.sequence(), last.nextReceipt().completedSequence());
        assertEquals(1, last.placements().size());
        assertEquals(1, last.placements().getFirst().index());
        assertEquals(1, last.placements().getFirst().count());
        return new Resumed(ledger, grant, first, last);
    }

    /** Resolve reflectively so the exact absent product method yields meaningful Red. */
    private static SavedGrantDeliveryReadback.Proof finishLastPartialBatch(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger, UUID player,
            GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore) throws Throwable {
        Class<?> stage = Class.forName(
                "co.surumene.simplecoins.paper.SavedGrantProtectedAckStage");
        var method = stage.getDeclaredMethod("finishLastPartialBatch",
                WriterEpochFence.class, WriterEpochFence.Seal.class,
                WorldStorageDiscovery.Manifest.class, GrantLedger.class,
                UUID.class, GrantLedger.Grant.class, PaperGrantInventorySlotPlan.class,
                Map.class, int.class, int.class);
        method.setAccessible(true);
        try {
            return (SavedGrantDeliveryReadback.Proof) method.invoke(
                    null, fence, seal, manifest, ledger, player, grant,
                    plan, savedBefore, 65536, 65536);
        } catch (InvocationTargetException invocation) {
            throw invocation.getCause();
        }
    }

    @Test void finalPartialBatchFreshSaveAcksWithoutReissuingEarlierCoins() throws Throwable {
        UUID player = UUID.randomUUID();
        Path journal = temp.resolve("grants-final-partial-success");
        Resumed pair = resumedPlan(new GrantLedger(journal), player);
        write(player, pair.first().nextReceipt(), 64, 0, "minecraft:stone");
        Map<String, Object> savedBefore = PlayerDataReader.read(
                playerFile(player), 65536, 65536);
        write(player, pair.last().nextReceipt(), 64, 1, "minecraft:stone");

        var fence = new WriterEpochFence(); fence.arm();
        var seal = fence.seal();
        var result = finishLastPartialBatch(fence, seal, discovery(),
                pair.ledger(), player, pair.grant(), pair.last(), savedBefore);
        assertEquals(BigInteger.valueOf(5000), result.matchedMonetaryValue());
        assertEquals(1, result.matchedStacks());
        assertEquals(pair.grant().sequence(), result.receipt().completedSequence());
        fence.requireDrained(seal);
        assertEquals(WriterEpochFence.State.SEALED, fence.state());

        var replay = new GrantLedger(journal);
        assertTrue(replay.pending(player).isEmpty());
        assertTrue(replay.historySnapshot().getFirst().delivered());
        var physical = PlayerDataReader.read(playerFile(player), 65536, 65536);
        var assets = co.surumene.simplecoins.domain.SavedPlayerAssets.extract(physical);
        assertEquals(BigInteger.valueOf(320000), assets.get("inventory:0"));
        assertEquals(BigInteger.valueOf(5000), assets.get("inventory:1"));
        assertEquals(pair.last().nextReceipt(),
                SavedGrantReceiptNbt.readAndVerify(physical, replay, player));
    }

    @Test void finalPartialBatchMissingOrChangedCoinFailsClosed() throws Throwable {
        for (int failureCase = 0; failureCase < 2; failureCase++) {
            UUID player = UUID.randomUUID();
            Path journal = temp.resolve("grants-final-partial-failure-" + failureCase);
            Resumed pair = resumedPlan(new GrantLedger(journal), player);
            write(player, pair.first().nextReceipt(), 64, 0, "minecraft:stone");
            Map<String, Object> savedBefore = PlayerDataReader.read(
                    playerFile(player), 65536, 65536);
            // Either the newly issued coin disappeared, or previously saved
            // 64 coins were modified while the final receipt falsely completed.
            write(player, pair.last().nextReceipt(), failureCase == 0 ? 64 : 63,
                    failureCase == 0 ? 0 : 1, "minecraft:stone");
            var fence = new WriterEpochFence(); fence.arm();
            var seal = fence.seal();
            assertThrows(IOException.class, () ->
                    finishLastPartialBatch(fence, seal, discovery(),
                            pair.ledger(), player, pair.grant(), pair.last(), savedBefore));
            assertEquals(WriterEpochFence.State.FAULTED, fence.state());
            assertEquals(1, new GrantLedger(journal).pending(player).size());
        }
    }

    @Test void singleBatchAckStillRejectsActivePriorCursor() throws Throwable {
        UUID player = UUID.randomUUID();
        Resumed pair = resumedPlan(new GrantLedger(temp.resolve("grants-final-single-negative")),
                player);
        write(player, pair.first().nextReceipt(), 64, 0, "minecraft:stone");
        var before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, pair.last().nextReceipt(), 64, 1, "minecraft:stone");
        var fence = new WriterEpochFence(); fence.arm();
        var seal = fence.seal();
        assertThrows(IllegalStateException.class, () ->
                execute(fence, seal, discovery(), pair.ledger(), player,
                        pair.grant(), pair.last(), before));
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        assertEquals(1, pair.ledger().pending(player).size());
    }

    @Test void successfullyFsyncsFreshProofAndAcksWithinOneSeal() throws Throwable {
        UUID player = UUID.randomUUID();
        Path grantPath = temp.resolve("grants-valid");
        GrantLedger ledger = new GrantLedger(grantPath);
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 1, "console");
        var plan = fullPlan(ledger, player, grant);
        write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        Map<String, Object> before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        WriterEpochFence fence = new WriterEpochFence();
        fence.arm();  // test-only; no claim about global writer coverage
        var seal = fence.seal();
        var proof = execute(fence, seal, discovery(), ledger, player, grant, plan, before);
        assertEquals(BigInteger.valueOf(5000), proof.matchedMonetaryValue());
        assertEquals(grant.sequence(), proof.receipt().completedSequence());
        assertEquals(WriterEpochFence.State.SEALED, fence.state());
        fence.requireDrained(seal);
        GrantLedger reopened = new GrantLedger(grantPath);
        assertTrue(reopened.pending(player).isEmpty());
        assertEquals(1, reopened.historySnapshot().size());
        assertTrue(reopened.historySnapshot().getFirst().delivered());
        assertEquals("minecraft:paper", ((Map<?, ?>)((java.util.List<?>)
                PlayerDataReader.read(playerFile(player), 65536, 65536)
                        .get("Inventory")).getFirst()).get("id"));
    }

    @Test void refusesOpenUnsealedAndWrongGrantOrPartial() throws Throwable {
        UUID player = UUID.randomUUID();
        GrantLedger ledger = new GrantLedger(temp.resolve("grants-invalid"));
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 1, "console");
        var plan = fullPlan(ledger, player, grant);
        write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        Map<String, Object> before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        var manifest = discovery();
        var fence = new WriterEpochFence();
        fence.arm();
        assertThrows(IllegalStateException.class, () ->
                execute(fence, null, manifest, ledger, player, grant, plan, before));
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        assertEquals(1, ledger.pending(player).size());
        var foreignFence = new WriterEpochFence();
        foreignFence.arm();
        var foreignSeal = foreignFence.seal();
        var actualFence = new WriterEpochFence();
        actualFence.arm();
        assertThrows(IllegalStateException.class, () ->
                execute(actualFence, foreignSeal, manifest, ledger, player, grant, plan, before));
        assertEquals(WriterEpochFence.State.FAULTED, actualFence.state());
        assertEquals(1, ledger.pending(player).size());

        // A later legitimate underfilled batch must never acknowledge the
        // entire grant merely because a single physical stack saved.
        UUID otherPlayer = UUID.randomUUID();
        var bigGrant = ledger.reserveCoins(otherPlayer, CoinDenomination.FIVE_THOUSAND,
                65, "console");
        var partial = fullPlan(ledger, otherPlayer, bigGrant);
        assertTrue(partial.nextReceipt().active());
        var partialFence = new WriterEpochFence();
        partialFence.arm();
        var partialSeal = partialFence.seal();
        assertThrows(IllegalStateException.class, () ->
                execute(partialFence, partialSeal, manifest, ledger,
                        otherPlayer, bigGrant, partial, before));
        assertEquals(WriterEpochFence.State.FAULTED, partialFence.state());
        assertEquals(1, ledger.pending(otherPlayer).size());
    }

    @Test void refusesMissingPhysicalCoinOrChangedStorage() throws Throwable {
        UUID player = UUID.randomUUID();
        Path journalPath = temp.resolve("grants-negative");
        GrantLedger ledger = new GrantLedger(journalPath);
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 1, "console");
        var plan = fullPlan(ledger, player, grant);
        write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        Map<String, Object> before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        var manifest = discovery();
        write(player, plan.nextReceipt(), false, "minecraft:stone");
        var fence = new WriterEpochFence();
        fence.arm();
        var seal = fence.seal();
        assertThrows(IOException.class, () ->
                execute(fence, seal, manifest, ledger, player, grant, plan, before));
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        assertEquals(1, new GrantLedger(journalPath).pending(player).size());

        // Existing unrelated inventory is authoritative and may not be
        // overwritten by a new grant batch.
        write(player, plan.nextReceipt(), true, "minecraft:dirt");
        var next = new WriterEpochFence();
        next.arm();
        var nextSeal = next.seal();
        assertThrows(IOException.class, () ->
                execute(next, nextSeal, manifest, ledger, player, grant, plan, before));
        assertEquals(WriterEpochFence.State.FAULTED, next.state());
        assertEquals(1, new GrantLedger(journalPath).pending(player).size());
    }

    /**
     * A02 same-generation exclusion must last across the complete REAL file
     * readback. The old point-in-time requireDrained checks allow a resume
     * while we are inside the verified prior receipt decode. This test has
     * an expected behavioral Red before the protected IO lease exists.
     */
    @Test void resumptionCannotEnterBetweenFreshReadbackAndDurableAck() throws Throwable {
        UUID player = UUID.randomUUID();
        Path journalPath = temp.resolve("grants-race");
        GrantLedger ledger = new GrantLedger(journalPath);
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 1, "console");
        var plan = fullPlan(ledger, player, grant);
        write(player, GrantReceiptState.empty(), false, "minecraft:stone");
        Map<String, Object> before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, plan.nextReceipt(), true, "minecraft:stone");
        var manifest = discovery();

        java.util.concurrent.CountDownLatch readbackEntered =
                new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch releaseReadback =
                new java.util.concurrent.CountDownLatch(1);
        Map<String, Object> pausedBefore = new java.util.AbstractMap<>() {
            @Override public Set<Map.Entry<String, Object>> entrySet() {
                return before.entrySet();
            }
            @Override public boolean containsKey(Object key) {
                if ("BukkitValues".equals(key)) {
                    readbackEntered.countDown();
                    try {
                        if (!releaseReadback.await(5, java.util.concurrent.TimeUnit.SECONDS))
                            throw new IllegalStateException("Test readback hold timed out");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Readback interrupted", interrupted);
                    }
                }
                return before.containsKey(key);
            }
            @Override public Object get(Object key) { return before.get(key); }
        };
        WriterEpochFence fence = new WriterEpochFence();
        fence.arm();
        var seal = fence.seal();
        var result = new java.util.concurrent.atomic.AtomicReference<
                SavedGrantDeliveryReadback.Proof>();
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread delivery = new Thread(() -> {
            try {
                result.set(execute(fence, seal, manifest, ledger, player, grant,
                        plan, pausedBefore));
            } catch (Throwable fault) {
                failure.set(fault);
            }
        }, "isolated-g3-proof-ack");
        delivery.start();
        try {
            assertTrue(readbackEntered.await(5, java.util.concurrent.TimeUnit.SECONDS),
                    "Real saved-file transition verification must reach the prior receipt");
            // The old stage allows resume here and reopens money mutation,
            // despite having already fsynced/read the same authoritative file.
            assertThrows(IllegalStateException.class, () -> fence.resume(seal),
                    "Writer epoch cannot reopen while money and receipt are being verified");
            assertEquals(WriterEpochFence.State.SEALED, fence.state());
            assertThrows(IllegalStateException.class, fence::enterRoot,
                    "No new writer may enter before persistent ACK completes");
        } finally {
            releaseReadback.countDown();
            delivery.join(5000);
        }
        assertFalse(delivery.isAlive(), "The protected grant must finish promptly");
        assertNull(failure.get(), "A failed ACK must not be reported as a successful delivery");
        assertNotNull(result.get());
        assertEquals(BigInteger.valueOf(5000), result.get().matchedMonetaryValue());
        assertEquals(WriterEpochFence.State.SEALED, fence.state());
        fence.requireDrained(seal);
        var replay = new GrantLedger(journalPath);
        assertEquals(0, replay.pending(player).size());
        assertEquals(1, replay.historySnapshot().size());
        assertTrue(replay.historySnapshot().getFirst().delivered());
    }

    /** Real partial product stage must NOT silently call GrantLedger ACK. */
    private static SavedGrantDeliveryReadback.Proof executePartial(
            WriterEpochFence fence, WriterEpochFence.Seal seal,
            WorldStorageDiscovery.Manifest manifest, GrantLedger ledger,
            UUID player, GrantLedger.Grant grant, PaperGrantInventorySlotPlan plan,
            Map<String, Object> savedBefore) throws Throwable {
        Class<?> stage = Class.forName(
                "co.surumene.simplecoins.paper.SavedGrantProtectedPartialStage");
        var operation = stage.getDeclaredMethod("verifyPartial",
                WriterEpochFence.class, WriterEpochFence.Seal.class,
                WorldStorageDiscovery.Manifest.class, GrantLedger.class, UUID.class,
                GrantLedger.Grant.class, PaperGrantInventorySlotPlan.class,
                Map.class, int.class, int.class);
        operation.setAccessible(true);
        try {
            return (SavedGrantDeliveryReadback.Proof) operation.invoke(null, fence, seal,
                    manifest, ledger, player, grant, plan, savedBefore, 65536, 65536);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    @Test void partialRealSavedBatchMaintainsPendingNoAck() throws Throwable {
        UUID player = UUID.randomUUID();
        Path journalPath = temp.resolve("grants-partial-authentic");
        GrantLedger ledger = new GrantLedger(journalPath);
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND,
                65, "console");
        var plan = fullPlan(ledger, player, grant);
        assertTrue(plan.nextReceipt().active(), "Only first 64 of 65 coins may be delivered");
        assertEquals(grant.sequence(), plan.nextReceipt().activeSequence());
        assertEquals(320000L, plan.nextReceipt().partialValue());
        assertEquals(1, plan.placements().size());
        assertEquals(64, plan.placements().getFirst().count());

        write(player, GrantReceiptState.empty(), 0, "minecraft:stone");
        var before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, plan.nextReceipt(), 64, "minecraft:stone");
        var manifest = discovery();
        var fence = new WriterEpochFence();
        fence.arm();
        var seal = fence.seal();
        var proof = executePartial(fence, seal, manifest,
                ledger, player, grant, plan, before);
        assertEquals(plan.nextReceipt(), proof.receipt());
        assertEquals(BigInteger.valueOf(320000), proof.matchedMonetaryValue());
        assertEquals(1, proof.matchedStacks());
        assertEquals(WriterEpochFence.State.SEALED, fence.state());
        fence.requireDrained(seal);
        var persisted = new GrantLedger(journalPath);
        assertEquals(1, persisted.pending(player).size());
        assertFalse(persisted.historySnapshot().getFirst().delivered(),
                "A correct partial physical delivery must NOT acknowledge the entire grant");
        var after = PlayerDataReader.read(playerFile(player), 65536, 65536);
        assertEquals(plan.nextReceipt(),
                SavedGrantReceiptNbt.readAndVerify(after, persisted, player));
        assertEquals(64, ((Number) ((Map<?, ?>) ((java.util.List<?>) after
                .get("Inventory")).getFirst()).get("count")).intValue());
    }

    @Test void partialSavedCoinMissingFaultsAndKeepsPending() throws Throwable {
        UUID player = UUID.randomUUID();
        Path journalPath = temp.resolve("grants-partial-missing");
        GrantLedger ledger = new GrantLedger(journalPath);
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND,
                65, "console");
        var plan = fullPlan(ledger, player, grant);
        write(player, GrantReceiptState.empty(), 0, "minecraft:stone");
        var before = PlayerDataReader.read(playerFile(player), 65536, 65536);
        write(player, plan.nextReceipt(), 0, "minecraft:stone");
        var manifest = discovery();
        var fence = new WriterEpochFence(); fence.arm(); var seal = fence.seal();
        assertThrows(IOException.class, () ->
                executePartial(fence, seal, manifest, ledger, player, grant, plan, before));
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        var persisted = new GrantLedger(journalPath);
        assertEquals(1, persisted.pending(player).size());
        assertFalse(persisted.historySnapshot().getFirst().delivered());
    }
}
