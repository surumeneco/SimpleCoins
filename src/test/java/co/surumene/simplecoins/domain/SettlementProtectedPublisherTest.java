package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

/** Bounded A01 product composition, not a certificate for all Paper/Plugin writers. */
final class SettlementProtectedPublisherTest {
    private static final UUID PLAYER = UUID.fromString(
            "00000000-0000-0000-0000-000000000101");

    private static SettlementAccumulator candidate(boolean complete) {
        var pending = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("real-source"));
        pending.accept(new SettlementAccumulator.Asset("real-source", "player-0",
                null, PLAYER, false, false, true, BigInteger.valueOf(5000),
                "player/inventory/0"));
        if (complete) pending.completeSource("real-source");
        return pending;
    }

    private static SettlementWriterEpochDecision.Evidence allEvidence() {
        return new SettlementWriterEpochDecision.Evidence(true, true, true, true,
                true, true, true, false);
    }

    // Reflective call means an absent product class yields the intended
    // behavior Red instead of a misleading javac error before implementation.
    private static final class Subject {
        private final Object delegate;
        private final java.lang.reflect.Method publish, current, mayAdmit, revoke;
        Subject(WriterEpochFence fence, WriterEpochFence.Seal seal,
                AdmissionBarrier barrier, BooleanSupplier journalActive,
                BooleanSupplier noOnline) throws Exception {
            Class<?> klass = Class.forName(
                    "co.surumene.simplecoins.domain.SettlementProtectedPublisher");
            Constructor<?> ctor = klass.getDeclaredConstructor(
                    WriterEpochFence.class, WriterEpochFence.Seal.class,
                    AdmissionBarrier.class, BooleanSupplier.class, BooleanSupplier.class);
            ctor.setAccessible(true);
            delegate = ctor.newInstance(fence, seal, barrier, journalActive, noOnline);
            publish = klass.getDeclaredMethod("evaluateAndPublish", long.class,
                    SettlementAccumulator.class, SettlementWriterEpochDecision.Evidence.class);
            publish.setAccessible(true);
            current = klass.getDeclaredMethod("current"); current.setAccessible(true);
            mayAdmit = klass.getDeclaredMethod("mayAdmit"); mayAdmit.setAccessible(true);
            revoke = klass.getDeclaredMethod("revoke"); revoke.setAccessible(true);
        }
        SettlementWriterEpochDecision.Verdict publish(
                long generation, SettlementAccumulator candidate,
                SettlementWriterEpochDecision.Evidence evidence) throws Throwable {
            try {
                return (SettlementWriterEpochDecision.Verdict) publish.invoke(
                        delegate, generation, candidate, evidence);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }
        @SuppressWarnings("unchecked")
        Optional<SettlementAccumulator.Result> current() throws Throwable {
            try { return (Optional<SettlementAccumulator.Result>) current.invoke(delegate); }
            catch (InvocationTargetException exception) { throw exception.getCause(); }
        }
        boolean mayAdmit() throws Throwable {
            try { return (boolean) mayAdmit.invoke(delegate); }
            catch (InvocationTargetException exception) { throw exception.getCause(); }
        }
        void revoke() throws Throwable {
            try { revoke.invoke(delegate); }
            catch (InvocationTargetException exception) { throw exception.getCause(); }
        }
    }

    @Test void publishUnderDrainedSealWithGenuineAccumulator() throws Throwable {
        var barrier = new AdmissionBarrier();
        long generation = barrier.begin();
        var fence = new WriterEpochFence(); fence.arm();
        var seal = fence.seal();
        var guarded = new Subject(fence, seal, barrier, () -> true, () -> true);
        assertFalse(guarded.mayAdmit());
        assertTrue(guarded.current().isEmpty());
        assertEquals(SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH,
                guarded.publish(generation, candidate(true), allEvidence()));
        assertEquals(AdmissionBarrier.State.READY, barrier.state());
        assertEquals(WriterEpochFence.State.SEALED, fence.state());
        var result = guarded.current().orElseThrow();
        assertEquals(BigInteger.valueOf(5000), result.statistics().worldTotal());
        assertEquals(Map.of(PLAYER, BigInteger.valueOf(5000)), result.personalBalances());
        assertTrue(guarded.mayAdmit());
    }

    @Test void holdDoesNotConsumeCandidate() throws Throwable {
        var gate = new AdmissionBarrier();
        long generation = gate.begin();
        var fence = new WriterEpochFence(); fence.arm();
        var seal = fence.seal();
        var subject = new Subject(fence, seal, gate, () -> true, () -> true);
        var pending = candidate(true);
        var badEvidence = new SettlementWriterEpochDecision.Evidence(
                false, true, true, true, true, true, true, false);
        assertEquals(SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED,
                subject.publish(generation, pending, badEvidence));
        assertEquals(AdmissionBarrier.State.SCANNING, gate.state());
        assertTrue(subject.current().isEmpty());
        assertEquals(SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH,
                subject.publish(generation, pending, allEvidence()),
                "HOLD must not call accumulator.commit or seal.resume");
        assertTrue(subject.current().isPresent());
    }

    @Test void rejectsUnsealedOrInFlightAndRevokes() throws Throwable {
        for (boolean inFlight : new boolean[]{false, true}) {
            var gate = new AdmissionBarrier(); long generation = gate.begin();
            var fence = new WriterEpochFence(); fence.arm();
            WriterEpochFence.Entry entry = inFlight ? fence.enterRoot() : null;
            var seal = inFlight ? fence.seal() : null;
            var subject = new Subject(fence, seal, gate, () -> true, () -> true);
            assertThrows(IllegalStateException.class,
                    () -> subject.publish(generation, candidate(true), allEvidence()));
            assertEquals(AdmissionBarrier.State.FAILED, gate.state());
            assertEquals(WriterEpochFence.State.FAULTED, fence.state());
            assertTrue(subject.current().isEmpty());
            if (entry != null) entry.normalReturn();
        }

        // Missing actual source must throw and never leave a readable partial.
        var gate = new AdmissionBarrier(); long generation = gate.begin();
        var fence = new WriterEpochFence(); fence.arm(); var seal = fence.seal();
        var subject = new Subject(fence, seal, gate, () -> true, () -> true);
        assertThrows(IllegalStateException.class,
                () -> subject.publish(generation, candidate(false), allEvidence()));
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        assertTrue(subject.current().isEmpty());

        var interruptedGate = new AdmissionBarrier();
        long gen = interruptedGate.begin();
        var another = new WriterEpochFence(); another.arm();
        var stale = another.seal(); another.resume(stale);
        var bad = new Subject(another, stale, interruptedGate, () -> true, () -> true);
        assertThrows(IllegalStateException.class,
                () -> bad.publish(gen, candidate(true), allEvidence()));
        assertEquals(AdmissionBarrier.State.FAILED, interruptedGate.state());
    }

    @Test void lateFaultRevokesVisibleResult() throws Throwable {
        var gate = new AdmissionBarrier(); long generation = gate.begin();
        var fence = new WriterEpochFence(); fence.arm(); var seal = fence.seal();
        var journal = new AtomicBoolean(true);
        var subject = new Subject(fence, seal, gate, journal::get, () -> true);
        subject.publish(generation, candidate(true), allEvidence());
        assertTrue(subject.current().isPresent());
        fence.invalidate();
        assertTrue(subject.current().isEmpty(),
                "A later native writer failure must not leave a cached permit");
        assertEquals(AdmissionBarrier.State.FAILED, gate.state());

        var gate2 = new AdmissionBarrier(); long gen2 = gate2.begin();
        var fence2 = new WriterEpochFence(); fence2.arm();
        var s2 = fence2.seal();
        var journal2 = new AtomicBoolean(true);
        var subject2 = new Subject(fence2, s2, gate2, journal2::get, () -> true);
        subject2.publish(gen2, candidate(true), allEvidence());
        journal2.set(false);
        assertFalse(subject2.mayAdmit());
        assertEquals(AdmissionBarrier.State.FAILED, gate2.state());
    }

    @Test void resumeCannotRaceWithAtomicCommit() throws Throwable {
        var gate = new AdmissionBarrier(); long generation = gate.begin();
        var fence = new WriterEpochFence(); fence.arm(); var seal = fence.seal();
        var callbackEntered = new CountDownLatch(1);
        var allowFinish = new CountDownLatch(1);
        BooleanSupplier noOnline = () -> {
            callbackEntered.countDown();
            try {
                if (!allowFinish.await(5, TimeUnit.SECONDS))
                    throw new IllegalStateException("Publication callback was stuck");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return true;
        };
        var subject = new Subject(fence, seal, gate, () -> true, noOnline);
        var result = new AtomicReference<Object>();
        Thread commit = new Thread(() -> {
            try { result.set(subject.publish(generation, candidate(true), allEvidence())); }
            catch (Throwable failure) { result.set(failure); }
        });
        commit.start();
        try {
            assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
            var resumeStarted = new CountDownLatch(1);
            var resumeDone = new CountDownLatch(1);
            Thread resume = new Thread(() -> {
                resumeStarted.countDown();
                try { fence.resume(seal); }
                finally { resumeDone.countDown(); }
            });
            resume.start();
            try {
                assertTrue(resumeStarted.await(5, TimeUnit.SECONDS));
                assertFalse(resumeDone.await(80, TimeUnit.MILLISECONDS),
                        "Resume cannot move epoch to OPEN during atomic commit");
            } finally {
                allowFinish.countDown();
            }
            commit.join(5000); resume.join(5000);
            assertFalse(commit.isAlive()); assertFalse(resume.isAlive());
            assertEquals(SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH, result.get());
            assertEquals(WriterEpochFence.State.OPEN, fence.state(),
                    "A later resume needs an external journal handoff, not this test's assertion");
            // Resume changed the epoch; the monitored publisher must revoke.
            assertTrue(subject.current().isEmpty());
            assertEquals(AdmissionBarrier.State.FAILED, gate.state());
        } finally {
            allowFinish.countDown();
            commit.join(5000);
        }
    }
}
