package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Observable composition contract for existing startup and publication adapters.
 * These tests characterize in-process fail-closed behavior, not authoritative
 * Paper world quiescence or crash-durable accounting.
 */
class SettlementStartupPublicationBoundaryIntegrationTest {
    private static final class Queue implements Executor {
        private final ArrayDeque<Runnable> pending = new ArrayDeque<>();

        @Override public void execute(Runnable task) {
            pending.add(task);
        }

        void next() {
            assertFalse(pending.isEmpty(), "Expected a scheduled phase");
            pending.removeFirst().run();
        }

        boolean isEmpty() { return pending.isEmpty(); }
    }

    private static SettlementAccumulator complete() {
        var pending = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("saved-source"));
        pending.completeSource("saved-source");
        return pending;
    }

    @Test void publicationIsInvisibleBetweenStagingAndMainThreadCommit() {
        var gate = new AdmissionBarrier();
        var published = new SettlementSnapshotPublisher(gate, () -> true);
        var background = new Queue();
        var main = new Queue();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                SettlementStartupPublicationBoundaryIntegrationTest::complete,
                published::publish, () -> true, cause -> fail(cause));

        startup.start();
        assertFalse(gate.mayAdmit());
        assertTrue(published.current().isEmpty());
        background.next();
        assertFalse(gate.mayAdmit());
        assertTrue(published.current().isEmpty());
        main.next();
        assertTrue(gate.mayAdmit());
        assertTrue(published.current().isPresent());
        assertTrue(startup.failure().isEmpty());
    }

    @Test void latePlayerArrivalAfterStagingRevokesAlreadyPublishedResult() {
        var gate = new AdmissionBarrier();
        var published = new SettlementSnapshotPublisher(gate, () -> true);
        var background = new Queue();
        var main = new Queue();
        var nobodyOnline = new AtomicBoolean(true);
        var signaled = new AtomicInteger();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                SettlementStartupPublicationBoundaryIntegrationTest::complete,
                candidate -> {
                    published.publish(candidate);
                    assertTrue(published.current().isEmpty(),
                            "Published state must stay invisible until READY");
                    nobodyOnline.set(false);
                },
                nobodyOnline::get, failure -> signaled.incrementAndGet());

        startup.start();
        background.next();
        main.next();
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        assertEquals(1, signaled.get());
        assertFalse(published.mayAdmit());
        assertTrue(published.current().isEmpty());
        assertTrue(startup.failure().isPresent());
    }

    @Test void publisherExceptionAfterStagingNeverLeaksPreviousResult() {
        var gate = new AdmissionBarrier();
        var published = new SettlementSnapshotPublisher(gate, () -> true);
        var background = new Queue();
        var main = new Queue();
        var signaled = new AtomicInteger();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                SettlementStartupPublicationBoundaryIntegrationTest::complete,
                candidate -> {
                    published.publish(candidate);
                    throw new IOException("Atomic publication handoff failed");
                }, () -> true, failure -> signaled.incrementAndGet());

        startup.start();
        background.next();
        main.next();
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        assertEquals(1, signaled.get());
        assertEquals("Atomic publication handoff failed",
                startup.failure().orElseThrow().getMessage());
        assertTrue(published.current().isEmpty());
        assertFalse(published.mayAdmit());
    }

    @Test void failedBackgroundStageNeverSchedulesPublication() {
        var gate = new AdmissionBarrier();
        var published = new SettlementSnapshotPublisher(gate, () -> true);
        var background = new Queue();
        var main = new Queue();
        var signaled = new AtomicInteger();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                () -> { throw new IOException("Incomplete saved-source closure"); },
                published::publish, () -> true, error -> signaled.incrementAndGet());

        startup.start();
        background.next();
        assertTrue(main.isEmpty());
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        assertEquals(1, signaled.get());
        assertTrue(published.current().isEmpty());
        assertFalse(published.mayAdmit());
    }

    /**
     * A READY domain barrier is intentionally NOT sufficient to release a
     * staged result: current() and mayAdmit() must also have a live monitor.
     * This is a pure fixture, not a real authoritative journal.
     */
    @Test void defaultJournalMonitorFailsClosedEvenAfterOtherwiseValidStaging() {
        var gate = new AdmissionBarrier();
        var published = new SettlementSnapshotPublisher(gate);
        var background = new Queue();
        var main = new Queue();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                SettlementStartupPublicationBoundaryIntegrationTest::complete,
                published::publish, () -> true, failure -> fail(failure));

        startup.start();
        background.next();
        assertTrue(published.current().isEmpty());
        main.next();
        assertEquals(AdmissionBarrier.State.READY, gate.state(),
                "Domain coordinator alone sets READY before monitor check");
        assertFalse(published.mayAdmit(),
                "An unbound journal monitor must never authorize publication");
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        assertTrue(published.current().isEmpty());
        assertTrue(startup.failure().isEmpty(),
                "Downstream monitor failure is terminal in publisher, not coordinator");
    }

    /** Losing journal evidence permanently revokes even an already visible result. */
    @Test void journalSignalLossRevokesVisiblePublicationAndCannotReopenIt() {
        var gate = new AdmissionBarrier();
        var active = new AtomicBoolean(true);
        var published = new SettlementSnapshotPublisher(gate, active::get);
        var background = new Queue();
        var main = new Queue();
        var startup = new SettlementStartupCoordinator<>(
                gate, background, main,
                SettlementStartupPublicationBoundaryIntegrationTest::complete,
                published::publish, () -> true, failure -> fail(failure));

        startup.start();
        background.next();
        main.next();
        assertEquals(AdmissionBarrier.State.READY, gate.state());
        assertTrue(published.current().isPresent());
        assertTrue(published.mayAdmit());

        active.set(false);
        assertTrue(published.current().isEmpty());
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        active.set(true);
        assertFalse(published.mayAdmit(), "Restoring the signal must not reopen a failed generation");
        assertTrue(published.current().isEmpty());
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
    }

}
