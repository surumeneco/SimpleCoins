package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.BanEvidence;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperBanEvidenceAdapterTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000031");

    @Test void currentProfileBanStatusIsOneNamedSource() {
        var banned = PaperBanEvidenceAdapter.read(OWNER, id -> true);
        var clear = PaperBanEvidenceAdapter.read(OWNER, id -> false);
        assertEquals("paper-profile", banned.id());
        assertEquals(BanEvidence.Status.BANNED, banned.status());
        assertEquals(BanEvidence.Status.CLEAR, clear.status());
    }

    @Test void failedMissingOrNullProfileLookupIsUnknown() {
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperBanEvidenceAdapter.read(OWNER, id -> null).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperBanEvidenceAdapter.read(OWNER, null).status());
        assertEquals(BanEvidence.Status.UNKNOWN,
                PaperBanEvidenceAdapter.read(OWNER, id -> {
                    throw new IllegalStateException("Paper lookup unavailable");
                }).status());
        assertThrows(IllegalArgumentException.class,
                () -> PaperBanEvidenceAdapter.read(null, id -> false));
    }

    @Test void PaperClearDoesNotProveMissingExternalOrIpBanSource() throws Exception {
        var paper = PaperBanEvidenceAdapter.read(OWNER, id -> false);
        var incomplete = BanEvidence.evaluate(
                Set.of("paper-profile", "external", "ip-ban"), List.of(paper));
        assertEquals(BanEvidence.Status.UNKNOWN, incomplete.status());
        var withExternalBan = BanEvidence.evaluate(
                Set.of("paper-profile", "external", "ip-ban"),
                List.of(paper, new BanEvidence.Source("external", BanEvidence.Status.BANNED)));
        assertEquals(BanEvidence.Status.BANNED, withExternalBan.status());
    }
}
