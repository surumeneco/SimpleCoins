package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Code-to-spec characterization of the existing source-ID canonicalization. */
class BanEvidenceLocaleNormalizationTest {
    @Test
    void turkishDefaultLocaleDoesNotChangeAsciiSourceIdentity() throws Exception {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(BanEvidence.Status.CLEAR,
                    BanEvidence.combine(Set.of("IP"),
                            List.of(new BanEvidence.Source("ip", BanEvidence.Status.CLEAR))));
            assertEquals(BanEvidence.Status.UNKNOWN,
                    BanEvidence.combine(Set.of("IP", "external"),
                            List.of(new BanEvidence.Source(" ip ", BanEvidence.Status.CLEAR))));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void duplicateCanonicalIdsAreRejected() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("IP", " ip "), List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("ip"), List.of(
                        new BanEvidence.Source("IP", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source(" ip ", BanEvidence.Status.UNKNOWN))));
    }
}
