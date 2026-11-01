package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class BanEvidenceTest {
    @Test void onlyCompleteClearEvidenceCanProveAnOwnerIsNotBanned() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, BanEvidence.combine(List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("external", BanEvidence.Status.CLEAR))));
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.combine(List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("external", BanEvidence.Status.UNKNOWN))));
    }

    @Test void verifiedBanRemainsBannedEvenIfAnotherSourceIsUnknown() throws Exception {
        assertEquals(BanEvidence.Status.BANNED, BanEvidence.combine(List.of(
                new BanEvidence.Source("paper-profile", BanEvidence.Status.UNKNOWN),
                new BanEvidence.Source("external", BanEvidence.Status.BANNED))));
    }

    @Test void missingDuplicateOrMalformedBanSourceEvidenceFailsClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(List.of(
                new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                new BanEvidence.Source("paper", BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(List.of(
                new BanEvidence.Source("  ", BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(List.of(
                new BanEvidence.Source("paper", null))));
    }
}
