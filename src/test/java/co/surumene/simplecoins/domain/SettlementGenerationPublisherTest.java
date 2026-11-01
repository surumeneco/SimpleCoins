package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class SettlementGenerationPublisherTest {
    private static SettlementAccumulator staged(long amount, boolean complete) {
        var acc = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("source"));
        acc.accept(new SettlementAccumulator.Asset(
                "source", "slot", "world", null, false, false, false,
                BigInteger.valueOf(amount), "world:0,64,0"));
        if (complete) acc.completeSource("source");
        return acc;
    }

    @Test void incompleteOrFailedSettlementCannotReplacePublishedTotals() {
        var publisher = new SettlementGenerationPublisher();
        assertTrue(publisher.current().isEmpty());
        var first = publisher.publish(0, staged(100, true));
        assertEquals(1L, first.generation());
        assertEquals(BigInteger.valueOf(100), first.result().free());

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(1, staged(200, false)));
        assertEquals(first, publisher.current().orElseThrow());
        assertThrows(IllegalStateException.class,
                () -> publisher.publish(0, staged(300, true)));
        assertEquals(first, publisher.current().orElseThrow());

        var next = staged(500, false);
        next.failSource("source", "snapshot changed");
        assertThrows(IllegalStateException.class, () -> publisher.publish(1, next));
        assertEquals(first, publisher.current().orElseThrow());
    }

    @Test void completedSettlementAdvancesExactlyOneGeneration() {
        var publisher = new SettlementGenerationPublisher();
        publisher.publish(0, staged(100, true));
        var second = publisher.publish(1, staged(250, true));
        assertEquals(2L, second.generation());
        assertEquals(BigInteger.valueOf(250), publisher.current().orElseThrow().result().free());
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publish(-1, staged(1, true)));
        assertThrows(NullPointerException.class,
                () -> publisher.publish(2, null));
        assertEquals(second, publisher.current().orElseThrow());
    }

    @Test void simultaneousWritersWithSameExpectedGenerationCannotBothPublish()
            throws Exception {
        var publisher = new SettlementGenerationPublisher();
        var gate = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                gate.await();
                try { publisher.publish(0, staged(100, true)); return true; }
                catch (IllegalStateException stale) { return false; }
            });
            var second = executor.submit(() -> {
                gate.await();
                try { publisher.publish(0, staged(200, true)); return true; }
                catch (IllegalStateException stale) { return false; }
            });
            gate.countDown();
            assertNotEquals(first.get(), second.get());
            assertEquals(1L, publisher.current().orElseThrow().generation());
        } finally {
            executor.shutdownNow();
        }
    }
}
