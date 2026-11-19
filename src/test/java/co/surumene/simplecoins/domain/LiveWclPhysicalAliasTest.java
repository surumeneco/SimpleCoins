package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies the physical-value alias without asserting that two observations
 * are from a frozen source/writer generation or may be coalesced.
 */
class LiveWclPhysicalAliasTest {
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000054");

    private static SettlementAccumulator.Asset observed(String source, String physicalKey,
                                                        UUID world, long value) {
        return new SettlementAccumulator.Asset(source, physicalKey, world.toString(),
                null, false, false, false, BigInteger.valueOf(value),
                "source/" + source + "/" + physicalKey);
    }

    private static String liveMainhand(UUID world, UUID entity) {
        return "world/" + world + "/entity/" + entity + "/equipment/mainhand";
    }

    @Test
    void sameWolfMirroredWeaponCannotBeCountedFromLiveAndWclSeparately() throws Exception {
        var staged = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("live", "wcl"));
        String wclWeapon = WclPhysicalSlotIdentity.keys(WOLF, "www", Set.of("weapon")).get("weapon");
        assertEquals("entity/" + WOLF + "/equipment/mainhand", wclWeapon);
        staged.accept(observed("live", liveMainhand(WORLD, WOLF), WORLD, 5000));
        assertThrows(IllegalArgumentException.class,
                () -> staged.accept(observed("wcl", wclWeapon, WORLD, 5000)));
        staged.completeSource("live");
        staged.completeSource("wcl");
        assertThrows(IllegalStateException.class, staged::commit,
                "A rejected duplicate must keep the entire generation unpublished");
    }

    @Test
    void savedAndLiveEquipmentOfSameEntityAlsoCollide() {
        var staged = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("live", "saved"));
        staged.accept(observed("saved", "entity/" + WOLF + "/equipment/mainhand", WORLD, 5000));
        assertThrows(IllegalArgumentException.class,
                () -> staged.accept(observed("live", liveMainhand(WORLD, WOLF), WORLD, 5000)));
        staged.completeSource("saved");
        staged.completeSource("live");
        assertThrows(IllegalStateException.class, staged::commit);
    }

    @Test
    void distinctEquipmentAndCargoRemainIndependent() {
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000055");
        var staged = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("live", "wcl"));
        staged.accept(observed("live", liveMainhand(WORLD, WOLF), WORLD, 5000));
        staged.accept(observed("live", liveMainhand(WORLD, other), WORLD, 2000));
        String cargoKey = "entity/" + WOLF + "/wcl/www/cargo:7";
        staged.accept(observed("wcl", cargoKey, WORLD, 3000));
        staged.completeSource("live");
        staged.completeSource("wcl");
        assertEquals(BigInteger.valueOf(10000), staged.commit().statistics().worldTotal());
    }

    @Test
    void existingRawLiveSourceIdentityIsNotModified() {
        String raw = liveMainhand(WORLD, WOLF);
        var asset = observed("live", raw, WORLD, 5000);
        var staged = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("live"));
        staged.accept(asset);
        staged.completeSource("live");
        assertEquals(raw, asset.physicalKey());
        assertEquals(BigInteger.valueOf(5000), staged.commit().statistics().worldTotal());
    }
}
