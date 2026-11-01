package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExternalOwnerDecisionTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-00000000002a");

    @Test void unavailableExternalOwnerCannotBecomeFreeOrPersonal() {
        var unknown = ExternalOwnerDecision.unknown();
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toBlock(
                unknown, BanEvidence.Status.CLEAR, true));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                unknown, BanEvidence.Status.CLEAR, true));
    }

    @Test void verifiedFreeAndPersonalDecisionsConvertWithoutInventingOwner() throws Exception {
        var free = ExternalOwnerDecision.toBlock(ExternalOwnerDecision.free(),
                BanEvidence.Status.UNKNOWN, false);
        assertNull(free.owner());
        assertFalse(free.currentlyBanned());
        assertFalse(free.hasPlayerdata());

        var personal = ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), BanEvidence.Status.BANNED, true);
        assertEquals(OWNER, personal.owner());
        assertTrue(personal.currentlyBanned());
        assertTrue(personal.hasPlayerdata());
    }

    @Test void unknownBanStatusCannotBeCoercedToClearForPersonalOwner() {
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toBlock(
                ExternalOwnerDecision.personal(OWNER), BanEvidence.Status.UNKNOWN, true));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                ExternalOwnerDecision.personal(OWNER), null, true));
    }

    @Test void malformedOwnerEvidenceIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () ->
                new ExternalOwnerDecision.Evidence(ExternalOwnerDecision.Kind.PERSONAL, null));
        assertThrows(IllegalArgumentException.class, () ->
                new ExternalOwnerDecision.Evidence(ExternalOwnerDecision.Kind.FREE, OWNER));
    }
}
