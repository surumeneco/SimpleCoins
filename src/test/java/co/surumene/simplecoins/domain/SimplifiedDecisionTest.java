package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class SimplifiedDecisionTest {
    private static boolean bit(int bits, int b) { return (bits & (1 << b)) != 0; }

    @Test
    void settlementOutcomes() {
        for (int b = 0; b < 64; b++) {
            var e = new SettlementWriterEpochDecision.Evidence(bit(b,0), bit(b,1), bit(b,2), bit(b,3), bit(b,4), bit(b,5));
            var expected = bit(b,5) ? SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED :
                b == 31 ? SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH : SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED;
            assertEquals(expected, SettlementWriterEpochDecision.evaluate(e));
            assertEquals(bit(b,5) ? SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED :
                SettlementWriterEpochDecision.Verdict.NO_ACTION, SettlementWriterEpochDecision.revoke(e));
        }
    }

    @Test
    void grantOutcomes() {
        for (int b = 0; b < 8; b++) {
            boolean pending = bit(b,0), full = bit(b,1), contradiction = bit(b,2);
            var e = new GrantDeliveryDecision.Evidence(pending, full, contradiction);
            var prepare = contradiction ?
                (pending ? GrantDeliveryDecision.Verdict.KEEP_PENDING_AND_BLOCK : GrantDeliveryDecision.Verdict.BLOCK_WITHOUT_PENDING) :
                (pending ? GrantDeliveryDecision.Verdict.KEEP_PENDING : GrantDeliveryDecision.Verdict.NO_ACTION);
            var verify = pending && full && !contradiction ? GrantDeliveryDecision.Verdict.ALLOW_ACK : prepare;
            assertEquals(prepare, GrantDeliveryDecision.prepare(e));
            assertEquals(verify, GrantDeliveryDecision.verify(e));
            assertEquals(verify, GrantDeliveryDecision.recover(e));
        }
    }
}
