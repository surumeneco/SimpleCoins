package co.surumene.simplecoins.domain;

import java.util.Objects;

/**
 * Local A04 decision from independently verified saved BlockLocker evidence.
 * Never reads a sign or asserts that a saved source, owner or journal is valid.
 * This is a candidate decision only, not a settlement or admission grant.
 */
public final class BlockLockerSavedOwnerVerdict {
    private BlockLockerSavedOwnerVerdict() {
    }

    public record Evidence(
            boolean savedTerrainSourceVerified,
            boolean allApplicableSignsVerified,
            boolean physicalStorageDeduplicated,
            boolean sameSavedSourceGenerationVerified,
            boolean protectionVerified,
            boolean nonProtectionVerified,
            boolean primaryOwnerUuidVerified,
            boolean everyoneEffectiveVerified,
            boolean everyoneAbsentVerified,
            boolean contradictionPresent) {
    }

    public enum Verdict {
        OWNER_UNPUBLISHED,
        FREE_UNPUBLISHED,
        HOLD
    }

    public static Verdict decide(Evidence evidence) {
        Objects.requireNonNull(evidence, "evidence");
        if (!evidence.savedTerrainSourceVerified()
                || !evidence.allApplicableSignsVerified()
                || !evidence.physicalStorageDeduplicated()
                || !evidence.sameSavedSourceGenerationVerified()
                || evidence.contradictionPresent()) {
            return Verdict.HOLD;
        }
        if (!evidence.protectionVerified()) {
            return evidence.nonProtectionVerified()
                    && !evidence.primaryOwnerUuidVerified()
                    && !evidence.everyoneEffectiveVerified()
                    && !evidence.everyoneAbsentVerified()
                    ? Verdict.FREE_UNPUBLISHED : Verdict.HOLD;
        }
        if (evidence.nonProtectionVerified() || evidence.everyoneAbsentVerified()
                == evidence.everyoneEffectiveVerified()) {
            return Verdict.HOLD;
        }
        if (evidence.everyoneEffectiveVerified()) {
            // A verified effective [Everyone] outranks a known private owner.
            return Verdict.FREE_UNPUBLISHED;
        }
        return evidence.primaryOwnerUuidVerified()
                ? Verdict.OWNER_UNPUBLISHED : Verdict.FREE_UNPUBLISHED;
    }
}
