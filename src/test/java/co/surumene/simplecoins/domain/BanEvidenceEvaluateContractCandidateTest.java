package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Characterization of the existing BAN Decision boundary.
 * This is not approval of a new external-plugin ownership contract.
 */
class BanEvidenceEvaluateContractCandidateTest {
    @Test
    void decisionDoesNotChangeWhenCallerMutatesEvidenceAfterEvaluation() throws IOException {
        Set<String> required = new HashSet<>(Set.of("local", "proxy"));
        List<BanEvidence.Source> observed = new ArrayList<>(
                List.of(new BanEvidence.Source("local", BanEvidence.Status.CLEAR)));
        BanEvidence.Decision decision = BanEvidence.evaluate(required, observed);
        assertEquals(BanEvidence.Status.UNKNOWN, decision.status());

        required.remove("proxy");
        observed.add(new BanEvidence.Source("proxy", BanEvidence.Status.BANNED));
        assertEquals(BanEvidence.Status.UNKNOWN, decision.status());
    }

    @Test
    void decisionPreservesBanPrecedenceWhenAnotherRequiredSourceIsAbsent() throws IOException {
        BanEvidence.Decision decision = BanEvidence.evaluate(
                Set.of("local", "proxy"),
                List.of(new BanEvidence.Source("local", BanEvidence.Status.BANNED)));
        assertEquals(BanEvidence.Status.BANNED, decision.status());
    }

    @Test
    void evaluateRejectsMalformedEvidenceRatherThanReturningDecision() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("local"),
                List.of(new BanEvidence.Source("local", null))));
    }
}
