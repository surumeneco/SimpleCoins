package co.surumene.simplecoins.a01agent;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bootstrap-visible, test-only monitor for two known WCL owner write methods.
 * Deliberately retains an exceptional invocation as active (fail closed);
 * no publication or grant ACK is authorized by these counts.
 */
public final class LiveOwnerWriterHook {
    private static final Object LOCK = new Object();
    private static final ThreadLocal<ArrayDeque<String>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static int active;
    private static int wwcReturns;
    private static int wwwReturns;
    private static int pdcReturns;
    private static int equipmentReturns;
    private static int inventoryReturns;
    private static int wwcDenied;
    private static int wwwDenied;
    private static int pdcDenied;
    private static int equipmentDenied;
    private static int inventoryDenied;
    private static boolean closed;
    private static final Set<String> REQUIRED_KNOWN_TARGETS = Set.of(
            "WWC", "WWW", "PDC", "EQUIPMENT", "INVENTORY");
    private static final Set<String> observedTransformedTargets = new HashSet<>();
    private static long coverageRevision;
    private static long closedGeneration;

    // Probe-only one-shot hold. The control thread never calls Bukkit APIs.
    private static String heldWriter;
    private static CountDownLatch heldEntered;
    private static CountDownLatch heldRelease;
    private static boolean holdConsumed;

    private LiveOwnerWriterHook() {}

    /**
     * Called only by the actual Java 25 Instrumentation transformer after
     * matching the exact target class and method descriptor count. No caller
     * can manufacture a successful live method return with this operation.
     */
    public static void registerObservedTransformation(String target) {
        synchronized (LOCK) {
            if (!REQUIRED_KNOWN_TARGETS.contains(target) || closed
                    || !observedTransformedTargets.add(target))
                throw new IllegalStateException("Unexpected or duplicate observed writer target");
            coverageRevision++;
        }
    }

    private static boolean knownTargetsTransformedAndReturned() {
        return observedTransformedTargets.equals(REQUIRED_KNOWN_TARGETS)
                && wwcReturns > 0 && wwwReturns > 0 && pdcReturns > 0
                && equipmentReturns > 0 && inventoryReturns > 0;
    }

    /** A non-constructible-by-callers token for FIVE audited methods, not global G0. */
    public static final class KnownSourcesClosedEpoch {
        private final long generation;
        private final long revision;
        private KnownSourcesClosedEpoch(long generation, long revision) {
            if (generation <= 0 || revision != REQUIRED_KNOWN_TARGETS.size())
                throw new IllegalArgumentException("Unverified known writer source epoch");
            this.generation = generation;
            this.revision = revision;
        }
    }

    public static KnownSourcesClosedEpoch mintKnownSourcesClosedEpoch() {
        synchronized (LOCK) {
            if (!closed || active != 0 || !knownTargetsTransformedAndReturned())
                throw new IllegalStateException("Missing observed known writer coverage or drain");
            return new KnownSourcesClosedEpoch(closedGeneration, coverageRevision);
        }
    }

    public static boolean knownEpochStillCurrent(KnownSourcesClosedEpoch proof) {
        synchronized (LOCK) {
            return proof != null && closed && active == 0
                    && proof.generation == closedGeneration
                    && proof.revision == coverageRevision
                    && knownTargetsTransformedAndReturned();
        }
    }

    public static boolean knownCoverageComplete() {
        synchronized (LOCK) { return knownTargetsTransformedAndReturned(); }
    }

    public static void beforeWWC() { enter("WWC"); }
    public static void afterWWC() { leave("WWC"); }
    public static void beforeWWW() { enter("WWW"); }
    public static void afterWWW() { leave("WWW"); }
    public static void beforePdcMutation() { enter("PDC"); }
    public static void afterPdcMutation() { leave("PDC"); }
    public static void beforeEquipmentMutation() { enter("EQUIPMENT"); }
    public static void afterEquipmentMutation() { leave("EQUIPMENT"); }
    public static void beforeInventoryMutation() { enter("INVENTORY"); }
    public static void afterInventoryMutation() { leave("INVENTORY"); }

