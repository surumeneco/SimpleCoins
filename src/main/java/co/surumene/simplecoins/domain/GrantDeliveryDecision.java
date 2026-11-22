package co.surumene.simplecoins.domain;

import java.util.Objects;

/**
 * Confirmed A02 PI/TCE verdict on external proof of authentic grant delivery.
 * No playerdata IO, currency placement, ledger acknowledgement or writer
 * fencing occurs here. A permissive verdict MUST NOT be used to acknowledge
 * a grant until the real save generation is guarded through durable ACK.
 */
public final class GrantDeliveryDecision {
    private GrantDeliveryDecision() {}

    public record Evidence(boolean reservationExists,
                           boolean savedPhysicalReceiptPair,
                           boolean reservationFullyReceived,
                           boolean savedGenerationProvedThroughAck,
                           boolean otherStoragePreserved,
                           boolean acknowledgementPending,
                           boolean contradictoryEvidence) {}

    public enum Verdict {
        ALLOW_ACK, KEEP_PENDING, KEEP_PENDING_AND_BLOCK,
        BLOCK_WITHOUT_PENDING, NO_ACTION
    }

    /** Preparation is never authorization for issuance or an ACK. */
    public static Verdict prepare(Evidence evidence) {
        Objects.requireNonNull(evidence, "grant evidence");
        if (!evidence.reservationExists())
            return evidence.contradictoryEvidence()
                    ? Verdict.BLOCK_WITHOUT_PENDING : Verdict.NO_ACTION;
        if (!evidence.acknowledgementPending())
            return evidence.contradictoryEvidence()
                    ? Verdict.BLOCK_WITHOUT_PENDING : Verdict.NO_ACTION;
        return evidence.contradictoryEvidence()
                ? Verdict.KEEP_PENDING_AND_BLOCK : Verdict.KEEP_PENDING;
    }

    /** Verified file-and-receipt proof must remain valid until actual ACK. */
    public static Verdict verify(Evidence evidence) {
        Objects.requireNonNull(evidence, "grant evidence");
        if (!evidence.reservationExists())
            return evidence.contradictoryEvidence()
                    ? Verdict.BLOCK_WITHOUT_PENDING : Verdict.NO_ACTION;
        if (!evidence.acknowledgementPending())
            return evidence.contradictoryEvidence()
                    ? Verdict.BLOCK_WITHOUT_PENDING : Verdict.NO_ACTION;
        if (evidence.contradictoryEvidence()
                || !evidence.savedGenerationProvedThroughAck()
                || !evidence.otherStoragePreserved()
                || (!evidence.savedPhysicalReceiptPair() && evidence.reservationFullyReceived()))
            return Verdict.KEEP_PENDING_AND_BLOCK;
        if (evidence.savedPhysicalReceiptPair() && evidence.reservationFullyReceived())
            return Verdict.ALLOW_ACK;
        return Verdict.KEEP_PENDING;
    }

    /** Crash recovery never considers a pending reservation already ACKed. */
    public static Verdict recover(Evidence evidence) {
        return verify(evidence);
    }
}
