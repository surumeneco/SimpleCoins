package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-003: only actual unslotted Entity inventories may be counted. */
class StoredEntityUnslottedInventoryTest {
    private static Map<String, Object> coin(boolean slotted) {
        Map<String, Object> value = new java.util.HashMap<>();
        value.put("id", "minecraft:paper");
        value.put("count", 1);
        value.put("components", Map.of("minecraft:custom_data",
                Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        if (slotted) value.put("Slot", (byte) 0);
        return value;
    }

    private static Map<String, Object> entity(String id, String inventoryTag,
                                               Map<String, Object> item) {
        return Map.of("id", id, "UUID", new int[]{0, 0, 0, 50},
                inventoryTag, List.of(item));
    }

    @Test void chestMinecartCannotInventUnslottedInventory() {
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity("minecraft:chest_minecart",
                        "Inventory", coin(false))))));
    }

    @Test void droppedItemCannotInventSecondUnslottedInventory() {
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity("minecraft:item",
                        "Inventory", coin(false))))));
    }

    @Test void villagerUnslottedInventoryRemainsCountable() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-000000000032:inventory:0",
                BigInteger.valueOf(100)), StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity("minecraft:villager",
                        "Inventory", coin(false))))));
    }

    @Test void allayUnslottedInventoryRemainsCountable() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-000000000032:inventory:0",
                BigInteger.valueOf(100)), StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity("minecraft:allay",
                        "Inventory", coin(false))))));
    }

    @Test void chestMinecartSlottedInventoryRemainsCountable() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-000000000032:inventory:0",
                BigInteger.valueOf(100)), StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity("minecraft:chest_minecart",
                        "Items", coin(true))))));
    }
}
