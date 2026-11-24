package co.surumene.simplecoins.paper;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Single-flight custody of a saved grant's off-main verification/ACK.
 * No queue of grants and no blocking main-thread join. A timed out operation
 * already inside a durable write may still commit: it is never retried and
 * its seal must not be returned. Shutdown does not interrupt durable IO.
 * This scheduling primitive supplies no writer-coverage or admission evidence.
 */
public final class SavedGrantCommitWorker implements AutoCloseable {
    public enum Status { RUNNING, SUCCEEDED, FAILED, ABANDONED, CLAIMED }

    @FunctionalInterface
    interface Commit<T> { T execute(BooleanSupplier live) throws Exception; }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "simplecoins-saved-grant-commit");
        thread.setDaemon(true);
        return thread;
    });
    private Ticket<?> active;
    private boolean closed;

    /** Only the actual saved-batch coordinator is allowed to submit in product code. */
    synchronized <T> Ticket<T> submit(long timeoutMillis, Commit<T> commit, Runnable faultSeal) {
        Objects.requireNonNull(commit, "saved grant operation");
        Objects.requireNonNull(faultSeal, "writer invalidation");
        if (timeoutMillis < 1 || timeoutMillis > 300_000)
            throw new IllegalArgumentException("Bounded saved grant timeout required");
        if (closed || active != null)
            throw new IllegalStateException("Saved grant worker closed or already owns a batch");
        Ticket<T> ticket = new Ticket<>(this, timeoutMillis);
        active = ticket;
        try {
            executor.execute(() -> {
                try {
                    if (ticket.live()) ticket.complete(commit.execute(ticket::live));
                } catch (Throwable failure) {
                    ticket.fail(failure);
                } finally {
                    if (ticket.status() != Status.SUCCEEDED && ticket.status() != Status.CLAIMED)
                        faultSeal.run(); // only worker thread, never block primary on writer monitor
                }
            });
            return ticket;
        } catch (RuntimeException | Error failure) {
            ticket.abandon();
            throw failure;
        }
    }

    private synchronized void claim(Ticket<?> ticket) {
        if (closed || active != ticket) throw new IllegalStateException("No current saved grant ticket");
        ticket.markClaimed();
        active = null;
    }

    /** Stop accepting work and abandon unclaimed custody. No await or interrupt. */
    @Override public synchronized void close() {
        closed = true;
        if (active != null) active.abandon();
        executor.shutdown();
    }

    public static final class Ticket<T> {
        private final SavedGrantCommitWorker owner;
        private final long deadline;
        private Status state = Status.RUNNING;
        private T value;
        private Throwable error;

        private Ticket(SavedGrantCommitWorker owner, long timeoutMillis) {
            this.owner = owner;
            deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        }

        /** Monotonic deadline; observation never waits for worker filesystem IO. */
        public synchronized Status status() {
            if ((state == Status.RUNNING || state == Status.SUCCEEDED)
                    && System.nanoTime() - deadline >= 0) {
                state = Status.ABANDONED;
                value = null;
            }
            return state;
        }

        private boolean live() { return status() == Status.RUNNING; }

        private synchronized void complete(T result) {
            if (live()) {
                value = Objects.requireNonNull(result, "saved grant proof");
                state = Status.SUCCEEDED;
            }
        }

        private synchronized void fail(Throwable failure) {
            if (live()) { error = failure; state = Status.FAILED; }
        }

        public synchronized T result() {
            if (status() != Status.SUCCEEDED)
                throw new IllegalStateException("No unclaimed successful saved grant proof");
            return value;
        }

        public synchronized Throwable failure() { return error; }

        synchronized void abandon() {
            if (state == Status.RUNNING || state == Status.SUCCEEDED) {
                state = Status.ABANDONED;
                value = null;
            }
        }

        void claimSuccess() { owner.claim(this); }

        private synchronized void markClaimed() {
            if (status() != Status.SUCCEEDED)
                throw new IllegalStateException("Saved grant proof cannot return its writer epoch");
            state = Status.CLAIMED;
        }
    }
}
