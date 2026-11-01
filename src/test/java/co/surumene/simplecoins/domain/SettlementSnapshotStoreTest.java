package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: versioned, restart-safe, fail-closed settlement generations. */
class SettlementSnapshotStoreTest {
    @TempDir Path directory;

    private static SettlementAccumulator staged(long personal, long free, long dead,
                                                boolean complete) {
        var acc = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("snapshot"));
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID banned = UUID.fromString("00000000-0000-0000-0000-000000000002");
        acc.accept(new SettlementAccumulator.Asset("snapshot", "slot:1", "world", player,
                false, false, true, BigInteger.valueOf(personal), "world:0,64,0"));
        acc.accept(new SettlementAccumulator.Asset("snapshot", "slot:2", "world", null,
                false, false, false, BigInteger.valueOf(free), "world:1,64,0"));
        acc.accept(new SettlementAccumulator.Asset("snapshot", "slot:3", "world", banned,
                false, true, true, BigInteger.valueOf(dead), "world:2,64,0"));
        if (complete) acc.completeSource("snapshot");
        return acc;
    }

    @Test void restartReplaysCompleteBalancesAndSuccessiveGeneration() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var publisher = new SettlementGenerationPublisher();
        var first = publisher.publishDurably(0, staged(100, 50, 25, true), store::persist);
        assertEquals(first, store.load().orElseThrow());
        assertEquals(BigInteger.valueOf(25), first.result().dead());
        assertEquals(1, first.result().deadLocations().size());
        var restarted = SettlementGenerationPublisher.recoverFrom(
                new SettlementSnapshotStore(directory.resolve("snapshots")));
        assertEquals(first, restarted.current().orElseThrow());
        var second = restarted.publishDurably(1, staged(200, 75, 10, true), store::persist);
        assertEquals(2L, second.generation());
        assertEquals(second, store.load().orElseThrow());
        assertEquals(second, SettlementGenerationPublisher.recoverFrom(store)
                .current().orElseThrow());
    }

    @Test void staleAndSkippedGenerationsNeverReplaceDurableHead() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var first = new SettlementGenerationPublisher()
                .publishDurably(0, staged(100, 0, 0, true), store::persist);
        assertThrows(IOException.class, () -> store.persist(first));
        var skipped = new SettlementGenerationPublisher.Published(3, staged(999, 0, 0, true).commit());
        assertThrows(IOException.class, () -> store.persist(skipped));
        assertEquals(first, store.load().orElseThrow());
    }

    @Test void incompleteStagingCannotWriteAndCorruptSnapshotFailsClosed() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var publisher = new SettlementGenerationPublisher();
        assertThrows(IllegalStateException.class,
                () -> publisher.publishDurably(0, staged(100, 0, 0, false), store::persist));
        assertTrue(store.load().isEmpty());
        publisher.publishDurably(0, staged(100, 0, 0, true), store::persist);
        Path file = directory.resolve("snapshots/settlement.snapshot");
        byte[] raw = Files.readAllBytes(file);
        raw[raw.length - 1] ^= 0x01;
        Files.write(file, raw);
        assertThrows(IOException.class, store::load);
        assertThrows(IOException.class, () -> SettlementGenerationPublisher.recoverFrom(store));
        assertThrows(IOException.class, () -> store.persist(
                new SettlementGenerationPublisher.Published(2, staged(200, 0, 0, true).commit())));
    }

    @Test void unknownVersionWithValidChecksumFailsClosed() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        store.persist(new SettlementGenerationPublisher.Published(1, staged(100, 0, 0, true).commit()));
        Path file = directory.resolve("snapshots/settlement.snapshot");
        byte[] payload = DurableFile.read(file, 4 * 1024 * 1024);
        payload[7] = 2; // format version, not checksum
        DurableFile.write(file, payload);
        assertThrows(IOException.class, store::load);
    }

    @Test void symlinkedRootOrSnapshotCannotRedirectDurableState() throws Exception {
        Path actual = Files.createDirectory(directory.resolve("actual"));
        Path alias = Files.createSymbolicLink(directory.resolve("alias"), actual);
        assertThrows(IOException.class, () -> new SettlementSnapshotStore(alias));
        var store = new SettlementSnapshotStore(actual);
        Path outside = Files.write(directory.resolve("outside"), new byte[] {1, 2, 3});
        Files.createSymbolicLink(actual.resolve("settlement.snapshot"), outside);
        assertThrows(IOException.class, store::load);
        assertThrows(IOException.class, () -> store.persist(
                new SettlementGenerationPublisher.Published(1, staged(1, 0, 0, true).commit())));
    }

    @Test void inconsistentStatisticsCannotBecomeCommittedGeneration() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var original = staged(100, 50, 25, true).commit();
        var forged = new SettlementAccumulator.Result(original.statistics(),
                Map.copyOf(original.personalBalances()), BigInteger.valueOf(51),
                original.dead(), original.deadLocations());
        assertThrows(IOException.class, () -> store.persist(
                new SettlementGenerationPublisher.Published(1, forged)));
        assertTrue(store.load().isEmpty());
    }

    @Test void competingStoreInstancesCannotBothCommitFirstGeneration() throws Exception {
        Path root = directory.resolve("snapshots");
        var first = new SettlementSnapshotStore(root);
        var second = new SettlementSnapshotStore(root);
        var candidate = new SettlementGenerationPublisher.Published(1, staged(100, 0, 0, true).commit());
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { start.await(); try { first.persist(candidate); return true; }
                catch (IOException e) { return false; } });
            var b = pool.submit(() -> { start.await(); try { second.persist(candidate); return true; }
                catch (IOException e) { return false; } });
            start.countDown();
            assertNotEquals(a.get(), b.get());
            assertEquals(candidate, first.load().orElseThrow());
        } finally {
            pool.shutdownNow();
        }
    }
}
