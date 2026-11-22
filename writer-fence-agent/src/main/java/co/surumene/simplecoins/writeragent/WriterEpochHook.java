package co.surumene.simplecoins.writeragent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Bootstrap-loader rendezvous for the standalone Java 25 Agent and the Paper plugin.
 * No direct Paper, plugin-loader, or product class references are allowed here.
 *
 * Initially PASSIVE to preserve normal save behavior. Transformed methods are
 * counted even while passive so activation cannot overlook an already-entered
 * observed writer. This bridge proves only CraftPlayer + NMS PlayerDataStorage,
 * never the complete G0/G3 roster or a persistent settlement/receipt epoch.
 */
public final class WriterEpochHook {
    public enum Mode { PASSIVE, LIMITED_TRACKING, FAULTED }

    private record Frame(String target, Object entry, boolean tracked) {}
    private static final Object LOCK = new Object();
    private static final ThreadLocal<ArrayDeque<Frame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final Set<String> TARGETS = Set.of("CRAFT", "NMS");
    private static final Set<String> TRANSFORMED = new HashSet<>();
    private static Mode mode = Mode.PASSIVE;
    private static int passiveActive;
    private static int trackedActive;
    private static MethodHandle enter, normal, exceptional, invalidateProduct;

    private WriterEpochHook() {}

    /** Transformer-only observation; a boolean passed by a plugin cannot replace this. */
    public static void recordInstrumentedTarget(String name) {
        synchronized (LOCK) {
            if (mode != Mode.PASSIVE || !TARGETS.contains(name) || !TRANSFORMED.add(name)) {
                faultLocked();
                throw new IllegalStateException("Unexpected, duplicate or late transformed writer: " + name);
            }
        }
    }

    public static Set<String> instrumentedTargets() {
        synchronized (LOCK) { return Set.copyOf(TRANSFORMED); }
    }

    public static Mode mode() { synchronized (LOCK) { return mode; } }
    public static int inFlight() {
        synchronized (LOCK) { return passiveActive + trackedActive; }
    }

    /** Non-arming bind. Does not imply the 24-JAR writer roster was closed. */
    public static void bindProductCallbacks(MethodHandle onEnter,
            MethodHandle onNormalReturn, MethodHandle onExceptionalReturn, MethodHandle onInvalidateProduct) {
        synchronized (LOCK) {
            Objects.requireNonNull(onEnter, "enter callback");
            Objects.requireNonNull(onNormalReturn, "normal return callback");
            Objects.requireNonNull(onExceptionalReturn, "exception callback");
            Objects.requireNonNull(onInvalidateProduct, "invalidation callback");
            if (mode != Mode.PASSIVE || enter != null
                    || !onEnter.type().equals(MethodType.methodType(Object.class, String.class, Object.class))
                    || !onNormalReturn.type().equals(MethodType.methodType(void.class, Object.class))
                    || !onExceptionalReturn.type().equals(MethodType.methodType(void.class, Object.class))
                    || !onInvalidateProduct.type().equals(MethodType.methodType(void.class))) {
                faultLocked();
                throw new IllegalStateException("Untrusted or duplicate product writer binding");
            }
            enter = onEnter;
            normal = onNormalReturn;
            exceptional = onExceptionalReturn;
            invalidateProduct = onInvalidateProduct;
        }
    }

    /**
     * Limited-family diagnostic only: caller must have separately armed the
     * product fence and audited every other effective writer before any future
     * product use. SimpleCoinsPlugin NEVER calls this API. Do not derive a G0
     * authorization or a G3 ACK from this state.
     */
    public static void beginLimitedFamilyProof() {
        synchronized (LOCK) {
            if (mode != Mode.PASSIVE || enter == null || normal == null || exceptional == null
                    || !TRANSFORMED.equals(TARGETS) || passiveActive != 0 || trackedActive != 0) {
                throw new IllegalStateException("Cannot start unproven or in-flight native writer scope");
            }
            mode = Mode.LIMITED_TRACKING;
        }
    }

    public static void fault() { synchronized (LOCK) { faultLocked(); } }
    private static void faultLocked() {
        boolean firstFault = mode != Mode.FAULTED;
        mode = Mode.FAULTED;
        LOCK.notifyAll();
        // A standalone Agent failure must invalidate the *same* product epoch.
        // Otherwise a previously minted Seal could still appear drained and valid.
        if (firstFault && invalidateProduct != null) {
            try { invalidateProduct.invokeExact(); }
            catch (Throwable failure) {
                throw new IllegalStateException(
                        "Cannot revoke product writer epoch after Agent fault", failure);
            }
        }
    }

    public static void beforeCraft() { before("CRAFT"); }
    public static void afterCraft() { after("CRAFT"); }
    public static void beforeNms() { before("NMS"); }
    public static void afterNms() { after("NMS"); }

    private static void before(String target) {
        synchronized (LOCK) {
            if (mode == Mode.FAULTED)
                throw new IllegalStateException("Writer Agent bridge invalidated");
            ArrayDeque<Frame> stack = STACK.get();
            if (mode == Mode.PASSIVE) {
                passiveActive = Math.addExact(passiveActive, 1);
                stack.addLast(new Frame(target, null, false));
                return;
            }
            if (!stack.isEmpty()
                    && !("NMS".equals(target) && "CRAFT".equals(stack.peekLast().target()))) {
                faultLocked();
                throw new IllegalStateException("Unrecognized nested native writer ancestry");
            }
            Object parent = stack.isEmpty() ? null : stack.peekLast().entry();
            try {
                Object token = (Object) enter.invokeExact(target, parent);
                if (token == null) throw new IllegalStateException("Null product writer token");
                trackedActive = Math.addExact(trackedActive, 1);
                stack.addLast(new Frame(target, token, true));
            } catch (Throwable failure) {
                faultLocked();
                throw new IllegalStateException("Product writer entry refused or failed", failure);
            }
        }
    }

    private static void after(String target) {
        synchronized (LOCK) {
            ArrayDeque<Frame> stack = STACK.get();
            Frame top = stack.peekLast();
            if (top == null || !target.equals(top.target())) {
                faultLocked();
                throw new IllegalStateException("Writer normal return has no matching entry");
            }
            stack.removeLast();
            if (stack.isEmpty()) STACK.remove();
            if (!top.tracked()) {
                if (passiveActive <= 0) {
                    faultLocked();
                    throw new IllegalStateException("Passive native writer count underflow");
                }
                passiveActive--;
            } else {
                if (trackedActive <= 0) {
                    faultLocked();
                    throw new IllegalStateException("Tracked native writer count underflow");
                }
                try {
                    normal.invokeExact(top.entry());
                } catch (Throwable failure) {
                    faultLocked();
                    throw new IllegalStateException("Product writer normal return failed", failure);
                }
                trackedActive--;
            }
            LOCK.notifyAll();
        }
    }
}
