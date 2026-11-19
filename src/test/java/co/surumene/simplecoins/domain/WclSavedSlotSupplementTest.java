package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Confirmed A05: a selected SAVED WCL holder never creates a second source. */
class WclSavedSlotSupplementTest {
    private static final String SOURCE = "saved:entity/region/r.0.0.mca";
    private static final UUID ENTITY = UUID.fromString("00000000-0000-0000-0000-000000000091");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000092");

    private static SettlementAccumulator pending() {
        return new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()), Set.of(SOURCE));
    }

    @Test void wwcShulkerAddsPersonalMoneyUnderTheExistingSavedEntitySource() throws Exception {
        var acc = pending();
        WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.valueOf(10000)),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Set.of(), Set.of(OWNER));
        acc.completeSource(SOURCE);
        var result = acc.commit();
        assertEquals(BigInteger.valueOf(10000), result.personalBalances().get(OWNER));
        assertEquals(BigInteger.valueOf(10000), result.statistics().worldTotal());
        assertEquals(BigInteger.ZERO, result.free());
    }

    @Test void ownerWithMissingPlayerdataOrCurrentBanIsDeadNotFree() throws Exception {
        for (boolean banned : new boolean[]{false, true}) {
            var acc = pending();
            WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "wwc",
                    Map.of("shulker", BigInteger.valueOf(10000)),
                    new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                    banned ? Set.of(OWNER) : Set.of(),
                    banned ? Set.of(OWNER) : Set.of());
            acc.completeSource(SOURCE);
            var result = acc.commit();
            assertEquals(BigInteger.valueOf(10000), result.dead());
            assertEquals(BigInteger.ZERO, result.statistics().worldTotal());
            assertEquals(1, result.deadLocations().size());
        }
    }

    @Test void positiveWwwWeaponOrWwcHeadCannotBeAddedWithoutMirrorProof() {
        for (String providerSlot : new String[]{"www/weapon", "wwc/head"}) {
            var pair = providerSlot.split("/");
            var acc = pending();
            assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                    acc, SOURCE, "world", ENTITY, pair[0],
                    Map.of(pair[1], BigInteger.valueOf(10000)),
                    new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                    Set.of(), Set.of()));
        }
    }

    @Test void repeatedSavedSlotAndAlreadyStagedVanillaAliasAreRejected() throws Exception {
        var acc = pending();
        var free = new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null);
        WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.valueOf(10000)), free, Set.of(), Set.of());
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.valueOf(10000)), free, Set.of(), Set.of()));

        var vanilla = pending();
        vanilla.accept(new SettlementAccumulator.Asset(SOURCE,
                "entity/" + ENTITY + "/wcl/wwc/shulker", "world",
                null, false, false, false, BigInteger.valueOf(10000), "world/entity"));
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                vanilla, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.valueOf(10000)), free, Set.of(), Set.of()));
    }

    @Test void unknownOwnerAndWrongSourceNeverBecomeZeroOrFree() {
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                pending(), SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.ZERO),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.HOLD, null),
                Set.of(), Set.of()));
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                pending(), "invented-saved-source", "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.ONE),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Set.of(), Set.of()));
    }

    @Test void zeroObservedWclSlotDoesNotCreateMoney() throws Exception {
        var acc = pending();
        WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.ZERO),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Set.of(), Set.of());
        acc.completeSource(SOURCE);
        assertEquals(BigInteger.ZERO, acc.commit().statistics().worldTotal());
    }

    @Test void provenSavedEquipmentMirrorKeepsOnePreviouslyStagedNativeCoin() throws Exception {
        var acc = pending();
        var sourceKey = "entity/" + ENTITY + "/equipment/mainhand";
        acc.accept(new SettlementAccumulator.Asset(SOURCE, sourceKey, "world",
                OWNER, false, false, true, BigInteger.valueOf(10000),
                "world/world/" + sourceKey));
        WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "www",
                Map.of("weapon", BigInteger.valueOf(10000)),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Set.of(), Set.of(OWNER), Set.of("weapon"));
        acc.completeSource(SOURCE);
        var result = acc.commit();
        assertEquals(BigInteger.valueOf(10000), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(10000), result.personalBalances().get(OWNER));
        assertEquals(BigInteger.ZERO, result.free());
    }

    @Test void provenHeadMirrorStillRequiresMatchingExistingNativeSlot() throws Exception {
        var acc = pending();
        var sourceKey = "entity/" + ENTITY + "/equipment/head";
        acc.accept(new SettlementAccumulator.Asset(SOURCE, sourceKey, "world",
                null, false, false, false, BigInteger.valueOf(10000),
                "world/world/" + sourceKey));
        WclSavedSlotSupplement.stage(acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("head", BigInteger.valueOf(10000)),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Set.of(), Set.of(), Set.of("head"));
        acc.completeSource(SOURCE);
        assertEquals(BigInteger.valueOf(10000), acc.commit().free());
    }

    @Test void amountOwnerAndMissingNativeCannotBeCoalesced() throws Exception {
        var nativeKey = "entity/" + ENTITY + "/equipment/mainhand";
        for (int different : new int[]{0, 1, 2}) {
            var acc = pending();
            if (different != 2)
                acc.accept(new SettlementAccumulator.Asset(SOURCE, nativeKey, "world",
                        different == 1 ? null : OWNER, false, false,
                        different == 1 ? false : true,
                        different == 0 ? BigInteger.valueOf(5000) : BigInteger.valueOf(10000),
                        "world/world/" + nativeKey));
            assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                    acc, SOURCE, "world", ENTITY, "www",
                    Map.of("weapon", BigInteger.valueOf(10000)),
                    new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                    Set.of(), Set.of(OWNER), Set.of("weapon")));
            assertThrows(IllegalStateException.class, acc::commit);
        }
    }

    @Test void proofForNonmirrorOrUnknownProviderSlotIsRejected() {
        var acc = pending();
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                acc, SOURCE, "world", ENTITY, "wwc",
                Map.of("shulker", BigInteger.valueOf(10000)),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.FREE, null),
                Set.of(), Set.of(), Set.of("shulker")));
    }

    @Test void classificationChangeFromPersonalToDeadIsNotEquivalent() throws Exception {
        var acc = pending();
        var key = "entity/" + ENTITY + "/equipment/mainhand";
        acc.accept(new SettlementAccumulator.Asset(SOURCE, key, "world",
                OWNER, false, false, true, BigInteger.valueOf(10000),
                "world/world/" + key));
        assertThrows(IOException.class, () -> WclSavedSlotSupplement.stage(
                acc, SOURCE, "world", ENTITY, "www",
                Map.of("weapon", BigInteger.valueOf(10000)),
                new WclOwnerEvidence.Decision(WclOwnerEvidence.Kind.OWNER, OWNER),
                Set.of(OWNER), Set.of(OWNER), Set.of("weapon")));
    }
}
