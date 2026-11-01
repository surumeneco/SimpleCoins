package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettlementSourceBarrierTest {
    @TempDir Path directory;

    private static SettlementAccumulator complete() {
        var stage = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("disk"));
        stage.accept(new SettlementAccumulator.Asset("disk", "slot", "world", null,
                false, false, false, BigInteger.TEN, "world:0,64,0"));
        stage.completeSource("disk");
        return stage;
    }

    @Test void onlyExactDurableReadbackOpensAllSources() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var expected = new SettlementGenerationPublisher.Published(1, complete().commit());
        var manifest = new WorldStorageDiscovery.Manifest(
                java.util.List.of(new WorldStorageDiscovery.SourceFile("world",
                        directory.resolve("region/r.0.0.mca"), WorldStorageDiscovery.Kind.REGION)),
                java.util.Map.of());
        assertEquals(SettlementSourceBarrier.State.BLOCKED,
                SettlementSourceBarrier.fromManifest(manifest).state());
        var gate = new SettlementSourceBarrier(Set.of("region", "playerdata"));
        assertThrows(IllegalStateException.class, gate::requireJoinReady);
        gate.beginStaging();
        gate.completeSource("region");
        store.persist(expected);
        assertThrows(IllegalStateException.class, () -> gate.openAfterDurableReadback(store, expected));
        gate.completeSource("playerdata");
        gate.openAfterDurableReadback(store, expected);
        assertDoesNotThrow(gate::requireJoinReady);
        var admissions = new java.util.concurrent.atomic.AtomicInteger();
        gate.withJoinPermit(admissions::incrementAndGet);
        assertEquals(1, admissions.get());
        var uncertain = new SettlementSourceBarrier(Set.of("region"));
        uncertain.beginStaging();
        uncertain.completeSource("region");
        uncertain.openAfterDurableReadback(store, expected);
        assertThrows(IllegalStateException.class,
                () -> uncertain.withJoinPermit(() -> { throw new IllegalStateException("admission failed"); }));
        assertEquals(SettlementSourceBarrier.State.FAILED, uncertain.state());
        var reentrant = new SettlementSourceBarrier(Set.of("region"));
        reentrant.beginStaging();
        reentrant.completeSource("region");
        reentrant.openAfterDurableReadback(store, expected);
        assertThrows(IllegalStateException.class,
                () -> reentrant.withJoinPermit(() -> reentrant.sourceMutated("region")));
        assertEquals(SettlementSourceBarrier.State.FAILED, reentrant.state());
        var late = new SettlementSourceBarrier(Set.of("region"));
        late.beginStaging();
        late.completeSource("region");
        late.openAfterDurableReadback(store, expected);
        late.failSource("region");
        assertEquals(SettlementSourceBarrier.State.FAILED, late.state());
        assertThrows(IllegalStateException.class, late::requireJoinReady);
        gate.sourceMutated("region");
        assertThrows(IllegalStateException.class, gate::requireJoinReady);
        assertThrows(IllegalStateException.class,
                () -> gate.withJoinPermit(admissions::incrementAndGet));
        assertEquals(1, admissions.get());
    }

    @Test void duplicateOrUnknownSourcesFailClosed() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        gate.completeSource("region");
        assertThrows(IllegalStateException.class, () -> gate.completeSource("region"));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
        var unknown = new SettlementSourceBarrier(Set.of("region"));
        unknown.beginStaging();
        assertThrows(IllegalStateException.class, () -> unknown.completeSource("unknown"));
        assertEquals(SettlementSourceBarrier.State.FAILED, unknown.state());
    }

    @Test void missingOrDifferentDurableGenerationDoesNotOpen() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("snapshots"));
        var expected = new SettlementGenerationPublisher.Published(1, complete().commit());
        var missing = new SettlementSourceBarrier(Set.of("disk"));
        missing.beginStaging();
        missing.completeSource("disk");
        assertThrows(IOException.class, () -> missing.openAfterDurableReadback(store, expected));
        assertEquals(SettlementSourceBarrier.State.FAILED, missing.state());
        store.persist(expected);
        var mismatch = new SettlementSourceBarrier(Set.of("disk"));
        mismatch.beginStaging();
        mismatch.completeSource("disk");
        var other = new SettlementGenerationPublisher.Published(2, complete().commit());
        assertThrows(IOException.class, () -> mismatch.openAfterDurableReadback(store, other));
        assertEquals(SettlementSourceBarrier.State.FAILED, mismatch.state());
    }
    @Test void nestedAdmissionFailsClosedBeforeNestedCallback() throws Exception {
        var store = new SettlementSnapshotStore(directory.resolve("nested-snapshots"));
        var expected = new SettlementGenerationPublisher.Published(1, complete().commit());
        store.persist(expected);
        var gate = new SettlementSourceBarrier(Set.of("disk"));
        gate.beginStaging();
        gate.completeSource("disk");
        gate.openAfterDurableReadback(store, expected);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        assertThrows(IllegalStateException.class, () -> gate.withJoinPermit(() -> {
            calls.incrementAndGet();
            gate.withJoinPermit(calls::incrementAndGet);
        }));
        assertEquals(1, calls.get(), "nested admission callback must not run");
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
        assertThrows(IllegalStateException.class,
                () -> gate.withJoinPermit(calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

}
