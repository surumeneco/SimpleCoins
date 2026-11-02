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
    @Test void sourceIdNormalizationDoesNotCreateIndependentDuplicateAuthorities() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, BanEvidence.evaluate(
                Set.of(" PAPER ", "EXTERNAL", "moderation"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source(" external ", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source("MODERATION", BanEvidence.Status.CLEAR))).status());
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("paper", " PAPER "), List.of()));
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("paper"), List.of(
                        new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source(" PAPER ", BanEvidence.Status.CLEAR))));
    }

    @Test void undeclaredOrMalformedEvidenceDoesNotProduceTypedDecision() {
        var required = Set.of("paper", "external");
        assertThrows(IOException.class, () -> BanEvidence.evaluate(required,
                List.of(new BanEvidence.Source("unregistered", BanEvidence.Status.CLEAR))));
        var malformed = new java.util.ArrayList<BanEvidence.Source>();
        malformed.add(null);
        assertThrows(IOException.class, () -> BanEvidence.evaluate(required, malformed));
        assertThrows(IOException.class, () -> BanEvidence.evaluate(required,
                List.of(new BanEvidence.Source("paper", null))));
        assertThrows(IOException.class, () -> BanEvidence.evaluate(null, List.of()));
        assertThrows(IOException.class, () -> BanEvidence.evaluate(required, null));
    }

    @Test void providerOrderDoesNotChangeDecision() throws Exception {
        var required = Set.of("paper", "external", "moderation");
        var rows = List.of(
            new BanEvidence.Source("paper", BanEvidence.Status.UNKNOWN),
            new BanEvidence.Source("external", BanEvidence.Status.BANNED),
            new BanEvidence.Source("moderation", BanEvidence.Status.CLEAR));
        for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
            if (a == b) continue;
            int c = 3 - a - b;
            assertEquals(BanEvidence.Status.BANNED, BanEvidence.evaluate(
                required, List.of(rows.get(a), rows.get(b), rows.get(c))).status());
        }
    }

}
