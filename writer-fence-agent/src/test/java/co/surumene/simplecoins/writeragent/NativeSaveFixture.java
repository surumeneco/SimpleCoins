package co.surumene.simplecoins.writeragent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Synchronize an already-admitted NMS body with a seal in another thread. */
public final class NativeSaveFixture {
    private static volatile CountDownLatch entered, release;
    private static volatile RuntimeException failNext;
    private NativeSaveFixture() {}
    public static void install(CountDownLatch e, CountDownLatch r) { entered=e; release=r; }
    public static void failNext(RuntimeException failure) { failNext = failure; }
    public static void onNmsBody() {
        RuntimeException failure = failNext;
        failNext = null;
        if (failure != null) throw failure;
        if (entered != null) {
            entered.countDown();
            try {
                if (!release.await(8, TimeUnit.SECONDS)) throw new AssertionError("Hold timed out");
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
        }
    }
}
