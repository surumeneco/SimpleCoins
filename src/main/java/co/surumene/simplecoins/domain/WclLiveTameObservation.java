package co.surumene.simplecoins.domain;

import java.util.UUID;

/**
 * Qualifies an independently observed, already-loaded vanilla tame owner.
 * These observations are local candidates, not a G0 writer-epoch proof or
 * authorization to publish any monetary balance.
 */
public final class WclLiveTameObservation {
    private WclLiveTameObservation() {}

    /**
     * A verified normal Chicken has no vanilla tame-owner capability.
     * The WCL WWC follow_target is resolved separately by WclOwnerEvidence.
     */
    public static WclOwnerEvidence.TameOwner chicken(boolean authoritative) {
        return authoritative ? WclOwnerEvidence.TameOwner.verifiedAbsent()
                : WclOwnerEvidence.TameOwner.unknown();
    }

    /**
     * Only an authoritative Wolf with a coherent tame-state/owner pair
     * can positively prove a vanilla owner or its absence.
     * Contradictions cannot become FREE, even if WCL has no owner key.
     */
    public static WclOwnerEvidence.TameOwner wolf(
            boolean authoritative, boolean tamed, UUID owner) {
        if (!authoritative || tamed != (owner != null))
            return WclOwnerEvidence.TameOwner.unknown();
        return owner == null ? WclOwnerEvidence.TameOwner.verifiedAbsent()
                : WclOwnerEvidence.TameOwner.verified(owner);
    }
}
