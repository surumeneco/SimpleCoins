package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WclLiveTameObservationTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID OTHER =
            UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test void authoritativeRegularChickenProvesNoVanillaTameOwner() throws Exception {
        var result = WclLiveTameObservation.chicken(true);
        assertTrue(result.verified());
        assertNull(result.owner());
        assertEquals(WclOwnerEvidence.Kind.FREE,
                WclOwnerEvidence.decide(true, "wwc", Map.of(), result).kind());
    }

    @Test void unverifiedChickenCannotBeFree() throws Exception {
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "wwc", Map.of(),
                        WclLiveTameObservation.chicken(false)).kind());
    }

    @Test void authoritativeTamedWolfWithMatchingWclOwnerIdentifiesOwner() throws Exception {
        var result = WclLiveTameObservation.wolf(true, true, OWNER);
        assertTrue(result.verified());
        assertEquals(OWNER, result.owner());
        assertEquals(OWNER, WclOwnerEvidence.decide(
                true, "www", Map.of("owner", OWNER), result).owner());
        // A missing WCL owner key does not prove the complete relationship
        // snapshot agrees with vanilla; the existing A05 rule is conservative.
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "www", Map.of(), result).kind());
    }

    @Test void authoritativeUntamedWolfProvesAbsence() throws Exception {
        var result = WclLiveTameObservation.wolf(true, false, null);
        assertTrue(result.verified());
        assertNull(result.owner());
        assertEquals(WclOwnerEvidence.Kind.FREE,
                WclOwnerEvidence.decide(true, "www", Map.of(), result).kind());
    }

    @Test void contradictoryTameStateOrMissingOwnerMustRemainUnknown() throws Exception {
        for (var result : new WclOwnerEvidence.TameOwner[] {
                WclLiveTameObservation.wolf(true, true, null),
                WclLiveTameObservation.wolf(true, false, OWNER),
                WclLiveTameObservation.wolf(false, true, OWNER),
                WclLiveTameObservation.wolf(false, false, null) }) {
            assertFalse(result.verified());
            assertEquals(WclOwnerEvidence.Kind.HOLD,
                    WclOwnerEvidence.decide(true, "www", Map.of(), result).kind());
        }
    }

    @Test void conflictingWclOwnerCannotBeSilentlyAdopted() throws Exception {
        var result = WclLiveTameObservation.wolf(true, true, OWNER);
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "www", Map.of("owner", OTHER), result).kind());
    }

    @Test void commanderPriorityIsPreservedWithMatchingIndependentOwner() throws Exception {
        var result = WclLiveTameObservation.wolf(true, true, OWNER);
        var resolved = WclOwnerEvidence.decide(true, "www",
                Map.of("commander", OTHER, "owner", OWNER), result);
        assertEquals(WclOwnerEvidence.Kind.OWNER, resolved.kind());
        assertEquals(OTHER, resolved.owner());
    }
}
