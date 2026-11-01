package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-003: a dropped Entity cannot also expose an inventory. */
class StoredDroppedItemRepresentationIntegrityTest {
    private static Map<String, Object> coin() {
        return Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 100))));
    }

    private static Map<String, Object> dropped() {
        return Map.of("id", "minecraft:item", "UUID", new int[]{0, 0, 0, 7},
                "Item", coin());
    }

    @Test void normalDroppedStackRemainsCountable() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-000000000007",
                        BigInteger.valueOf(200)),
                StoredDroppedItemAssets.extract(Map.of("Entities", List.of(dropped()))));
    }

    @Test void droppedStackCannotAlsoExposeSlottedInventoryEvenWhenEmpty() {
        var entity = new java.util.HashMap<String, Object>(dropped());
        entity.put("Items", List.of());
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(
                Map.of("Entities", List.of(entity))));
    }

    @Test void droppedStackCannotAlsoExposeUnslottedInventoryEvenWhenEmpty() {
        var entity = new java.util.HashMap<String, Object>(dropped());
        entity.put("Inventory", List.of());
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(
                Map.of("Entities", List.of(entity))));
    }

    @Test void inventoryExtractorCannotCountFabricatedDroppedItemSlots() {
        var entity = new java.util.HashMap<String, Object>(dropped());
        var slotted = new java.util.HashMap<String, Object>(coin());
        slotted.put("Slot", (byte) 0);
        entity.put("Items", List.of(slotted));
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity))));
    }

    @Test void chestMinecartInventoryRemainsCountable() throws Exception {
        var slotted = new java.util.HashMap<String, Object>(coin());
        slotted.put("Slot", (byte) 0);
        var minecart = Map.of("id", "minecraft:chest_minecart",
                "UUID", new int[]{0, 0, 0, 8}, "Items", List.of(slotted));
        assertEquals(Map.of("00000000-0000-0000-0000-000000000008:inventory:0",
                        BigInteger.valueOf(200)),
                StoredEntityInventoryAssets.extract(Map.of("Entities", List.of(minecart))));
    }
}
