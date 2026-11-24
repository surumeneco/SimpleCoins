package co.surumene.simplecoins.domain;

import java.util.Objects;

/** A01 publication eligibility; inputs describe observed settlement outcomes, not a mandatory fence implementation. */
public final class SettlementWriterEpochDecision {
    private SettlementWriterEpochDecision() {}

    public record Evidence(boolean sourcesComplete,
                           boolean attributionAndDuplicatesReconciled,
                           boolean conflictsResolved,
                           boolean normalJournalHandoffReady,
                           boolean admissionClosedAndPlayersAbsent,
                           boolean seriousInconsistency) {
        /** Adapter for existing protected-epoch callers; only their chosen mechanism requires a drained seal. */
        public Evidence(boolean sourcesComplete, boolean newWriterAdmissionsDenied,
                        boolean admittedWritersDrained, boolean candidatesSameEpoch,
                        boolean attributionAndJournalsReconciled, boolean normalJournalHandoffReady,
                        boolean playersAbsent, boolean epochInvalidated) {
            this(sourcesComplete, attributionAndJournalsReconciled,
                 newWriterAdmissionsDenied && admittedWritersDrained && candidatesSameEpoch,
                 normalJournalHandoffReady, playersAbsent, epochInvalidated);
        }
    }

    public enum Verdict {
        TRY_ATOMIC_PUBLISH, HOLD_UNPUBLISHED, REVOKE_PUBLISHED, NO_ACTION
    }

    public static Verdict evaluate(Evidence evidence) {
        Objects.requireNonNull(evidence, "G0 observed evidence");
        if (evidence.seriousInconsistency()) return Verdict.REVOKE_PUBLISHED;
        return evidence.sourcesComplete()
                && evidence.attributionAndDuplicatesReconciled()
                && evidence.conflictsResolved()
                && evidence.normalJournalHandoffReady()
                && evidence.admissionClosedAndPlayersAbsent()
                ? Verdict.TRY_ATOMIC_PUBLISH : Verdict.HOLD_UNPUBLISHED;
    }

    public static Verdict revoke(Evidence evidence) {
        Objects.requireNonNull(evidence, "G0 observed evidence");
        return evidence.seriousInconsistency() ? Verdict.REVOKE_PUBLISHED : Verdict.NO_ACTION;
    }
}
