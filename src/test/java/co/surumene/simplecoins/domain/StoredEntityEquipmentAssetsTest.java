package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredEntityEquipmentAssetsTest {
    @Test void extractsCurrencyInSavedArmorStandAndMobEquipmentWithoutAssumingOwnership() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 50))));
        Map<String, Object> stand = Map.of("id", "minecraft:armor_stand",
                "UUID", new int[]{0, 0, 0, 20},
                "equipment", Map.of("mainhand", coin));
        Map<String, Object> mob = Map.of("id", "minecraft:zombie",
                "UUID", new int[]{0, 0, 0, 21},
                "equipment", Map.of("offhand", coin));
        assertEquals(Map.of(
                "00000000-0000-0000-0000-000000000014:mainhand", BigInteger.valueOf(100),
                "00000000-0000-0000-0000-000000000015:offhand", BigInteger.valueOf(100)),
                StoredEntityEquipmentAssets.extract(Map.of("Entities", List.of(stand, mob))));
    }

    @Test void malformedEquipmentMustNotBeInterpretedAsEmpty() {
        Map<String, Object> mob = Map.of("id", "minecraft:armor_stand",
                "UUID", new int[]{0, 0, 0, 20},
                "equipment", Map.of("mainhand", "not-an-item"));
        assertThrows(java.io.IOException.class, () ->
                StoredEntityEquipmentAssets.extract(Map.of("Entities", List.of(mob))));
    }

    @Test void duplicateEquipmentEntityUuidIsAmbiguous() {
        Map<String, Object> mob = Map.of("id", "minecraft:armor_stand",
                "UUID", new int[]{0, 0, 0, 20}, "equipment", Map.of());
        assertThrows(java.io.IOException.class, () ->
                StoredEntityEquipmentAssets.extract(Map.of("Entities", List.of(mob, mob))));
    }
}
