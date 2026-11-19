package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WclPhysicalSlotIdentityTest {
    private static final UUID WOLF = UUID.fromString("00000000-0000-0000-0000-000000000054");

    @Test void wolfWclWeaponUsesExactlyTheVanillaEquipmentPhysicalKey() throws Exception {
        var slots = WclPhysicalSlotIdentity.keys(WOLF, "www", List.of("weapon", "cargo:7"));
        assertEquals("entity/" + WOLF + "/equipment/mainhand", slots.get("weapon"));
        assertEquals("entity/" + WOLF + "/wcl/www/cargo:7", slots.get("cargo:7"));
    }

    @Test void anUnprovenDoubleViewOfTheMirroredWeaponCannotBeCommitted() throws Exception {
        var settlement = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("saved:entities"));
        String equipmentKey = "entity/" + WOLF + "/equipment/mainhand";
        String wclWeaponKey = WclPhysicalSlotIdentity.keys(WOLF, "www", Set.of("weapon"))
                .get("weapon");
        settlement.accept(new SettlementAccumulator.Asset("saved:entities", equipmentKey,
                "world", null, false, false, false, BigInteger.valueOf(10000),
                "world/world/entity/" + WOLF + "/equipment/mainhand"));
        assertThrows(IllegalArgumentException.class, () -> settlement.accept(
                new SettlementAccumulator.Asset("saved:entities", wclWeaponKey,
                        "world", null, false, false, false, BigInteger.valueOf(10000),
                        "world/world/entity/" + WOLF + "/wcl/www/weapon")));
        assertThrows(IllegalStateException.class, settlement::commit,
                "A collision must force HOLD, not silently double or drop currency");
    }

    @Test void chickenShulkerIsNotEquatedWithUnverifiedHeadEquipment() throws Exception {
        assertEquals("entity/" + WOLF + "/wcl/wwc/shulker",
                WclPhysicalSlotIdentity.keys(WOLF, "wwc", Set.of("shulker")).get("shulker"));
    }

    @Test void missingOrUnrecognizedSlotContractsAreNotAccepted() {
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "www", Set.of("cargo:07")));
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "www", Set.of("cargo:-1")));
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "www", Set.of("cargo:2147483648")));
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "unknown", Set.of("weapon")));
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "wwc", Set.of("weapon")));
        assertThrows(IOException.class,
                () -> WclPhysicalSlotIdentity.keys(WOLF, "www", List.of("weapon", "weapon")));
    }
}
