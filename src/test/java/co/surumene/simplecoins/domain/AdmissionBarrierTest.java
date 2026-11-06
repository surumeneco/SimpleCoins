package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Experimental Phase 6 admission-state prototype; CAT Draft is not Confirmed. */
class AdmissionBarrierTest {
    @Test void initialAndActiveSettlementDenyAdmission() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        assertEquals(AdmissionBarrier.State.BLOCKED, barrier.state());
        assertFalse(barrier.mayAdmit());
        long generation = barrier.begin();
        assertEquals(1, generation);
        assertEquals(AdmissionBarrier.State.SCANNING, barrier.state());
        assertFalse(barrier.mayAdmit());
        assertThrows(IllegalStateException.class, barrier::begin);
    }

    @Test void onlyCurrentVerifiedAndQuiescentGenerationMayPublishReadiness() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        long generation = barrier.begin();
        assertThrows(IllegalStateException.class, () -> barrier.complete(generation + 1, true, true));
        assertFalse(barrier.mayAdmit());
        barrier.complete(generation, true, true);
        assertTrue(barrier.mayAdmit());
        assertThrows(IllegalStateException.class, () -> barrier.complete(generation, true, true));
    }

    @Test void incompleteSnapshotOrOnlinePlayerPreventsAdmissionPermanently() {
        for (int flags = 0; flags < 3; flags++) {
            AdmissionBarrier barrier = new AdmissionBarrier();
            long generation = barrier.begin();
            switch (flags) {
                case 0 -> assertThrows(IllegalStateException.class,
                        () -> barrier.complete(generation, false, true));
                case 1 -> assertThrows(IllegalStateException.class,
                        () -> barrier.complete(generation, true, false));
                default -> barrier.fail(generation);
            }
            assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
            assertFalse(barrier.mayAdmit());
            assertThrows(IllegalStateException.class, barrier::begin);
            assertThrows(IllegalStateException.class,
                    () -> barrier.complete(generation, true, true));
        }
    }

    @Test void fatalFailureAndShutdownRevokePreviouslyPublishedAdmission() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        barrier.complete(barrier.begin(), true, true);
        assertTrue(barrier.mayAdmit());
        barrier.fatalFailure();
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertFalse(barrier.mayAdmit());
        barrier.stop();
        barrier.fatalFailure();
        assertEquals(AdmissionBarrier.State.STOPPED, barrier.state());
        assertFalse(barrier.mayAdmit());
        assertThrows(IllegalStateException.class, barrier::begin);
    }

    @Test void readersCannotObserveReadyBeforePublicationAcrossThreads() throws Exception {
        for (int n = 0; n < 120; n++) {
            AdmissionBarrier barrier = new AdmissionBarrier();
            long generation = barrier.begin();
            CountDownLatch go = new CountDownLatch(1);
            AtomicBoolean premature = new AtomicBoolean();
            Thread joinAttempt = new Thread(() -> {
                try {
                    go.await();
                    premature.set(barrier.mayAdmit());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            joinAttempt.start();
            go.countDown();
            joinAttempt.join();
            assertFalse(premature.get());
            barrier.complete(generation, true, true);
            assertTrue(barrier.mayAdmit());
            barrier.stop();
            assertFalse(barrier.mayAdmit());
        }
    }
}
