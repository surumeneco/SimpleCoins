package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Characterization of fail-closed BAN evidence parsing when a positive BAN
 * observation precedes a malformed later observation. A positive decision
 * must not short-circuit structural validation of the complete evidence set.
 */
class BanEvidenceMalformedAfterBanTest {
    private static final Set<String> REQUIRED = Set.of("paper-profile", "external");

    @Test void positiveBanCannotHideAnUnexpectedLaterSource() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(REQUIRED, List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.BANNED),
                new BanEvidence.Source("unregistered-plugin", BanEvidence.Status.CLEAR))));
    }

    @Test void positiveBanCannotHideADuplicateLaterSource() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(REQUIRED, List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.BANNED),
                new BanEvidence.Source(" PAPER-PROFILE ", BanEvidence.Status.UNKNOWN))));
    }

    @Test void positiveBanCannotHideANullStatusLaterSource() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(REQUIRED, List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.BANNED),
                new BanEvidence.Source("external", null))));
    }

    @Test void positiveBanCannotHideANullLaterRecord() {
        assertThrows(IOException.class, () -> BanEvidence.evaluate(REQUIRED,
                java.util.Arrays.asList(
                        new BanEvidence.Source("paper-profile", BanEvidence.Status.BANNED),
                        null)));
    }
}
