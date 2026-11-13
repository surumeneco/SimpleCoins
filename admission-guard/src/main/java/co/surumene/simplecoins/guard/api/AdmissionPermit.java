package co.surumene.simplecoins.guard.api;

/**
 * Read-only signal exposed by the authoritative SimpleCoins plugin.
 * A missing provider, failure or uncertain state must be treated as denied.
 */
@FunctionalInterface
public interface AdmissionPermit {
    boolean mayAdmit();
}