    private static void enter(String writer) {
        CountDownLatch entered = null;
        CountDownLatch release = null;
        synchronized (LOCK) {
            // Only an already-admitted, same-thread invocation can inherit a
            // closed gate. This is NOT an async handoff token.
            ArrayDeque<String> callStack = STACK.get();
            if (closed && callStack.isEmpty()) {
                switch (writer) {
                    case "WWC" -> wwcDenied++;
                    case "WWW" -> wwwDenied++;
                    case "PDC" -> pdcDenied++;
                    case "EQUIPMENT" -> equipmentDenied++;
                    case "INVENTORY" -> inventoryDenied++;
                    default -> throw new IllegalStateException("Unknown writer type");
                }
                System.out.println("PAPER_A01_LIVE_AGENT_" + writer + "_ENTRY_DENIED");
                throw new IllegalStateException("A01 live provider writer fenced");
            }
            callStack.addLast(writer);
            active++;
            if (writer.equals(heldWriter) && !holdConsumed) {
                holdConsumed = true;
                entered = heldEntered;
                release = heldRelease;
            }
        }
        if (release != null) {
            entered.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS))
                    throw new IllegalStateException("A01 already-admitted owner writer hold timed out");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "A01 admitted owner writer interrupted", interrupted);
            }
            // On exceptional completion the active scope stays nonzero.
            System.out.println("PAPER_A01_LIVE_AGENT_" + writer
                    + "_ALREADY_ADMITTED_RELEASED_CLOSED_" + isClosed());
        }
    }

    private static void leave(String writer) {
        synchronized (LOCK) {
            ArrayDeque<String> stack = STACK.get();
            if (active <= 0 || !writer.equals(stack.peekLast()))
                throw new IllegalStateException("A01 live provider hook return unmatched");
            stack.removeLast();
            if (stack.isEmpty()) STACK.remove();
            active--;
            switch (writer) {
                case "WWC" -> wwcReturns++;
                case "WWW" -> wwwReturns++;
                case "PDC" -> pdcReturns++;
                case "EQUIPMENT" -> equipmentReturns++;
                case "INVENTORY" -> inventoryReturns++;
                default -> throw new IllegalStateException("Unknown writer type");
            }
            LOCK.notifyAll();
        }
    }

    public static void closeForProbe() {
        synchronized (LOCK) {
            if (closed)
                throw new IllegalStateException("Owner writer admission is already closed");
            closed = true;
            closedGeneration = Math.addExact(closedGeneration, 1);
            System.out.println("PAPER_A01_LIVE_AGENT_PROBE_CLOSED");
        }
    }

    /** Atomic test-only admission gate: false while a real body remains active. */
    public static boolean awaitDrain(long millis) throws InterruptedException {
        if (millis < 1 || millis > 15000)
            throw new IllegalArgumentException("Invalid bounded owner-writer drain deadline");
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        synchronized (LOCK) {
            if (!closed)
                throw new IllegalStateException("Cannot drain while new owner writers can enter");
            while (active != 0) {
                long remain = deadline - System.nanoTime();
                if (remain <= 0) return false;
                LOCK.wait(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remain)));
            }
            return true;
        }
    }

    public static boolean isClosed() {
        synchronized (LOCK) { return closed; }
    }

    public static void armHold(String writer) {
        if (!"WWC".equals(writer) && !"WWW".equals(writer))
            throw new IllegalArgumentException("Unknown real owner writer " + writer);
        synchronized (LOCK) {
            if (closed || active != 0 || heldWriter != null)
                throw new IllegalStateException("Cannot arm owner writer hold now");
            heldWriter = writer;
            heldEntered = new CountDownLatch(1);
            heldRelease = new CountDownLatch(1);
            holdConsumed = false;
        }
    }

    public static boolean awaitHeldEntry(String writer, long millis) throws InterruptedException {
        CountDownLatch entered;
        synchronized (LOCK) {
            if (!writer.equals(heldWriter)) throw new IllegalStateException("Hold target changed");
            entered = heldEntered;
        }
        if (millis < 1 || millis > 15000)
            throw new IllegalArgumentException("Invalid real owner-writer entry deadline");
        return entered.await(millis, TimeUnit.MILLISECONDS);
    }

    public static void releaseHeldEntry(String writer) {
        CountDownLatch release;
        synchronized (LOCK) {
            if (!writer.equals(heldWriter)) throw new IllegalStateException("Hold target changed");
            release = heldRelease;
        }
        release.countDown();
    }

    public static void clearHold(String writer) {
        synchronized (LOCK) {
            if (!writer.equals(heldWriter) || !holdConsumed || active != 0)
                throw new IllegalStateException("Real owner writer hold did not drain");
            heldWriter = null;
            heldEntered = null;
            heldRelease = null;
            holdConsumed = false;
        }
    }

    public static void reopenForProbe() {
        synchronized (LOCK) {
            if (!closed || active != 0)
                throw new IllegalStateException("Cannot reopen active provider gate");
            closed = false;
            System.out.println("PAPER_A01_LIVE_AGENT_PROBE_REOPENED");
        }
    }

    public static int count(String kind) {
        synchronized (LOCK) {
            return switch (kind) {
                case "active" -> active;
                case "wwcReturns" -> wwcReturns;
                case "wwwReturns" -> wwwReturns;
                case "pdcReturns" -> pdcReturns;
                case "equipmentReturns" -> equipmentReturns;
                case "inventoryReturns" -> inventoryReturns;
                case "wwcDenied" -> wwcDenied;
                case "wwwDenied" -> wwwDenied;
                case "pdcDenied" -> pdcDenied;
                case "equipmentDenied" -> equipmentDenied;
                case "inventoryDenied" -> inventoryDenied;
                default -> throw new IllegalArgumentException("Unknown hook evidence " + kind);
            };
        }
    }
}
