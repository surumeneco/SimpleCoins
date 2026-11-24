package co.surumene.simplecoins.paper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Same executable checks for direct JDK and JUnit runners. */
public final class SavedGrantCommitWorkerChecks {
    private SavedGrantCommitWorkerChecks() {}
    public static void main(String[] args) throws Exception {
        successAndExclusion(); expiryAndShutdown(); unclaimedSuccessExpires(); failures();
        System.out.println("SAVED_GRANT_COMMIT_WORKER_CHECKS_PASSED");
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejected(Runnable run) {
        try { run.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("Expected fail-closed rejection");
    }
    private static void await(CountDownLatch latch) throws InterruptedException {
        check(latch.await(5, TimeUnit.SECONDS));
    }
    private static void terminal(SavedGrantCommitWorker.Ticket<?> ticket) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (ticket.status() == SavedGrantCommitWorker.Status.RUNNING) {
            if (System.nanoTime() - end >= 0) throw new AssertionError("Worker did not terminate");
            Thread.sleep(1);
        }
    }
    private static void successAndExclusion() throws Exception {
        Thread caller = Thread.currentThread();
        try (var worker = new SavedGrantCommitWorker()) {
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
            var ticket = worker.submit(10000, live -> {
                check(Thread.currentThread() != caller); entered.countDown(); await(release);
                check(live.getAsBoolean()); return "ack";
            }, () -> { throw new AssertionError("Successful work abandoned"); });
            await(entered);
            check(ticket.status() == SavedGrantCommitWorker.Status.RUNNING);
            rejected(() -> worker.submit(1000, live -> "wrong", () -> {}));
            rejected(ticket::result);
            rejected(ticket::claimSuccess);
            release.countDown(); terminal(ticket);
            check(ticket.status() == SavedGrantCommitWorker.Status.SUCCEEDED);
            check(ticket.result().equals("ack"));
            rejected(() -> worker.submit(1000, live -> "wrong", () -> {}));
            ticket.claimSuccess(); rejected(ticket::claimSuccess);
            var next = worker.submit(10000, live -> "next", () -> {});
            terminal(next); check(next.result().equals("next")); next.claimSuccess();
        }
    }
    private static void expiryAndShutdown() throws Exception {
        for (boolean shutdown : new boolean[]{false, true}) {
            try (var worker = new SavedGrantCommitWorker()) {
                var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
                var retired = new CountDownLatch(1); var wrote = new AtomicBoolean();
                var ticket = worker.submit(shutdown ? 10000 : 1000, live -> {
                    entered.countDown(); await(release);
                    if (live.getAsBoolean()) wrote.set(true);
                    return "late";
                }, retired::countDown);
                await(entered);
                if (shutdown) worker.close(); else terminal(ticket);
                check(ticket.status() == SavedGrantCommitWorker.Status.ABANDONED);
                rejected(ticket::result); rejected(ticket::claimSuccess);
                rejected(() -> worker.submit(1000, live -> "wrong", () -> {}));
                release.countDown(); await(retired); check(!wrote.get());
            }
        }
        var worker = new SavedGrantCommitWorker();
        var completed = worker.submit(10000, live -> "ack", () -> {});
        terminal(completed); check(completed.status() == SavedGrantCommitWorker.Status.SUCCEEDED);
        worker.close(); check(completed.status() == SavedGrantCommitWorker.Status.ABANDONED);
        rejected(completed::claimSuccess);
    }
    private static void unclaimedSuccessExpires() throws Exception {
        try (var worker = new SavedGrantCommitWorker()) {
            var ticket = worker.submit(300, live -> "complete", () -> {});
            terminal(ticket); check(ticket.status() == SavedGrantCommitWorker.Status.SUCCEEDED);
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (ticket.status() == SavedGrantCommitWorker.Status.SUCCEEDED
                    && System.nanoTime() < until) Thread.sleep(5);
            check(ticket.status() == SavedGrantCommitWorker.Status.ABANDONED);
            rejected(ticket::result); rejected(ticket::claimSuccess);
            rejected(() -> worker.submit(1000, live -> "wrong", () -> {}));
        }
    }
    private static void failures() throws Exception {
        try (var worker = new SavedGrantCommitWorker()) {
            var ticket = worker.submit(10000, live -> { throw new java.io.IOException("disk"); }, () -> {});
            terminal(ticket); check(ticket.status() == SavedGrantCommitWorker.Status.FAILED);
            check(ticket.failure() instanceof java.io.IOException);
            rejected(ticket::result); rejected(ticket::claimSuccess);
            rejected(() -> worker.submit(1000, live -> "wrong", () -> {}));
        }
    }
}
