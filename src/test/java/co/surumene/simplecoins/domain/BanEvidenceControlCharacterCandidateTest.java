package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Candidate security characterization, NOT a Confirmed source-ID contract.
 * Source IDs are used as trust-boundary identifiers; control characters must
 * not silently become distinct, apparently valid source declarations.
 */
class BanEvidenceControlCharacterCandidateTest {
    @Test
    void embeddedNulInRequiredSourceIdIsRejected() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("server\u0000ban"),
                List.of(new BanEvidence.Source("server\u0000ban", BanEvidence.Status.CLEAR))));
    }

    @Test
    void embeddedTabInRequiredSourceIdIsRejected() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("server\\tban"),
                List.of(new BanEvidence.Source("server\\tban", BanEvidence.Status.CLEAR))));
    }

    @Test
    void embeddedNewlineInObservedSourceIdIsRejected() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(
                Set.of("server\nban"),
                List.of(new BanEvidence.Source("server\nban", BanEvidence.Status.CLEAR))));
    }
}
