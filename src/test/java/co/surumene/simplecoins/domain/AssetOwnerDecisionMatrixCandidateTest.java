package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec observation of the existing pure classification boundary.
 * These tests do not authorize unknown evidence to become FREE, or establish
 * that external BAN/playerdata sources are complete and trustworthy.
 */
class AssetOwnerDecisionMatrixCandidateTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0001-0000-000000000002");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0003-0000-000000000004");

    @Test
    void absentOwnerIsCurrentlyFreeEvenWhenBanAndPlayerdataFlagsVary() {
        for (boolean banned : new boolean[]{false, true}) {
            for (boolean playerdata : new boolean[]{false, true}) {
                var result = AssetOwner.resolve(null, false, banned, playerdata);
                assertEquals(AssetOwner.Kind.FREE, result.kind());
                assertNull(result.player());
                assertNull(result.reason());
            }
        }
    }

    @Test
    void everyoneMarkerCurrentlyPrecedesBanAndMissingPlayerdata() {
        var result = AssetOwner.resolve(OWNER, true, true, false);
        assertEquals(AssetOwner.Kind.FREE, result.kind());
        assertNull(result.player());
        assertNull(result.reason());
    }

    @Test
    void banTakesPrecedenceOverMissingPlayerdataForKnownOwner() {
        var result = AssetOwner.resolve(OWNER, false, true, false);
        assertEquals(AssetOwner.Kind.DEAD, result.kind());
        assertEquals(OWNER, result.player());
        assertEquals(AssetOwner.DeadReason.CURRENT_BAN, result.reason());
    }

    @Test
    void missingPlayerdataAndActiveOwnerRemainDistinct() {
        var missing = AssetOwner.resolve(OWNER, false, false, false);
        assertEquals(AssetOwner.Kind.DEAD, missing.kind());
        assertEquals(AssetOwner.DeadReason.MISSING_PLAYERDATA, missing.reason());
        var active = AssetOwner.resolve(OWNER, false, false, true);
        assertEquals(AssetOwner.Kind.PERSONAL, active.kind());
        assertEquals(OWNER, active.player());
        assertNull(active.reason());
    }

    @Test
    void commanderOverridesTameOwnerWithoutInventingAnUnknownOwner() {
        assertEquals(COMMANDER, AssetOwner.mobOwner(COMMANDER, OWNER));
        assertEquals(OWNER, AssetOwner.mobOwner(null, OWNER));
        assertNull(AssetOwner.mobOwner(null, null));
    }
}
