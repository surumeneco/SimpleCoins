package co.surumene.simplecoins.a02agent;

/**
 * Minimal bootstrap-visible target for the test-only Paper save gate.
 * This class has no references to plugins, agent transformers or Paper APIs.
 * It does NOT certify all native save or external writer entrypoints.
 */
public final class NativeSaveHook {
    private NativeSaveHook() {}

    // A single test-only already-admitted method invocation, never a product
    // writer counter. Hold occurs strictly AFTER checking entry is open and
    // BEFORE executing the real Paper save body.
    private static final java.util.concurrent.CountDownLatch HELD_CRAFT_ENTERED =
            new java.util.concurrent.CountDownLatch(1);
    private static final java.util.concurrent.CountDownLatch HELD_CRAFT_RELEASE =
            new java.util.concurrent.CountDownLatch(1);
    private static final java.util.concurrent.atomic.AtomicBoolean HELD_CRAFT_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean();

    public static boolean awaitCraftSaveEntry(long timeoutMillis) throws InterruptedException {
        return HELD_CRAFT_ENTERED.await(timeoutMillis,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static void releaseHeldCraftSave() {
        HELD_CRAFT_RELEASE.countDown();
    }

    public static void beforeNativeSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only Paper native save rejected during closed proof interval");
        }
        if (Boolean.getBoolean("a02.nativeAgent.holdInsideCraft")) {
            if (!HELD_CRAFT_ONCE.compareAndSet(false, true))
                throw new IllegalStateException("A02 held native save entered more than once");
            System.out.println("PAPER_A02_NATIVE_ALREADY_ADMITTED_CRAFT_SAVE_HELD_OPEN");
            HELD_CRAFT_ENTERED.countDown();
            try {
                if (!HELD_CRAFT_RELEASE.await(20, java.util.concurrent.TimeUnit.SECONDS))
                    throw new IllegalStateException("A02 already-admitted save hold timed out");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("A02 already-admitted save was interrupted",
                        interrupted);
            }
            if (!Boolean.getBoolean("a02.nativeAgent.closed"))
                throw new IllegalStateException("A02 held save released before admission closure");
            System.out.println("PAPER_A02_NATIVE_ALREADY_ADMITTED_CRAFT_SAVE_RELEASED_CLOSED");
        }
    }

    /** Separate NMS internal playerdata writer entry, not a universal IO fence. */
    public static void beforeNmsSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_NMS_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only NMS PlayerDataStorage save rejected during closed interval");
        }
    }
}
