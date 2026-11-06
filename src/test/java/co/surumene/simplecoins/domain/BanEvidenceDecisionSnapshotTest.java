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
 * Characterization of the existing fail-closed BAN decision snapshot.
 * This does not establish an external BAN adapter or a CAT normative Gate.
 */
class BanEvidenceDecisionSnapshotTest {
    @Test
    void decisionDoesNotChangeWhenCallerMutatesEvidenceAfterEvaluation() throws Exception {
        Set<String> required = new HashSet<>(Set.of("ban", "ipban"));
        List<BanEvidence.Source> observations = new ArrayList<>(List.of(
                new BanEvidence.Source("ban", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("ipban", BanEvidence.Status.CLEAR)));
        BanEvidence.Decision decision = BanEvidence.evaluate(required, observations);
        assertEquals(BanEvidence.Status.CLEAR, decision.status());

        required.add("external");
        observations.clear();
        observations.add(new BanEvidence.Source("ban", BanEvidence.Status.BANNED));
        assertEquals(BanEvidence.Status.CLEAR, decision.status(),
                "An issued decision must not silently re-evaluate mutable caller inputs");
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.evaluate(required, observations).status(),
                "A fresh decision must treat newly missing required sources as unknown");
    }

    @Test
    void missingRequiredEvidenceNeverBecomesClearEvenWhenOtherSourceIsClear() throws Exception {
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.evaluate(
                Set.of("ban", "ipban"), List.of(
                        new BanEvidence.Source("ban", BanEvidence.Status.CLEAR))).status());
        assertEquals(BanEvidence.Status.BANNED, BanEvidence.evaluate(
                Set.of("ban", "ipban"), List.of(
                        new BanEvidence.Source("ban", BanEvidence.Status.BANNED))).status());
    }

    @Test
    void malformedEvidenceIsRejectedEvenWhenBanWasAlreadyObserved() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(Set.of("ban"), List.of(
                new BanEvidence.Source("ban", BanEvidence.Status.BANNED),
                new BanEvidence.Source("other", BanEvidence.Status.CLEAR))));
    }
}
