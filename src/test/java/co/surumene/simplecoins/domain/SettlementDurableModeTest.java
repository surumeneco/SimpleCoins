package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: durable state must never silently become memory-only. */
class SettlementDurableModeTest {
    @TempDir Path directory;

    private static SettlementAccumulator staged(long amount) {
        var acc = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("source"));
        acc.accept(new SettlementAccumulator.Asset("source", "slot", "world", null,
                false, false, false, BigInteger.valueOf(amount), "world:0,64,0"));
        acc.completeSource("source");
        return acc;
    }

    @Test void recoveredPublisherCannotPublishWithoutDurableWriter() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var publisher = SettlementGenerationPublisher.recoverFrom(store);
        assertTrue(publisher.current().isEmpty());
        assertThrows(IllegalStateException.class, () -> publisher.publish(0, staged(10)));
        var committed = publisher.publishDurably(0, staged(20), store::persist);
        assertEquals(committed, store.load().orElseThrow());
        assertThrows(IllegalStateException.class, () -> publisher.publish(1, staged(30)));
        assertEquals(committed, store.load().orElseThrow());
    }

    @Test void firstDurablePublicationPreventsLaterMemoryOnlyGeneration() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var publisher = new SettlementGenerationPublisher();
        publisher.publishDurably(0, staged(100), store::persist);
        assertThrows(IllegalStateException.class, () -> publisher.publish(1, staged(200)));
        assertEquals(BigInteger.valueOf(100), store.load().orElseThrow().result().free());
        var recovered = SettlementGenerationPublisher.recoverFrom(store);
        assertThrows(IllegalStateException.class, () -> recovered.publish(1, staged(300)));
        assertEquals(2L, recovered.publishDurably(1, staged(400), store::persist).generation());
    }

    @Test void explicitlyMemoryOnlyPublisherRetainsExistingBehavior() {
        var publisher = new SettlementGenerationPublisher();
        assertEquals(1L, publisher.publish(0, staged(10)).generation());
        assertEquals(2L, publisher.publish(1, staged(20)).generation());
    }
}
