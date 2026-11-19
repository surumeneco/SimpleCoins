package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperWonderfulLiveCandidateTest {
    private static final UUID ENTITY = new UUID(0, 12);
    private static final UUID WORLD = new UUID(0, 34);
    private static final UUID OWNER = new UUID(0, 56);

    @Test void oneCandidateKeepsItsSlotAmountsAndOwnerWithoutPublishing() {
        var candidate = new PaperWonderfulLiveCandidates.Candidate(
                ENTITY, WORLD, "world", "wwc",
                Map.of("follow_target", OWNER),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Map.of("shulker", BigInteger.valueOf(10000)),
                Map.of("shulker", "entity/" + ENTITY + "/wcl/wwc/shulker"));
        assertEquals(ENTITY, candidate.entityId());
        assertEquals(WORLD, candidate.worldId());
        assertEquals(OWNER, candidate.ownerEvidence().owner());
        assertEquals(BigInteger.valueOf(10000), candidate.total());
        assertEquals("entity/" + ENTITY + "/wcl/wwc/shulker",
                candidate.physicalSlotKeys().get("shulker"));
    }

    @Test void localHoldMustRemainDistinctFromZeroOrFree() {
        var candidate = new PaperWonderfulLiveCandidates.Candidate(
                ENTITY, WORLD, "world", "www", Map.of(),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.HOLD, null),
                Map.of("cargo:0", BigInteger.ZERO),
                Map.of("cargo:0", "entity/" + ENTITY + "/wcl/www/cargo:0"));
        assertEquals(WclOwnerEvidence.Kind.HOLD, candidate.ownerEvidence().kind());
        assertEquals(Map.of("cargo:0", BigInteger.ZERO), candidate.slotValues());
        assertEquals(BigInteger.ZERO, candidate.total());
    }

    @Test void invalidOrContradictoryCandidateCannotBeConstructed() {
        var owner = new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER);
        assertThrows(IllegalArgumentException.class, () ->
                new PaperWonderfulLiveCandidates.Candidate(
                        ENTITY, WORLD, "world", "wwc", Map.of(), owner,
                        Map.of("shulker", BigInteger.valueOf(10000)), Map.of()));
    }
}
