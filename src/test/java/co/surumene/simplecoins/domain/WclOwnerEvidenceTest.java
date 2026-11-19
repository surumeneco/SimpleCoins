package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WclOwnerEvidenceTest {
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-000000000111");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000222");

    @Test void commanderTakesPrecedenceOverVerifiedTameOwner() throws Exception {
        var result = WclOwnerEvidence.decide(true, "www",
                Map.of("commander", COMMANDER, "owner", OWNER),
                WclOwnerEvidence.TameOwner.verified(OWNER));
        assertEquals(WclOwnerEvidence.Kind.OWNER, result.kind());
        assertEquals(COMMANDER, result.owner());
    }

    @Test void wolfOwnerIsFallbackWhenCommanderAbsent() throws Exception {
        var result = WclOwnerEvidence.decide(true, "www", Map.of("owner", OWNER),
                WclOwnerEvidence.TameOwner.unknown());
        assertEquals(WclOwnerEvidence.Kind.OWNER, result.kind());
        assertEquals(OWNER, result.owner());
    }

    @Test void chickenFollowTargetWinsEvenIfTameObservationMissing() throws Exception {
        var result = WclOwnerEvidence.decide(true, "wwc", Map.of("follow_target", COMMANDER),
                WclOwnerEvidence.TameOwner.unknown());
        assertEquals(COMMANDER, result.owner());
    }

    @Test void missingFollowTargetAndUnverifiedTameOwnerCannotBecomeFree() throws Exception {
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "wwc", Map.of(),
                        WclOwnerEvidence.TameOwner.unknown()).kind());
    }

    @Test void verifiedAbsenceAllowsOnlyThatEntityToBeFree() throws Exception {
        assertEquals(WclOwnerEvidence.Kind.FREE,
                WclOwnerEvidence.decide(true, "wwc", Map.of(),
                        WclOwnerEvidence.TameOwner.verifiedAbsent()).kind());
        assertEquals(WclOwnerEvidence.Kind.FREE,
                WclOwnerEvidence.decide(true, "www", Map.of(),
                        WclOwnerEvidence.TameOwner.verifiedAbsent()).kind());
    }

    @Test void wolfDisagreeingVanillaAndWclOwnerMustHold() throws Exception {
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "www", Map.of("owner", OWNER),
                        WclOwnerEvidence.TameOwner.verified(COMMANDER)).kind());
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(true, "www", Map.of(),
                        WclOwnerEvidence.TameOwner.verified(OWNER)).kind());
    }

    @Test void unverifiableProviderMustNeverResolveOwnerOrFree() throws Exception {
        assertEquals(WclOwnerEvidence.Kind.HOLD,
                WclOwnerEvidence.decide(false, "www", Map.of("commander", COMMANDER),
                        WclOwnerEvidence.TameOwner.verifiedAbsent()).kind());
    }

    @Test void unfamiliarProviderRelationshipsAreNotEvidence() {
        assertThrows(IOException.class, () -> WclOwnerEvidence.decide(true, "wwc",
                Map.of("owner", OWNER), WclOwnerEvidence.TameOwner.unknown()));
        assertThrows(IOException.class, () -> WclOwnerEvidence.decide(true, "www",
                Map.of("follow_target", OWNER), WclOwnerEvidence.TameOwner.unknown()));
        assertThrows(IOException.class, () -> WclOwnerEvidence.decide(true, "mystery",
                Map.of(), WclOwnerEvidence.TameOwner.unknown()));
    }
}
