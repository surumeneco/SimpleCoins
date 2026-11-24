package co.surumene.simplecoins.domain;

import java.util.Objects;

/** A02 decision for one target's reservation. Save/receipt observations must come from the delivery service. */
public final class GrantDeliveryDecision {
    private GrantDeliveryDecision() {}

    public record Evidence(boolean pendingReservation, boolean fullSaveConfirmed, boolean contradictoryEvidence) {
        /** Conservative adapter for callers of the previous protected-save protocol. */
        public Evidence(boolean reservationExists, boolean savedPhysicalReceiptPair,
                        boolean reservationFullyReceived, boolean savedGenerationProvedThroughAck,
                        boolean otherStoragePreserved, boolean acknowledgementPending,
                        boolean contradictoryEvidence) {
            this(reservationExists && acknowledgementPending,
                 savedPhysicalReceiptPair && reservationFullyReceived && savedGenerationProvedThroughAck && otherStoragePreserved,
                 contradictoryEvidence || (reservationExists && acknowledgementPending &&
                         (!savedGenerationProvedThroughAck || !otherStoragePreserved ||
                         (!savedPhysicalReceiptPair && reservationFullyReceived))));
        }
    }

    public enum Verdict {
        ALLOW_ACK, KEEP_PENDING, KEEP_PENDING_AND_BLOCK,
        BLOCK_WITHOUT_PENDING, NO_ACTION
    }

    public static Verdict prepare(Evidence evidence) {
        Objects.requireNonNull(evidence, "grant evidence");
        if (evidence.contradictoryEvidence())
            return evidence.pendingReservation() ? Verdict.KEEP_PENDING_AND_BLOCK : Verdict.BLOCK_WITHOUT_PENDING;
        return evidence.pendingReservation() ? Verdict.KEEP_PENDING : Verdict.NO_ACTION;
    }

    public static Verdict verify(Evidence evidence) {
        var preparation = prepare(evidence);
        return preparation == Verdict.KEEP_PENDING && evidence.fullSaveConfirmed()
                ? Verdict.ALLOW_ACK : preparation;
    }

    public static Verdict recover(Evidence evidence) {
        return verify(evidence);
    }
}
