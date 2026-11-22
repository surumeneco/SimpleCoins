package co.surumene.simplecoins.a01agent;

import java.util.ArrayDeque;

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
    private static int wwcDenied;
    private static int wwwDenied;
    private static boolean closed;

    private LiveOwnerWriterHook() {}

    public static void beforeWWC() { enter("WWC"); }
    public static void afterWWC() { leave("WWC"); }
    public static void beforeWWW() { enter("WWW"); }
    public static void afterWWW() { leave("WWW"); }

    private static void enter(String writer) {
        synchronized (LOCK) {
            // Only an already-admitted, same-thread invocation can inherit a
            // closed gate. This is NOT a proof of arbitrary async continuations.
            ArrayDeque<String> callStack = STACK.get();
            if (closed && callStack.isEmpty()) {
                if ("WWC".equals(writer)) wwcDenied++;
                else wwwDenied++;
                System.out.println("PAPER_A01_LIVE_AGENT_" + writer + "_ENTRY_DENIED");
                throw new IllegalStateException("A01 live provider writer fenced");
            }
            callStack.addLast(writer);
            active++;
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
            if ("WWC".equals(writer)) wwcReturns++;
            else wwwReturns++;
            LOCK.notifyAll();
        }
    }

    public static void closeForProbe() {
        synchronized (LOCK) {
            if (closed || active != 0)
                throw new IllegalStateException("Cannot close during active provider write");
            closed = true;
            System.out.println("PAPER_A01_LIVE_AGENT_PROBE_CLOSED");
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
                case "wwcDenied" -> wwcDenied;
                case "wwwDenied" -> wwwDenied;
                default -> throw new IllegalArgumentException("Unknown hook evidence " + kind);
            };
        }
    }
}
