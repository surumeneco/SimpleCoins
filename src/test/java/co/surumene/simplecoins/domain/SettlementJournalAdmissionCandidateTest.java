package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Confirmed admission-eligibility journal-active constraint: direct candidate regression. */
class SettlementJournalAdmissionCandidateTest {
    private static void publish(AdmissionBarrier barrier, SettlementSnapshotPublisher publisher) {
        long generation = barrier.begin();
        SettlementAccumulator candidate = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("complete-source"));
        candidate.completeSource("complete-source");
        publisher.publish(candidate);
        barrier.complete(generation, true, true);
    }

    @Test void fullyPublishedSettlementCannotAdmitWhenJournalMonitorIsNotConnected() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier);
        publish(barrier, publisher);
        assertFalse(publisher.mayAdmit(), "A snapshot is insufficient without live journal monitoring");
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertTrue(publisher.current().isEmpty());
    }

    @Test void liveJournalCanPermitPublishedSnapshotButItsLossIrreversiblyClosesAdmission() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        AtomicBoolean journal = new AtomicBoolean(true);
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier, journal::get);
        publish(barrier, publisher);
        assertTrue(publisher.mayAdmit());
        journal.set(false);
        assertFalse(publisher.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        journal.set(true);
        assertFalse(publisher.mayAdmit(), "A lost journal must not automatically reopen access");
    }

    @Test void monitorSignalExceptionMustNeverGrantAdmission() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementSnapshotPublisher publisher = new SettlementSnapshotPublisher(barrier,
                () -> { throw new IllegalStateException("monitor unavailable"); });
        publish(barrier, publisher);
        assertFalse(publisher.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertTrue(publisher.current().isEmpty());
    }

    @Test void nullMonitorSupplierCannotBeRegistered() {
        assertThrows(NullPointerException.class, () ->
                new SettlementSnapshotPublisher(new AdmissionBarrier(), null));
    }
}
