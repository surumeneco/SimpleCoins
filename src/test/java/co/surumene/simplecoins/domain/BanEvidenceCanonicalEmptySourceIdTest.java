package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Draft BAN-CEMPTY vectors: normalization must not admit an empty authority ID. */
class BanEvidenceCanonicalEmptySourceIdTest {
    private static void assertMalformed(String rawId) {
        var observations = List.of(new BanEvidence.Source(rawId, BanEvidence.Status.CLEAR));
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of(rawId), observations),
                "A control-only source ID cannot prove a distinct BAN authority");
        assertThrows(IOException.class, () -> BanEvidence.evaluate(Set.of(rawId), observations),
                "A control-only source ID cannot produce a typed settlement decision");
    }

    @Test void nulOnlyIdIsNotAnAuthority() {
        assertMalformed(String.valueOf((char) 0));
    }

    @Test void sohOnlyIdIsNotAnAuthority() {
        assertMalformed(String.valueOf((char) 1));
    }

    @Test void mixedC0ControlsDoNotBecomeAnEmptyAuthority() {
        assertMalformed(new String(new char[] {0, 1, 0}));
    }

    @Test void ordinaryDeclaredAuthorityStillProducesClear() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, BanEvidence.combine(
                Set.of("paper-profile"),
                List.of(new BanEvidence.Source("paper-profile", BanEvidence.Status.CLEAR))));
    }
}
