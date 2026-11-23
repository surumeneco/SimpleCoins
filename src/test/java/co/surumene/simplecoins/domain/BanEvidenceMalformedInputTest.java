package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Characterization of malformed BAN provider input: no invalid evidence can imply CLEAR. */
class BanEvidenceMalformedInputTest {
    @Test
    void missingOrEmptyDeclarationsFailClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(null, List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of(), List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of("paper"), null));
    }

    @Test
    void malformedRequiredProviderNamesFailClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of(" "), List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper", " "), List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR))));
    }

    @Test
    void nullObservationOrFieldsFailClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), Arrays.asList((BanEvidence.Source) null)));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(new BanEvidence.Source(null, BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(new BanEvidence.Source(" ", BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(new BanEvidence.Source("paper", null))));
    }

    @Test
    void absentRequiredSourceIsUnknownRatherThanClear() throws Exception {
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.combine(
                Set.of("paper", "external"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR))));
    }

    @Test
    void observedBanDominatesMissingSourceButNotMalformedEvidence() throws Exception {
        assertEquals(BanEvidence.Status.BANNED, BanEvidence.combine(
                Set.of("paper", "external"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.BANNED))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper", "external"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.BANNED),
                        new BanEvidence.Source("external", null))));
    }
}
