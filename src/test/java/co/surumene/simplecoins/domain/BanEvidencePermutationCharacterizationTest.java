package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Characterizes source-order invariance of the current BAN evidence aggregator.
 * This is an observational regression, not a normative CAT confirmation.
 */
class BanEvidencePermutationCharacterizationTest {
    private static final Set<String> REQUIRED = Set.of("paper-profile", "external", "plugin");

    @Test
    void completeClearEvidenceIsOrderIndependent() throws Exception {
        assertEveryPermutation(List.of(
                source("paper-profile", BanEvidence.Status.CLEAR),
                source("external", BanEvidence.Status.CLEAR),
                source("plugin", BanEvidence.Status.CLEAR)),
                BanEvidence.Status.CLEAR);
    }

    @Test
    void aSingleBanDominatesUnknownAndClearRegardlessOfSourceOrder() throws Exception {
        assertEveryPermutation(List.of(
                source("paper-profile", BanEvidence.Status.UNKNOWN),
                source("external", BanEvidence.Status.BANNED),
                source("plugin", BanEvidence.Status.CLEAR)),
                BanEvidence.Status.BANNED);
    }

    @Test
    void unknownCannotBecomeClearByReorderingEvidence() throws Exception {
        assertEveryPermutation(List.of(
                source("paper-profile", BanEvidence.Status.CLEAR),
                source("external", BanEvidence.Status.UNKNOWN),
                source("plugin", BanEvidence.Status.CLEAR)),
                BanEvidence.Status.UNKNOWN);
    }

    @Test
    void unexpectedEvidenceFailsClosedRegardlessOfItsPosition() {
        List<BanEvidence.Source> observations = List.of(
                source("paper-profile", BanEvidence.Status.BANNED),
                source("external", BanEvidence.Status.CLEAR),
                source("untrusted", BanEvidence.Status.CLEAR));
        for (List<BanEvidence.Source> ordering : permutations(observations)) {
            assertThrows(IOException.class, () -> BanEvidence.combine(REQUIRED, ordering));
        }
    }

    private static BanEvidence.Source source(String id, BanEvidence.Status status) {
        return new BanEvidence.Source(id, status);
    }

    private static void assertEveryPermutation(List<BanEvidence.Source> observations,
                                               BanEvidence.Status expected) throws Exception {
        for (List<BanEvidence.Source> ordering : permutations(observations)) {
            assertEquals(expected, BanEvidence.combine(REQUIRED, ordering),
                    () -> "Unexpected result for " + ordering);
        }
    }

    private static List<List<BanEvidence.Source>> permutations(List<BanEvidence.Source> items) {
        List<List<BanEvidence.Source>> result = new ArrayList<>();
        permute(new ArrayList<>(), new ArrayList<>(items), result);
        return result;
    }

    private static void permute(List<BanEvidence.Source> prefix,
                                List<BanEvidence.Source> remaining,
                                List<List<BanEvidence.Source>> result) {
        if (remaining.isEmpty()) {
            result.add(List.copyOf(prefix));
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            BanEvidence.Source item = remaining.remove(i);
            prefix.add(item);
            permute(prefix, remaining, result);
            prefix.remove(prefix.size() - 1);
            remaining.add(i, item);
        }
    }
}
