package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-003: Items is not authority to invent physical Entity slots. */
class StoredEntitySlottedHolderDomainTest {
    private static Map<String, Object> coin() {
        return Map.of("id", "minecraft:paper", "count", 1, "Slot", (byte) 0,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
    }

    private static Map<String, Object> holder(String id) {
        return Map.of("id", id, "UUID", new int[]{0, 0, 0, 91},
                "Items", List.of(coin()));
    }

    @Test void cowCannotInventSlottedCurrency() {
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(holder("minecraft:cow")))));
    }

    @Test void unknownPluginEntityCannotInventSlottedCurrency() {
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(holder("custom:unknown_holder")))));
    }

    @Test void inventedVanillaNamespaceChestBoatCannotInventSlots() {
        assertThrows(IOException.class, () -> StoredEntityInventoryAssets.extract(
                Map.of("Entities", List.of(holder("minecraft:fictional_chest_boat")))));
    }

    @Test void knownPaleOakChestBoatRemainsSupported() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-00000000005b:inventory:0",
                        BigInteger.valueOf(100)),
                StoredEntityInventoryAssets.extract(Map.of("Entities",
                        List.of(holder("minecraft:pale_oak_chest_boat")))));
    }

    @Test void supportedChestMinecartStillCountsPhysicalSlot() throws Exception {
        assertEquals(Map.of("00000000-0000-0000-0000-00000000005b:inventory:0",
                        BigInteger.valueOf(100)),
                StoredEntityInventoryAssets.extract(
                        Map.of("Entities", List.of(holder("minecraft:chest_minecart")))));
    }

    @Test void noInventoryTagIsNotAStorageHolder() throws Exception {
        assertTrue(StoredEntityInventoryAssets.extract(Map.of("Entities",
                List.of(Map.of("id", "minecraft:cow")))).isEmpty());
    }
}
