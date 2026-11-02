package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssetOwnerTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000002a");
    private static final UUID COMMANDER = UUID.fromString("00000000-0000-0000-0000-0000000000ab");

    @Test void verifiedOwnerWithPlayerdataAndNoBanIsPersonal() {
        var result = AssetOwner.resolve(OWNER, false, false, true);
        assertEquals(AssetOwner.Kind.PERSONAL, result.kind());
        assertEquals(OWNER, result.player());
        assertNull(result.reason());
    }

    @Test void bannedOwnerTakesPrecedenceOverMissingPlayerdata() {
        var result = AssetOwner.resolve(OWNER, false, true, false);
        assertEquals(AssetOwner.Kind.DEAD, result.kind());
        assertEquals(OWNER, result.player());
        assertEquals(AssetOwner.DeadReason.CURRENT_BAN, result.reason());
    }

    @Test void missingPlayerdataIsDeadNotFree() {
        var result = AssetOwner.resolve(OWNER, false, false, false);
        assertEquals(AssetOwner.Kind.DEAD, result.kind());
        assertEquals(AssetOwner.DeadReason.MISSING_PLAYERDATA, result.reason());
    }

    @Test void verifiedFreeAndEveryoneProtectedHaveNoPersonalFields() {
        for (boolean banned : new boolean[]{false, true}) {
            for (boolean hasData : new boolean[]{false, true}) {
                for (UUID owner : new UUID[]{null, OWNER}) {
                    var result = AssetOwner.resolve(owner, true, banned, hasData);
                    assertEquals(AssetOwner.Kind.FREE, result.kind());
                    assertNull(result.player());
                    assertNull(result.reason());
                }
                var ownerless = AssetOwner.resolve(null, false, banned, hasData);
                assertEquals(AssetOwner.Kind.FREE, ownerless.kind());
                assertNull(ownerless.player());
                assertNull(ownerless.reason());
            }
        }
    }

    @Test void allValidAndInvalidRecordShapesAreEnforced() {
        assertEquals(AssetOwner.Kind.FREE,
                new AssetOwner.Result(AssetOwner.Kind.FREE, null, null).kind());
        assertEquals(AssetOwner.Kind.PERSONAL,
                new AssetOwner.Result(AssetOwner.Kind.PERSONAL, OWNER, null).kind());
        assertEquals(AssetOwner.Kind.DEAD,
                new AssetOwner.Result(AssetOwner.Kind.DEAD, OWNER,
                        AssetOwner.DeadReason.CURRENT_BAN).kind());
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(null, OWNER, null));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.FREE, OWNER, null));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.FREE, null,
                        AssetOwner.DeadReason.CURRENT_BAN));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.PERSONAL, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.PERSONAL, OWNER,
                        AssetOwner.DeadReason.MISSING_PLAYERDATA));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.DEAD, OWNER, null));
        assertThrows(IllegalArgumentException.class, () ->
                new AssetOwner.Result(AssetOwner.Kind.DEAD, null,
                        AssetOwner.DeadReason.CURRENT_BAN));
    }

    @Test void commanderPrecedenceIsStableForAllKnownCombinations() {
        assertEquals(COMMANDER, AssetOwner.mobOwner(COMMANDER, OWNER));
        assertEquals(COMMANDER, AssetOwner.mobOwner(COMMANDER, null));
        assertEquals(OWNER, AssetOwner.mobOwner(null, OWNER));
        assertNull(AssetOwner.mobOwner(null, null));
    }

    @Test void sixteenInputCombinationsFollowPostValidationStateTable() {
        int combinations = 0;
        for (UUID owner : new UUID[]{null, OWNER}) {
            for (boolean everyone : new boolean[]{false, true}) {
                for (boolean banned : new boolean[]{false, true}) {
                    for (boolean playerdata : new boolean[]{false, true}) {
                        var result = AssetOwner.resolve(owner, everyone, banned, playerdata);
                        AssetOwner.Kind expected = owner == null || everyone
                                ? AssetOwner.Kind.FREE
                                : banned || !playerdata ? AssetOwner.Kind.DEAD : AssetOwner.Kind.PERSONAL;
                        assertEquals(expected, result.kind());
                        if (expected == AssetOwner.Kind.DEAD) {
                            assertEquals(banned ? AssetOwner.DeadReason.CURRENT_BAN
                                    : AssetOwner.DeadReason.MISSING_PLAYERDATA, result.reason());
                        } else {
                            assertNull(result.reason());
                        }
                        combinations++;
                    }
                }
            }
        }
        assertEquals(16, combinations);
    }
}
