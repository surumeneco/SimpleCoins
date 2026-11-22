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


    // Independent NMS-root proof only. Hold AFTER the real NMS root has
    // entered the shared family counter and BEFORE it writes playerdata.
    // Never enabled by the normal nested-Craft test or production plugin.
    private static final java.util.concurrent.CountDownLatch HELD_NMS_ENTERED =
            new java.util.concurrent.CountDownLatch(1);
    private static final java.util.concurrent.CountDownLatch HELD_NMS_RELEASE =
            new java.util.concurrent.CountDownLatch(1);
    private static final java.util.concurrent.atomic.AtomicBoolean HELD_NMS_ONCE =
            new java.util.concurrent.atomic.AtomicBoolean();

    public static boolean awaitNmsSaveEntry(long timeoutMillis) throws InterruptedException {
        return HELD_NMS_ENTERED.await(timeoutMillis,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public static void releaseHeldNmsSave() {
        HELD_NMS_RELEASE.countDown();
    }

    // Shared, test-only gate for the exact synchronous CraftPlayer -> NMS
    // method nesting. Thread-local ancestry is NOT an async handoff token.
    private static final Object FAMILY_LOCK = new Object();
    private static final ThreadLocal<java.util.ArrayDeque<String>> FAMILY_STACK =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static int familyActive;
    private static int familyCraftReturns;
    private static int familyNmsReturns;
    private static int familyNestedNms;
    private static int familyIndependentDenials;
    private static boolean familyClosed;

    public static int familyInFlight() {
        synchronized (FAMILY_LOCK) { return familyActive; }
    }

    public static int familyCraftNormalReturns() {
        synchronized (FAMILY_LOCK) { return familyCraftReturns; }
    }

    public static int familyNmsNormalReturns() {
        synchronized (FAMILY_LOCK) { return familyNmsReturns; }
    }

    public static int familyNestedNmsAdmissions() {
        synchronized (FAMILY_LOCK) { return familyNestedNms; }
    }

    public static int familyIndependentNmsDenials() {
        synchronized (FAMILY_LOCK) { return familyIndependentDenials; }
    }

    public static void closeNativeFamilyAdmissions() {
        synchronized (FAMILY_LOCK) {
            if (!Boolean.getBoolean("a02.nativeAgent.trackNativeFamily") || familyClosed)
                throw new IllegalStateException("Native test family gate was not open");
            familyClosed = true;
            System.setProperty("a02.nativeAgent.closed", "true");
            System.out.println("PAPER_A02_NATIVE_FAMILY_CLOSED_ACTIVE_" + familyActive);
        }
    }

    public static boolean awaitNativeFamilyDrain(long millis) throws InterruptedException {
        if (millis < 1 || millis > 30000)
            throw new IllegalArgumentException("Invalid native family drain timeout");
        long deadline = System.nanoTime()
                + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(millis);
        synchronized (FAMILY_LOCK) {
            if (!familyClosed)
                throw new IllegalStateException("Native family must be closed before drain");
            while (familyActive != 0) {
                long remain = deadline - System.nanoTime();
                if (remain <= 0) return false;
                FAMILY_LOCK.wait(Math.max(1L,
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remain)));
            }
            return true;
        }
    }

    // A test-only protected operation of one already CLOSED+drained native
    // save family. No token from this scope authorizes a production grant.
    private static boolean nativeGuardedActionActive;
    private static boolean nativeGuardedActionFailed;

    public static void runUnderClosedNativeFamily(Runnable action) {
        java.util.Objects.requireNonNull(action, "native protected callback");
        synchronized (FAMILY_LOCK) {
            if (!Boolean.getBoolean("a02.nativeAgent.trackNativeFamily")
                    || !familyClosed || familyActive != 0
                    || nativeGuardedActionActive || nativeGuardedActionFailed
                    || familyCraftReturns < 1 || familyNmsReturns < 1
                    || familyNestedNms < 1)
                throw new IllegalStateException("Real native save family is not fully closed/drained");
            nativeGuardedActionActive = true;
            try {
                action.run();
                if (familyActive != 0 || !familyClosed)
                    throw new IllegalStateException("Native writer scope changed during callback");
            } catch (RuntimeException | Error failed) {
                nativeGuardedActionFailed = true;
                throw failed;
            } finally {
                nativeGuardedActionActive = false;
            }
        }
    }

    private static void enterNativeFamily(String method) {
        synchronized (FAMILY_LOCK) {
            java.util.ArrayDeque<String> callStack = FAMILY_STACK.get();
            boolean nestedNms = "NMS".equals(method)
                    && callStack.size() == 1 && "CRAFT".equals(callStack.peekLast());
            boolean independent = callStack.isEmpty();
            if (!independent && !nestedNms)
                throw new IllegalStateException("Unrecognized native save recursion/ancestry");
            // After closing, only the NMS child of an already counted
            // Craft invocation on THE SAME thread inherits admission.
            if (familyClosed || Boolean.getBoolean("a02.nativeAgent.closed")) {
                if (!nestedNms) {
                    if ("NMS".equals(method)) familyIndependentDenials++;
                    System.out.println("PAPER_A02_NATIVE_FAMILY_NEW_" + method + "_DENIED");
                    throw new IllegalStateException("A02 native family closed to new root writers");
                }
            }
            callStack.addLast(method);
            familyActive++;
            if (nestedNms) {
                familyNestedNms++;
                System.out.println("PAPER_A02_NATIVE_FAMILY_ACTUAL_NMS_NESTED_ADMITTED_CLOSED_"
                        + familyClosed);
            }
        }
    }

    private static void exitNativeFamily(String method) {
        synchronized (FAMILY_LOCK) {
            java.util.ArrayDeque<String> stack = FAMILY_STACK.get();
            if (familyActive < 1 || !method.equals(stack.peekLast()))
                throw new IllegalStateException("Unmatched native family normal-return scope");
            stack.removeLast();
            if (stack.isEmpty()) FAMILY_STACK.remove();
            familyActive--;
            if ("CRAFT".equals(method)) familyCraftReturns++;
            else familyNmsReturns++;
            FAMILY_LOCK.notifyAll();
        }
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
        if (Boolean.getBoolean("a02.nativeAgent.trackNativeFamily")) {
            exitNativeFamily("CRAFT");
            return;
        }
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
        if (Boolean.getBoolean("a02.nativeAgent.trackNativeFamily")) {
            enterNativeFamily("CRAFT");
        } else if (Boolean.getBoolean("a02.nativeAgent.trackCraftDrain")) {
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
        if (Boolean.getBoolean("a02.nativeAgent.trackNativeFamily")) {
            enterNativeFamily("NMS");
            if (Boolean.getBoolean("a02.nativeAgent.holdInsideNms")) {
                if (!HELD_NMS_ONCE.compareAndSet(false, true))
                    throw new IllegalStateException("A02 held NMS root entered more than once");
                System.out.println("PAPER_A02_NATIVE_ALREADY_ADMITTED_NMS_ROOT_HELD_OPEN");
                HELD_NMS_ENTERED.countDown();
                try {
                    if (!HELD_NMS_RELEASE.await(20, java.util.concurrent.TimeUnit.SECONDS))
                        throw new IllegalStateException("A02 admitted NMS root hold timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("A02 admitted NMS root interrupted", interrupted);
                }
                if (!Boolean.getBoolean("a02.nativeAgent.closed"))
                    throw new IllegalStateException("NMS root released before admission closure");
                System.out.println("PAPER_A02_NATIVE_ALREADY_ADMITTED_NMS_ROOT_RELEASED_CLOSED");
            }
            return;
        }
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_NMS_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only NMS PlayerDataStorage save rejected during closed interval");
        }
    }

    /** Invoked ONLY at the actual normal return of the instrumented NMS save. */
    public static void afterNmsSave() {
        if (Boolean.getBoolean("a02.nativeAgent.trackNativeFamily"))
            exitNativeFamily("NMS");
    }
}
