package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** Confirmed A02 boundary examples; no fabricated playerdata/ACK evidence. */
final class GrantDeliveryDecisionTest {

    private static GrantDeliveryDecision.Evidence evidence(
            boolean reservation, boolean pair, boolean full,
            boolean epoch, boolean other, boolean pending, boolean contradiction) {
        return new GrantDeliveryDecision.Evidence(
                reservation, pair, full, epoch, other, pending, contradiction);
    }

    @Test
    void preparationNeverAcknowledgesEvenWithCompleteEvidence() {
        var complete = evidence(true, true, true, true, true, true, false);
        assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING,
                GrantDeliveryDecision.prepare(complete));
        assertEquals(GrantDeliveryDecision.Verdict.ALLOW_ACK,
                GrantDeliveryDecision.verify(complete));
        assertEquals(GrantDeliveryDecision.Verdict.ALLOW_ACK,
                GrantDeliveryDecision.recover(complete));
        assertNotEquals(GrantDeliveryDecision.Verdict.ALLOW_ACK,
                GrantDeliveryDecision.prepare(complete));
    }

    @Test
    void partialAndUnsavedReceiptsNeverBecomeAcknowledged() {
        var partial = evidence(true, true, false, true, true, true, false);
        var unsaved = evidence(true, false, false, true, true, true, false);
        assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING,
                GrantDeliveryDecision.verify(partial));
        assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING,
                GrantDeliveryDecision.recover(unsaved));
        assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK,
                GrantDeliveryDecision.verify(evidence(true, false, true,
                        true, true, true, false)));
    }

    @Test
    void lostGenerationOrOtherStorageProofBlocksEvenCompleteReceipt() {
        var staleEpoch = evidence(true, true, true, false, true, true, false);
        var otherStorageLost = evidence(true, true, true, true, false, true, false);
        var contradictory = evidence(true, true, true, true, true, true, true);
        for (var e : new GrantDeliveryDecision.Evidence[]{
                staleEpoch, otherStorageLost, contradictory}) {
            assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK,
                    GrantDeliveryDecision.verify(e));
            assertEquals(GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK,
                    GrantDeliveryDecision.recover(e));
        }
    }

    @Test
    void unrecordedReceiptAndAcknowledgedHistoryNeverReissue() {
        assertEquals(GrantDeliveryDecision.Verdict.NO_ACTION,
                GrantDeliveryDecision.prepare(evidence(false, false, false,
                        false, false, false, false)));
        assertEquals(GrantDeliveryDecision.Verdict.BLOCK_WITHOUT_PENDING,
                GrantDeliveryDecision.verify(evidence(false, false, false,
                        false, false, false, true)));
        assertEquals(GrantDeliveryDecision.Verdict.NO_ACTION,
                GrantDeliveryDecision.recover(evidence(true, true, true,
                        true, true, false, false)));
        assertEquals(GrantDeliveryDecision.Verdict.BLOCK_WITHOUT_PENDING,
                GrantDeliveryDecision.recover(evidence(true, true, true,
                        true, true, false, true)));
        assertThrows(NullPointerException.class,
                () -> GrantDeliveryDecision.verify(null));
    }
}
