package co.surumene.simplecoins.domain;

import java.util.Objects;

/**
 * Pure local verdict for a caller-supplied, independently verified WCL source.
 * No reader call, global settlement publication, or admission authorization.
 */
public final class WclStorageEvidenceDecision {
    private WclStorageEvidenceDecision() {
    }

    public record Evidence(
            boolean providersVerified,
            boolean targetEntityVerified,
            boolean nonTargetEntityVerified,
            boolean successfulRead,
            boolean sourceIdentityVerified,
            boolean ownershipVerified,
            boolean sameGenerationAndDeduplicated,
            boolean contradictionPresent) {
    }

    public enum Verdict {
        ADOPT_UNPUBLISHED,
        CONTINUE_VERIFIED_NON_TARGET,
        HOLD
    }

    public static Verdict decide(Evidence evidence) {
        Objects.requireNonNull(evidence, "evidence");
        // The booleans must come from the upstream provenance/owner/generation
        // verifier. This pure verdict must never invent positive evidence.
        if (!evidence.providersVerified() || evidence.contradictionPresent()) {
            return Verdict.HOLD;
        }
        if (!evidence.targetEntityVerified() && evidence.nonTargetEntityVerified()) {
            return Verdict.CONTINUE_VERIFIED_NON_TARGET;
        }
        if (evidence.targetEntityVerified()
                && !evidence.nonTargetEntityVerified()
                && evidence.successfulRead()
                && evidence.sourceIdentityVerified()
                && evidence.ownershipVerified()
                && evidence.sameGenerationAndDeduplicated()) {
            return Verdict.ADOPT_UNPUBLISHED;
        }
        // Unknown target, impossible simultaneous target/non-target, read
        // failure, source mismatch, ownership gap or stale generation.
        return Verdict.HOLD;
    }
}
