package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec Candidate RC-GEN-01/02: second-generation fault injection.
 * This does not simulate power loss or prove external world snapshot consistency.
 */
class SettlementGenerationVisibilityFaultTest {
    @TempDir Path directory;

    private static SettlementGenerationPublisher.Published candidate(
            long generation, long amount) {
        var accumulator = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("snapshot"));
        accumulator.accept(new SettlementAccumulator.Asset(
                "snapshot", "slot", "world", null, false, false, false,
                BigInteger.valueOf(amount), "world:0,64,0"));
        accumulator.completeSource("snapshot");
        return new SettlementGenerationPublisher.Published(
                generation, accumulator.commit());
    }

    @Test void failedSecondGenerationRetainsPriorHeadAndCanRecover() throws Exception {
        Path root = directory.resolve("second-write-failure");
        var store = new SettlementSnapshotStore(root);
        var first = candidate(1, 100);
        var second = candidate(2, 200);
        store.persist(first);

        assertThrows(IOException.class, () -> store.persist(second, () -> {
            throw new IOException("Injected failure before second snapshot replacement");
        }));
        assertEquals(first, store.load().orElseThrow(),
                "Failure before replacing the second generation must preserve generation 1");

        var restarted = new SettlementSnapshotStore(root);
        assertEquals(first, restarted.load().orElseThrow());
        restarted.persist(second);
        assertEquals(second, restarted.load().orElseThrow());
    }

    @Test void concurrentReaderSeesOldHeadUntilSecondGenerationCommit() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("reader-visibility"));
        var first = candidate(1, 100);
        var second = candidate(2, 200);
        store.persist(first);

        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var writer = executor.submit(() -> {
                store.persist(second, () -> {
                    entered.countDown();
                    try {
                        if (!release.await(15, TimeUnit.SECONDS))
                            throw new IOException("Timed out waiting for injected writer release");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted injected writer", interrupted);
                    }
                });
                return null;
            });
            try {
                assertTrue(entered.await(15, TimeUnit.SECONDS),
                        "Writer must reach the pre-replacement hook");
                assertEquals(first, store.load().orElseThrow(),
                        "Concurrent readers must not observe an uncommitted generation");
            } finally {
                release.countDown();
            }
            writer.get(15, TimeUnit.SECONDS);
            assertEquals(second, store.load().orElseThrow(),
                    "Completed second generation must become visible after commit");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
