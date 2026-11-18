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
        // Current Queue case.s0 is intentionally Red before the implementation.
        return Verdict.HOLD;
    }
}
