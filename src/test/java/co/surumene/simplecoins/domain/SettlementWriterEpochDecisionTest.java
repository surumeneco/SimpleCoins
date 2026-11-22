package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** Independent bounded tests of the confirmed A01 Boolean partition, not writer proof. */
final class SettlementWriterEpochDecisionTest {

    @Test
    void onlyCompleteNonInvalidatedEvidenceCanAttemptPublication() {
        // 2^8 valuations: seven independent required proofs plus revocation.
        for (int bits = 0; bits < 256; bits++) {
            var evidence = new SettlementWriterEpochDecision.Evidence(
                    (bits & 1) != 0, (bits & 2) != 0,
                    (bits & 4) != 0, (bits & 8) != 0,
                    (bits & 16) != 0, (bits & 32) != 0,
                    (bits & 64) != 0, (bits & 128) != 0);
            var expected = (bits & 128) != 0
                    ? SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED
                    : (bits & 127) == 127
                        ? SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH
                        : SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED;
            assertEquals(expected, SettlementWriterEpochDecision.evaluate(evidence),
                    "A01 evaluation mask " + bits);
        }
    }

    @Test
    void revokeIsNotAnAlternatePublicationPath() {
        var allVerified = new SettlementWriterEpochDecision.Evidence(
                true, true, true, true, true, true, true, false);
        var invalidated = new SettlementWriterEpochDecision.Evidence(
                true, true, true, true, true, true, true, true);
        assertEquals(SettlementWriterEpochDecision.Verdict.NO_ACTION,
                SettlementWriterEpochDecision.revoke(allVerified));
        assertEquals(SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED,
                SettlementWriterEpochDecision.revoke(invalidated));
        assertThrows(NullPointerException.class,
                () -> SettlementWriterEpochDecision.evaluate(null));
    }
}
