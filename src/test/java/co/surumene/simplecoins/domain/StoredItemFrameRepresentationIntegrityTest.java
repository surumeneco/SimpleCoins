package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-003: a frame displays one Item, never an extra inventory. */
class StoredItemFrameRepresentationIntegrityTest {
    private static Map<String, Object> coin() {
        return Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 100))));
    }

    private static Map<String, Object> frame(String type) {
        return Map.of("id", type, "UUID", new int[]{0, 0, 0, 9}, "Item", coin());
    }

    @Test void normalFrameStillContainsOnePhysicalCoinStack() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-000000000009",
                        BigInteger.valueOf(200)),
                StoredItemFrameAssets.extract(Map.of("Entities",
                        List.of(frame("minecraft:item_frame")))));
    }

    @Test void normalFrameCannotHaveEvenAnEmptySlottedInventory() {
        var entity = new java.util.HashMap<String, Object>(
                frame("minecraft:item_frame"));
        entity.put("Items", List.of());
        assertThrows(IOException.class, () -> StoredItemFrameAssets.extract(
                Map.of("Entities", List.of(entity))));
    }

    @Test void glowFrameCannotHaveAnUnslottedInventory() {
        var entity = new java.util.HashMap<String, Object>(
                frame("minecraft:glow_item_frame"));
        entity.put("Inventory", List.of());
        assertThrows(IOException.class, () -> StoredItemFrameAssets.extract(
                Map.of("Entities", List.of(entity))));
    }

    @Test void inventoryExtractorRejectsFabricatedFrameSlots() {
        var entity = new java.util.HashMap<String, Object>(
                frame("minecraft:item_frame"));
        var slotted = new java.util.HashMap<String, Object>(coin());
        slotted.put("Slot", (byte) 0);
        entity.put("Items", List.of(slotted));
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(entity))));
    }
}
