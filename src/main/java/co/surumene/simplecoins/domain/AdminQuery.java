package co.surumene.simplecoins.domain;

/** Read-only visibility rules. A zero registered balance remains visible. */
public final class AdminQuery {
    public enum Kind { STATISTICS, PLAYER, DEAD }
    public enum Outcome { DENIED, UNAVAILABLE, UNREGISTERED, DISPLAY }
    private AdminQuery() {}
    public static Outcome evaluate(boolean permitted, boolean ready, Kind kind, boolean registered) {
        if (!permitted) return Outcome.DENIED;
        if (!ready) return Outcome.UNAVAILABLE;
        if (kind == Kind.PLAYER && !registered) return Outcome.UNREGISTERED;
        return Outcome.DISPLAY;
    }
}
