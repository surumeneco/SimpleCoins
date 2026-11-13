package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Phase6 publication barrier tests. JDK25 unit evidence is not a substitute
 * for proving live Minecraft world/disk snapshot authority.
 */
class SettlementSnapshotPublisherTest {
    private static SettlementAccumulator pending() {
        return new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("authoritative-file"));
    }

    @Test void completeSnapshotIsInvisibleUntilAdmissionOpens() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier, () -> true);
        assertTrue(publisher.current().isEmpty());
        long generation = barrier.begin();
        var source = pending();
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000101");
        source.accept(new SettlementAccumulator.Asset("authoritative-file", "player-slot-0",
                null, owner, false, false, true, BigInteger.valueOf(125), "player/inventory/0"));
        source.completeSource("authoritative-file");
        publisher.publish(source);
        assertFalse(publisher.mayAdmit());
        assertTrue(publisher.current().isEmpty());

        barrier.complete(generation, true, true);
        var result = publisher.current().orElseThrow();
        assertTrue(publisher.mayAdmit());
        assertEquals(BigInteger.valueOf(125), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(125), result.personalBalances().get(owner));
        assertThrows(UnsupportedOperationException.class, () ->
                result.personalBalances().put(UUID.randomUUID(), BigInteger.ONE));
        assertThrows(UnsupportedOperationException.class, () ->
                result.deadLocations().add(new SettlementAccumulator.DeadLocation(
                        owner, AssetOwner.DeadReason.CURRENT_BAN, BigInteger.ONE, "wrong")));
    }

    @Test void missingMandatorySourceFailsClosedBeforeAnyPublication() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier);
        barrier.begin();
        var partial = pending();
        partial.accept(new SettlementAccumulator.Asset("authoritative-file", "slot-1",
                "world", null, false, false, false, BigInteger.TEN, "world/block/1"));
        assertThrows(IllegalStateException.class, () -> publisher.publish(partial));
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertTrue(publisher.current().isEmpty());
        assertFalse(publisher.mayAdmit());
    }

    @Test void failedSourceAndDuplicatePublishRevokeReadiness() {
        AdmissionBarrier first = new AdmissionBarrier();
        SettlementSnapshotPublisher bad = new SettlementSnapshotPublisher(first);
        first.begin();
        var partial = pending();
        partial.failSource("authoritative-file", "corrupt NBT");
        assertThrows(IllegalStateException.class, () -> bad.publish(partial));
        assertEquals(AdmissionBarrier.State.FAILED, first.state());

        AdmissionBarrier second = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(second);
        second.begin();
        var source = pending();
        source.completeSource("authoritative-file");
        publisher.publish(source);
        assertThrows(IllegalStateException.class, () -> publisher.publish(source));
        assertEquals(AdmissionBarrier.State.FAILED, second.state());
        assertTrue(publisher.current().isEmpty());
    }

    @Test void currentFailureAndStopHidePreviouslyPublishedResult() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier, () -> true);
        long generation = barrier.begin();
        var source = pending();
        source.completeSource("authoritative-file");
        publisher.publish(source);
        barrier.complete(generation, true, true);
        assertTrue(publisher.current().isPresent());
        barrier.fatalFailure();
        assertTrue(publisher.current().isEmpty());
        assertFalse(publisher.mayAdmit());
        barrier.stop();
        assertTrue(publisher.current().isEmpty());
    }

    @Test void deadAssetTotalAndLocationRemainConsistent() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier, () -> true);
        long generation = barrier.begin();
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000102");
        var source = pending();
        source.accept(new SettlementAccumulator.Asset("authoritative-file", "container-slot-0",
                "world", owner, false, true, true, BigInteger.valueOf(750),
                "world/chest/3/inventory/0"));
        source.completeSource("authoritative-file");
        publisher.publish(source);
        barrier.complete(generation, true, true);
        var result = publisher.current().orElseThrow();
        assertEquals(BigInteger.ZERO, result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(750), result.dead());
        assertEquals(1, result.deadLocations().size());
        assertEquals("world/chest/3/inventory/0", result.deadLocations().getFirst().location());
    }

    @Test void unsafePrematureOrNullPublishCannotOpenAdmission() {
        AdmissionBarrier early = new AdmissionBarrier();
        SettlementSnapshotPublisher premature = new SettlementSnapshotPublisher(early);
        var source = pending();
        source.completeSource("authoritative-file");
        assertThrows(IllegalStateException.class, () -> premature.publish(source));
        assertEquals(AdmissionBarrier.State.FAILED, early.state());

        AdmissionBarrier missing = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(missing);
        missing.begin();
        assertThrows(NullPointerException.class, () -> publisher.publish(null));
        assertEquals(AdmissionBarrier.State.FAILED, missing.state());
    }
}
