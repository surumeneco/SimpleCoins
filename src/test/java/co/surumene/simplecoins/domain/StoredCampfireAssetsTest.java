package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-003: four physical cooking slots per saved campfire. */
class StoredCampfireAssetsTest {
    private static Map<String, Object> coin(int slot) {
        return Map.of("Slot", (byte) slot, "id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
    }

    private static Map<String, Object> chunk(String id, List<?> items) {
        return Map.of("block_entities", List.of(Map.of(
                "id", id, "x", 0, "y", 64, "z", 0, "Items", items)));
    }

    @Test void campfireCannotInventFifthCookingSlot() {
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                chunk("minecraft:campfire", List.of(coin(4)))));
    }

    @Test void soulCampfireCannotInventFifthCookingSlot() {
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                chunk("minecraft:soul_campfire", List.of(coin(4)))));
    }

    @Test void finalPhysicalCookingSlotRemainsCountable() throws Exception {
        for (String id : List.of("minecraft:campfire", "minecraft:soul_campfire")) {
            assertEquals(Map.of("0,64,0:3", BigInteger.valueOf(100)),
                    StoredBlockEntityAssets.extract(chunk(id, List.of(coin(3)))), id);
        }
    }

    @Test void emptyCampfireInventoryRemainsValid() throws Exception {
        assertTrue(StoredBlockEntityAssets.extract(
                chunk("minecraft:campfire", List.of())).isEmpty());
    }
}
