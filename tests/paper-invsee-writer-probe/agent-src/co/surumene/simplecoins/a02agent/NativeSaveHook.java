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

    // Active entry accounting for exactly ONE instrumented method.
    // The gate lock makes close-vs-enter a single linearizable decision.
    // This is a throwaway proof, not a deployed Paper writer coordinator.
    private static final Object CRAFT_DRAIN_LOCK = new Object();
    private static final ThreadLocal<Boolean> TRACKED_ENTRY = new ThreadLocal<>();
    private static int activeCraftSaves;
    private static int normalCraftReturns;
    private static boolean trackedAdmissionsClosed;

    public static int trackedCraftInFlight() {
        synchronized (CRAFT_DRAIN_LOCK) {
            return activeCraftSaves;
        }
    }

    public static int trackedCraftNormalReturns() {
        synchronized (CRAFT_DRAIN_LOCK) {
            return normalCraftReturns;
        }
    }

    public static void closeTrackedCraftAdmissions() {
        synchronized (CRAFT_DRAIN_LOCK) {
            if (!Boolean.getBoolean("a02.nativeAgent.trackCraftDrain")
                    || trackedAdmissionsClosed)
                throw new IllegalStateException("Native tracked gate not open");
            trackedAdmissionsClosed = true;
            System.setProperty("a02.nativeAgent.closed", "true");
            System.out.println("PAPER_A02_TRACKED_NATIVE_ENTRY_CLOSED_ACTIVE_"
                    + activeCraftSaves);
        }
    }

    public static boolean awaitTrackedCraftDrain(long millis) throws InterruptedException {
        if (millis < 1 || millis > 30000)
            throw new IllegalArgumentException("Invalid bounded drain timeout");
        long deadline = System.nanoTime()
                + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(millis);
        synchronized (CRAFT_DRAIN_LOCK) {
            if (!trackedAdmissionsClosed)
                throw new IllegalStateException("Cannot drain before closing admissions");
            while (activeCraftSaves != 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return false;
                long timeoutMillis = Math.max(1L,
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining));
                CRAFT_DRAIN_LOCK.wait(timeoutMillis);
            }
            return true;
        }
    }

    public static void afterNativeSave() {
        // Injected by the JVM Agent ONLY at the actual normal-return opcode,
        // not when the original method body was merely released to run.
        if (!Boolean.getBoolean("a02.nativeAgent.trackCraftDrain")) return;
        synchronized (CRAFT_DRAIN_LOCK) {
            if (!Boolean.TRUE.equals(TRACKED_ENTRY.get()) || activeCraftSaves < 1)
                throw new IllegalStateException("Unmatched real native save normal return");
            TRACKED_ENTRY.remove();
            activeCraftSaves--;
            normalCraftReturns++;
            CRAFT_DRAIN_LOCK.notifyAll();
        }
    }

    public static void beforeNativeSave() {
        if (Boolean.getBoolean("a02.nativeAgent.trackCraftDrain")) {
            synchronized (CRAFT_DRAIN_LOCK) {
                if (trackedAdmissionsClosed || Boolean.getBoolean("a02.nativeAgent.closed")) {
                    System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_SAVE_DENIED");
                    throw new IllegalStateException(
                            "A02 test-only Paper native save rejected during closed proof interval");
                }
                if (Boolean.TRUE.equals(TRACKED_ENTRY.get()))
                    throw new IllegalStateException("Recursive tracked Paper save entry");
                TRACKED_ENTRY.set(Boolean.TRUE);
                activeCraftSaves++;
            }
        } else if (Boolean.getBoolean("a02.nativeAgent.closed")) {
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
