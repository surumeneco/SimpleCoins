package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Existing-code characterization: canonical provider identity must not depend on host locale. */
class BanEvidenceProviderIdentityTest {
    @Test
    void mixedCaseAndWhitespaceAreNormalizedWithoutChangingDecision() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, BanEvidence.combine(
                Set.of(" Paper ", "EXTERNAL"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source(" external ", BanEvidence.Status.CLEAR))));
    }

    @Test
    void providerIdentityIsLocaleIndependentForTurkishDefaultLocale() throws Exception {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(BanEvidence.Status.BANNED, BanEvidence.combine(
                    Set.of("IP"), List.of(new BanEvidence.Source("ip", BanEvidence.Status.BANNED))));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void duplicateDeclaredProviderAfterCanonicalizationFailsClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("PAPER", " paper "), List.of()));
    }

    @Test
    void duplicateObservedProviderAfterCanonicalizationFailsClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(
                        new BanEvidence.Source("PAPER", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source(" paper ", BanEvidence.Status.BANNED))));
    }

    @Test
    void undeclaredProviderFailsClosedEvenWhenAllRequiredSourcesAreClear() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(
                        new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                        new BanEvidence.Source("external", BanEvidence.Status.CLEAR))));
    }
}
