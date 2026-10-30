package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SavedPlayerAssetsTest {
    private static Map<String,Object> coin(int slot, int denomination, int quantity) {
        return Map.of("Slot", (byte) slot, "id", "minecraft:paper", "count", quantity,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", denomination))));
    }

    @Test void keepsInventoryAndEnderChestSlotsDistinctForSettlement() throws Exception {
        Map<String,Object> root = Map.of(
                "Inventory", List.of(coin(0, 5, 4), coin(1, 1, 2)),
                "EnderItems", List.of(coin(0, 100, 1)));
        Map<String,BigInteger> assets = SavedPlayerAssets.extract(root);
        assertEquals(3, assets.size());
        assertEquals(BigInteger.valueOf(20), assets.get("inventory:0"));
        assertEquals(BigInteger.valueOf(2), assets.get("inventory:1"));
        assertEquals(BigInteger.valueOf(100), assets.get("ender:0"));
    }

    @Test void countsModernEquipmentSlotsOutsideLegacyInventory() throws Exception {
        Map<String,Object> equipmentCoin = Map.of(
                "id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 50))));
        Map<String,Object> data = Map.of(
                "Inventory", List.of(), "EnderItems", List.of(),
                "equipment", Map.of("offhand", equipmentCoin));
        assertEquals(BigInteger.valueOf(100),
                SavedPlayerAssets.extract(data).get("equipment:offhand"));
    }

    @Test void missingInventoryOrDuplicatePhysicalSlotCannotBeTreatedAsComplete() {
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(
                Map.of("Inventory", List.of(coin(0, 1, 1)))));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(
                Map.of("Inventory", List.of(coin(0, 1, 1), coin(0, 1, 1)),
                        "EnderItems", List.of())));
    }
    @Test void overlappingModernAndLegacyEquipmentIsRejectedInsteadOfDoubleCounted() {
        Map<String,Object> item = Map.of("id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 100))));
        Map<String,Object> root = Map.of(
                "Inventory", List.of(coin(-106, 100, 1)),
                "EnderItems", List.of(),
                "equipment", Map.of("offhand", item));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(root));
    }

    @Test void malformedOrUnknownEquipmentDoesNotGetSilentlyIgnored() {
        Map<String,Object> badKind = Map.of(
                "Inventory", List.of(), "EnderItems", List.of(),
                "equipment", List.of("unexpected"));
        Map<String,Object> unknownSlot = Map.of(
                "Inventory", List.of(), "EnderItems", List.of(),
                "equipment", Map.of("unknown_slot", Map.of()));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(badKind));
        assertThrows(IOException.class, () -> SavedPlayerAssets.extract(unknownSlot));
    }

}
