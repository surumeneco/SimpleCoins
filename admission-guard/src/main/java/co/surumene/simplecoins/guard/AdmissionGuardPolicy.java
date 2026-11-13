package co.surumene.simplecoins.guard;

/** Pure, deny-by-default extension of the Confirmed settlement admission contract. */
public final class AdmissionGuardPolicy {
    private AdmissionGuardPolicy() {}

    public static boolean allowed(boolean mainEnabled, boolean providerRegistered,
                                  boolean verifiedAdmission, boolean shuttingDown) {
        return mainEnabled && providerRegistered && verifiedAdmission && !shuttingDown;
    }
}
