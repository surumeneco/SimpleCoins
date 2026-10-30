package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredBlockEntityAssetsTest {
    private static Map<String,Object> coin(int slot, int count) {
        return Map.of("Slot", (byte) slot, "id", "minecraft:paper", "count", count,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
    }

    @Test void extractsChestInventoryFromModernChunkCoordinatesAndSlots() throws Exception {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 24, "y", 70, "z", -9,
                "Items", List.of(coin(0, 2), coin(1, 3)));
        Map<String,BigInteger> assets = StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest)));
        assertEquals(2, assets.size());
        assertEquals(BigInteger.valueOf(200), assets.get("24,70,-9:0"));
        assertEquals(BigInteger.valueOf(300), assets.get("24,70,-9:1"));
    }

    @Test void decoratedPotSingleItemStackIsIncludedInPhysicalAssets() throws Exception {
        Map<String,Object> pot = Map.of(
                "id", "minecraft:decorated_pot", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3));
        Map<String,BigInteger> assets = StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(pot)));
        assertEquals(Map.of("5,70,-2:0", BigInteger.valueOf(300)), assets);
    }

    @Test void unknownSingleItemStorageFailsClosedInsteadOfBeingCountedAsEmpty() {
        Map<String,Object> unknown = Map.of(
                "id", "minecraft:unknown_storage", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(unknown))));
    }

    @Test void decoratedPotWithConflictingInventoryFormatsFailsClosed() {
        Map<String,Object> pot = Map.of(
                "id", "minecraft:decorated_pot", "x", 5, "y", 70, "z", -2,
                "item", coin(0, 3), "Items", List.of(coin(0, 2)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(pot))));
    }

    @Test void unknownOrDuplicatedBlockEntityInventoryCannotBeSilentlyCountedAsZero() {
        Map<String,Object> chest = Map.of(
                "id", "minecraft:chest", "x", 0, "y", 64, "z", 0,
                "Items", List.of(coin(0, 1)));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(Map.of()));
        assertThrows(IOException.class, () -> StoredBlockEntityAssets.extract(
                Map.of("block_entities", List.of(chest, chest))));
    }
}
