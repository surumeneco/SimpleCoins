package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Fault injection against the experimental coordinator, not Paper integration. */
class SettlementStartupCoordinatorTest {
    private static final class Queue implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        @Override public void execute(Runnable work) { tasks.add(work); }
        void next() { assertFalse(tasks.isEmpty()); tasks.remove().run(); }
    }

    @Test void queuedWorkRemainsClosedAndStageThenPublishOpens() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicBoolean published = new AtomicBoolean();
        SettlementStartupCoordinator<String> startup = new SettlementStartupCoordinator<>(
                barrier, disk, main, () -> "validated",
                value -> { assertEquals("validated", value); assertFalse(barrier.mayAdmit()); published.set(true); },
                () -> true, failure -> fail(failure));
        startup.start();
        assertFalse(barrier.mayAdmit());
        assertFalse(published.get());
        disk.next();
        assertFalse(barrier.mayAdmit());
        assertFalse(published.get());
        main.next();
        assertTrue(published.get());
        assertTrue(barrier.mayAdmit());
        assertTrue(startup.failure().isEmpty());
        assertThrows(IllegalStateException.class, startup::start);
    }

    @Test void scannerFailureFailsClosedAndSignalsOnce() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicInteger signals = new AtomicInteger();
        SettlementStartupCoordinator<String> startup = new SettlementStartupCoordinator<>(
                barrier, disk, main, () -> { throw new IOException("NBT corrupted"); },
                ignored -> fail("publication must not run"), () -> true,
                ignored -> signals.incrementAndGet());
        startup.start();
        disk.next();
        assertFalse(barrier.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertEquals("NBT corrupted", startup.failure().orElseThrow().getMessage());
        assertEquals(1, signals.get());
        assertTrue(main.tasks.isEmpty());
    }

    @Test void nullStageResultFailsClosed() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicInteger failures = new AtomicInteger();
        var coordinator = new SettlementStartupCoordinator<>(barrier, disk, main,
                () -> null, value -> fail(), () -> true, ignored -> failures.incrementAndGet());
        coordinator.start(); disk.next();
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertEquals(1, failures.get());
        assertTrue(coordinator.failure().isPresent());
        assertTrue(main.tasks.isEmpty());
    }

    @Test void onlinePlayerStopsPublicationAndNeverOpensAdmission() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicBoolean published = new AtomicBoolean();
        var coordinator = new SettlementStartupCoordinator<>(barrier, disk, main,
                () -> "valid", value -> published.set(true), () -> false, ignored -> {});
        coordinator.start(); disk.next(); main.next();
        assertFalse(published.get());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertFalse(barrier.mayAdmit());
    }

    @Test void failedPublisherNeverOpens() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicInteger signals = new AtomicInteger();
        var coordinator = new SettlementStartupCoordinator<>(barrier, disk, main,
                () -> "verified", value -> { throw new IOException("Atomic swap failed"); },
                () -> true, ignored -> signals.incrementAndGet());
        coordinator.start(); disk.next(); main.next();
        assertFalse(barrier.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertEquals("Atomic swap failed", coordinator.failure().orElseThrow().getMessage());
        assertEquals(1, signals.get());
    }

    @Test void onlinePlayerAppearingDuringPublicationStillCloses() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicBoolean zeroOnline = new AtomicBoolean(true);
        AtomicInteger signals = new AtomicInteger();
        var coordinator = new SettlementStartupCoordinator<>(barrier, disk, main,
                () -> "verified", value -> zeroOnline.set(false),
                zeroOnline::get, ignored -> signals.incrementAndGet());
        coordinator.start(); disk.next(); main.next();
        assertFalse(barrier.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
        assertEquals(1, signals.get());
    }

    @Test void rejectedWorkerOrMainThreadQueueFailsClosed() {
        for (boolean rejectBackground : new boolean[]{true, false}) {
            AdmissionBarrier barrier = new AdmissionBarrier();
            Queue disk = new Queue();
            Executor rejecting = task -> { throw new RejectedExecutionException("executor stopped"); };
            AtomicInteger signals = new AtomicInteger();
            var coordinator = new SettlementStartupCoordinator<>(barrier,
                    rejectBackground ? rejecting : disk,
                    rejectBackground ? disk : rejecting,
                    () -> "valid", value -> fail(), () -> true,
                    ignored -> signals.incrementAndGet());
            coordinator.start();
            if (!rejectBackground) disk.next();
            assertEquals(AdmissionBarrier.State.FAILED, barrier.state());
            assertFalse(barrier.mayAdmit());
            assertEquals(1, signals.get());
        }
    }

    @Test void shutdownBeforePublishRejectsStaleWork() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        Queue disk = new Queue(), main = new Queue();
        AtomicBoolean published = new AtomicBoolean();
        var coordinator = new SettlementStartupCoordinator<>(barrier, disk, main,
                () -> "valid", value -> published.set(true), () -> true, ignored -> {});
        coordinator.start();
        disk.next();
        barrier.stop();
        main.next();
        assertFalse(published.get());
        assertEquals(AdmissionBarrier.State.STOPPED, barrier.state());
        assertFalse(barrier.mayAdmit());
    }
}
