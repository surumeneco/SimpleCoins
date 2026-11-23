package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe admission/drain primitive for a single instrumented writer family.
 *
 * <p>Initially denies every writer. A caller must independently verify that all
 * applicable bytecode hooks were installed, including asynchronous and direct
 * storage writers, before calling {@link #arm()}. A drained seal proves only
 * that methods which actually entered this object have returned normally;
 * it is NOT an A01/G0 whole-world writer-closure certificate, a safe-to-publish
 * decision, a grant ACK permission, or a player admission permit.</p>
 *
 * <p>The single monitor linearizes root entry and closure. A child may enter
 * after closure only from an already-admitted parent on the same thread. Async
 * handoffs require separately tracked root entry before the closure boundary;
 * ThreadLocal ancestry must never be passed as asynchronous authorization.</p>
 */
public final class WriterEpochFence {
    public enum State { DISARMED, OPEN, SEALED, FAULTED }

    /** An opaque, generation-bound reference, not evidence of complete hook coverage. */
    public static final class Seal {
        private final WriterEpochFence owner;
        private final long epoch;
        private Seal(WriterEpochFence owner, long epoch) {
            this.owner = owner;
            this.epoch = epoch;
        }
    }

    /** Instrumentation must call exactly one terminal method per admitted invocation. */
    public final class Entry {
        private final Thread thread = Thread.currentThread();
        private final long epoch;
        private boolean terminal;
        private Entry(long epoch) { this.epoch = epoch; }

        public void normalReturn() { finish(false); }
        public void exceptionalReturn() { finish(true); }

        private void finish(boolean exceptional) {
            synchronized (lock) {
                var stack = ancestry.get();
                if (terminal || thread != Thread.currentThread() ||
                        stack.peekLast() != this || epoch != generation || active < 1) {
                    fault();
                    throw new IllegalStateException("Writer completion mismatch; epoch invalidated");
                }
                terminal = true;
                stack.removeLast();
                if (stack.isEmpty()) ancestry.remove();
                active--;
                if (exceptional) fault();
                lock.notifyAll();
            }
        }
    }

    private final Object lock = new Object();
    private final ThreadLocal<ArrayDeque<Entry>> ancestry =
            ThreadLocal.withInitial(ArrayDeque::new);
    private State state = State.DISARMED;
    private long generation;
    private int active;
    // Only short, synchronous final-publication callbacks may hold this lock.
    private boolean atomicPublicationActive;
    // Exclusive owner can keep the epoch SEALED while slow bounded IO runs
    // without pinning the monitor required by Agent admission/invalidations.
    private Thread durableLeaseOwner;

    public State state() { synchronized (lock) { return state; } }

    public int inFlight() { synchronized (lock) { return active; } }

    /** Start a new epoch only after the external hook-coverage audit. */
    public void arm() {
        synchronized (lock) {
            if (state != State.DISARMED || active != 0)
                throw new IllegalStateException("Writer fence not initially disarmed");
            advanceEpoch();
            state = State.OPEN;
        }
    }

    /** Root admission always fails once the seal has been acquired. */
    public Entry enterRoot() {
        synchronized (lock) {
            if (state != State.OPEN || !ancestry.get().isEmpty())
                throw new IllegalStateException("New or recursive writer root not admitted");
            return beginEntry();
        }
    }

    /** Same-thread synchronous child is admitted under an existing root. */
    public Entry enterNested(Entry parent) {
        Objects.requireNonNull(parent, "parent entry");
        synchronized (lock) {
            var stack = ancestry.get();
            if ((state != State.OPEN && state != State.SEALED) ||
                    parent.thread != Thread.currentThread() || parent.terminal ||
                    parent.epoch != generation || stack.peekLast() != parent)
                throw new IllegalStateException("Nested writer lacks a live admitted parent");
            return beginEntry();
        }
    }

    private Entry beginEntry() {
        active = Math.addExact(active, 1);
        Entry entry = new Entry(generation);
        ancestry.get().addLast(entry);
        return entry;
    }

    /** Close new root admission atomically with entries already in flight. */
    public Seal seal() {
        synchronized (lock) {
            if (state != State.OPEN)
                throw new IllegalStateException("Writer fence cannot be sealed");
            state = State.SEALED;
            return new Seal(this, generation);
        }
    }

    /** Return false on timeout. A timeout does not re-open admissions. */
    public boolean awaitDrained(Seal seal, long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 1 || timeoutMillis > 30000)
            throw new IllegalArgumentException("Bounded drain timeout required");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        synchronized (lock) {
            requireSeal(seal);
            while (active != 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return false;
                TimeUnit.NANOSECONDS.timedWait(lock, remaining);
                requireSeal(seal);
            }
            return true;
        }
    }

    /** Check seal identity and complete drain, without minting A01 evidence. */
    public void requireDrained(Seal seal) {
        synchronized (lock) {
            requireSeal(seal);
            if (active != 0)
                throw new IllegalStateException("Writer epoch still has admitted invocations");
        }
    }

    /** Checked saved-file readback that may fail with genuine storage IO. */
    @FunctionalInterface
    public interface DurableLeaseWork<T> {
        T run() throws IOException;
    }

    /**
     * Keep a drained SEAL exclusive across a potentially slow file/receipt
     * readback without retaining the internal Agent-admission monitor while
     * filesystem IO is in progress. Root admission stays denied (SEALED),
     * and any independent resume fails rather than reopening this epoch.
     *
     * The owning thread alone may enter withDrainedSeal inside the lease for
     * the final short durable ACK operation. Neither this lease nor that
     * operation can prove coverage of unhooked native/Plugin/OS writers.
     */
    public <T> T withDrainedLease(Seal seal, DurableLeaseWork<T> action) throws IOException {
        Objects.requireNonNull(action, "protected saved-file operation");
        synchronized (lock) {
            requireSeal(seal);
            if (active != 0 || durableLeaseOwner != null || atomicPublicationActive)
                throw new IllegalStateException("Writer epoch cannot begin exclusive IO lease");
            durableLeaseOwner = Thread.currentThread();
        }
        try {
            T result = action.run();
            synchronized (lock) {
                requireSeal(seal);
                if (active != 0)
                    throw new IllegalStateException("Admitted writer after exclusive IO lease");
            }
            return result;
        } catch (IOException | RuntimeException | Error failure) {
            invalidate();
            throw failure;
        } finally {
            synchronized (lock) {
                durableLeaseOwner = null;
                lock.notifyAll();
            }
        }
    }

    /**
     * Run only the short, synchronous FINAL commit of an already staged
     * candidate while the same monitor excludes both root entry and seal
     * resumption. A pair of requireDrained calls outside this lock could
     * otherwise allow OPEN to interleave between the check and commit.
     *
     * NO disk scan, NBT IO, Bukkit task dispatch or waiting for another writer
     * may occur in the callback. The caller must prove complete writer-hook
     * coverage and external journal readiness independently. This method is
     * not a whole-world G0 authority or an admission permit.
     */
    public <T> T withDrainedSeal(Seal seal, Supplier<T> finalCommit) {
        Objects.requireNonNull(finalCommit, "atomic final publication");
        synchronized (lock) {
            requireSeal(seal);
            if (active != 0 || atomicPublicationActive ||
                    (durableLeaseOwner != null && durableLeaseOwner != Thread.currentThread()))
                throw new IllegalStateException("Writer epoch not ready for atomic commit");
            atomicPublicationActive = true;
            try {
                T committed = finalCommit.get();
                requireSeal(seal);
                if (active != 0)
                    throw new IllegalStateException("Writer admitted during atomic commit");
                return committed;
            } catch (RuntimeException | Error failure) {
                fault();
                throw failure;
            } finally {
                atomicPublicationActive = false;
            }
        }
    }

    /** End a protected interval; the old seal becomes permanently stale. */
    public void resume(Seal seal) {
        synchronized (lock) {
            if (atomicPublicationActive || durableLeaseOwner != null)
                throw new IllegalStateException("Cannot resume an exclusively held writer epoch");
            requireDrained(seal);
            advanceEpoch();
            state = State.OPEN;
        }
    }

    /** An observed escape or exceptional writer completion irrevocably fails closed. */
    public void invalidate() {
        synchronized (lock) { fault(); }
    }

    private void requireSeal(Seal seal) {
        if (seal == null || seal.owner != this ||
                seal.epoch != generation || state != State.SEALED)
            throw new IllegalStateException("Stale or invalid writer epoch seal");
    }

    private void advanceEpoch() {
        try { generation = Math.addExact(generation, 1); }
        catch (ArithmeticException exhausted) {
            fault();
            throw new IllegalStateException("Writer epoch overflow", exhausted);
        }
    }

    private void fault() {
        state = State.FAULTED;
        lock.notifyAll();
    }
}
