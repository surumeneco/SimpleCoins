package co.surumene.simplecoins.domain;

/** Admission of a validated administrative request, not proof of issuance. */
public final class AdminGiveRequest {
    private AdminGiveRequest() {}
    public enum Outcome { DENIED, UNAVAILABLE, INVALID, ACCEPT }
    public static Outcome evaluate(boolean permitted, boolean settled, boolean targetValid, boolean requestValid) {
        if (!permitted) return Outcome.DENIED;
        if (!settled) return Outcome.UNAVAILABLE;
        if (!targetValid || !requestValid) return Outcome.INVALID;
        return Outcome.ACCEPT;
    }
}
