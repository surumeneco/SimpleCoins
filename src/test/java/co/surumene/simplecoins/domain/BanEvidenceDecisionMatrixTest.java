package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Typed BAN evidence matrix; independent verification, CAT Draft. */
class BanEvidenceDecisionMatrixTest {
    @Test void everyThreeSourceCombinationHasCorrectPrecedence() throws Exception {
        var values = BanEvidence.Status.values();
        var required = Set.of("paper", "external", "moderation");
        int count = 0;
        for (var a : values) for (var b : values) for (var c : values) {
            var observations = List.of(
                new BanEvidence.Source("paper", a),
                new BanEvidence.Source("external", b),
                new BanEvidence.Source("moderation", c));
            var expected = a == BanEvidence.Status.BANNED ||
                    b == BanEvidence.Status.BANNED || c == BanEvidence.Status.BANNED
                    ? BanEvidence.Status.BANNED
                    : a == BanEvidence.Status.UNKNOWN || b == BanEvidence.Status.UNKNOWN ||
                      c == BanEvidence.Status.UNKNOWN
                    ? BanEvidence.Status.UNKNOWN : BanEvidence.Status.CLEAR;
            assertEquals(expected, BanEvidence.evaluate(required, observations).status());
            count++;
        }
        assertEquals(27, count);
    }

    @Test void omittedSourceCannotProveClear() throws Exception {
        var required = Set.of("paper", "external", "moderation");
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.evaluate(required, List.of(
                new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("external", BanEvidence.Status.CLEAR))).status());
        assertEquals(BanEvidence.Status.BANNED, BanEvidence.evaluate(required, List.of(
                new BanEvidence.Source("paper", BanEvidence.Status.BANNED))).status());
    }
}
