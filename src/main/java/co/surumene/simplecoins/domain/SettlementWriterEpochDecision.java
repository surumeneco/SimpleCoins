package co.surumene.simplecoins.domain;

import java.util.Objects;

/**
 * Confirmed A01 PI/TCE decision over independently proved G0 evidence.
 * This pure verdict neither observes writers nor acquires a protected world
 * epoch, publishes a snapshot, or grants admission. An input flag is TRUE
 * only when an independent producer has proved the exact named condition.
 */
public final class SettlementWriterEpochDecision {
    private SettlementWriterEpochDecision() {}

    public record Evidence(boolean sourcesComplete,
                           boolean newWriterAdmissionsDenied,
                           boolean admittedWritersDrained,
                           boolean candidatesSameEpoch,
                           boolean attributionAndJournalsReconciled,
                           boolean normalJournalHandoffReady,
                           boolean playersAbsent,
                           boolean epochInvalidated) {}

    public enum Verdict {
        TRY_ATOMIC_PUBLISH, HOLD_UNPUBLISHED, REVOKE_PUBLISHED, NO_ACTION
    }

    /** Corresponds only to the A01 公開判定 (evaluate) PI trigger. */
    public static Verdict evaluate(Evidence evidence) {
        Objects.requireNonNull(evidence, "G0 observed evidence");
        if (evidence.epochInvalidated()) return Verdict.REVOKE_PUBLISHED;
        return evidence.sourcesComplete()
                && evidence.newWriterAdmissionsDenied()
                && evidence.admittedWritersDrained()
                && evidence.candidatesSameEpoch()
                && evidence.attributionAndJournalsReconciled()
                && evidence.normalJournalHandoffReady()
                && evidence.playersAbsent()
                ? Verdict.TRY_ATOMIC_PUBLISH : Verdict.HOLD_UNPUBLISHED;
    }

    /** A revoke notification with no invalidation never mints a new permit. */
    public static Verdict revoke(Evidence evidence) {
        Objects.requireNonNull(evidence, "G0 observed evidence");
        return evidence.epochInvalidated()
                ? Verdict.REVOKE_PUBLISHED : Verdict.NO_ACTION;
    }
}
