package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Candidate contract: visually padded Unicode source identifiers must not
 * become authoritative BAN evidence without an explicit normalization policy.
 * This is a characterization proposal, not a confirmed CAT requirement.
 */
class BanEvidenceUnicodeWhitespaceCandidateTest {
    @Test
    void unicodeWhitespaceOnlyRequiredIdMustFailClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("\u2003"), List.of(new BanEvidence.Source("\u2003", BanEvidence.Status.CLEAR))));
    }

    @Test
    void unicodePaddedRequiredIdMustNotBecomeClearWithoutContract() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("\u2003ban\u2003"),
                List.of(new BanEvidence.Source("\u2003ban\u2003", BanEvidence.Status.CLEAR))));
    }
}
