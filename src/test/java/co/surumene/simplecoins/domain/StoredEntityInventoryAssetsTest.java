package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredEntityInventoryAssetsTest {
    @Test void extractsSavedChestBoatAndMinecartInventorySlotsWithoutAssigningOwners() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 3,
                "Slot", (byte) 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 10))));
        Map<String, Object> boat = Map.of("id", "minecraft:oak_chest_boat",
                "UUID", new int[]{0, 0, 0, 30}, "Items", List.of(coin));
        Map<String, Object> minecart = Map.of("id", "minecraft:chest_minecart",
                "UUID", new int[]{0, 0, 0, 31}, "Items", List.of(coin));
        assertEquals(Map.of(
                "00000000-0000-0000-0000-00000000001e:inventory:2", BigInteger.valueOf(30),
                "00000000-0000-0000-0000-00000000001f:inventory:2", BigInteger.valueOf(30)),
                StoredEntityInventoryAssets.extract(Map.of("Entities", List.of(boat, minecart))));
    }
}
