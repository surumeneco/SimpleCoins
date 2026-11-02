package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004: exhaustive fail-closed BAN decision contract vectors.
 * This checks the already-implemented pure decision boundary; it does not
 * establish external plugin discovery, UUID/IP association, or live Paper proof.
 */
class BanEvidenceDecisionMatrixTest {
    private static final List<String> IDS = List.of("paper", "external", "ip");
    private static final Set<String> REQUIRED = Set.copyOf(IDS);
    private static final int[][] PERMUTATIONS = {
        {0, 1, 2}, {0, 2, 1}, {1, 0, 2},
        {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
    };

    @Test void everyCompleteStatusCombinationIsIndependentOfSourceOrder() throws Exception {
        for (BanEvidence.Status first : BanEvidence.Status.values()) {
            for (BanEvidence.Status second : BanEvidence.Status.values()) {
                for (BanEvidence.Status third : BanEvidence.Status.values()) {
                    List<BanEvidence.Status> statuses = List.of(first, second, third);
                    BanEvidence.Status expected = statuses.contains(BanEvidence.Status.BANNED)
                            ? BanEvidence.Status.BANNED
                            : statuses.contains(BanEvidence.Status.UNKNOWN)
                                    ? BanEvidence.Status.UNKNOWN : BanEvidence.Status.CLEAR;
                    for (int[] permutation : PERMUTATIONS) {
                        List<BanEvidence.Source> sources = new ArrayList<>();
                        for (int index : permutation) {
                            sources.add(new BanEvidence.Source(
                                    IDS.get(index), statuses.get(index)));
                        }
                        assertEquals(expected, BanEvidence.evaluate(REQUIRED, sources).status(),
                                () -> "BAN evidence order must not change " + statuses);
                    }
                }
            }
        }
    }

    @Test void missingRequiredIpEvidenceCannotClearButPositiveBanStillWins()
            throws Exception {
        for (BanEvidence.Status paper : BanEvidence.Status.values()) {
            for (BanEvidence.Status external : BanEvidence.Status.values()) {
                BanEvidence.Status expected =
                        paper == BanEvidence.Status.BANNED ||
                        external == BanEvidence.Status.BANNED
                                ? BanEvidence.Status.BANNED : BanEvidence.Status.UNKNOWN;
                assertEquals(expected, BanEvidence.evaluate(REQUIRED, List.of(
                        new BanEvidence.Source("paper", paper),
                        new BanEvidence.Source("external", external))).status());
            }
        }
    }
}
