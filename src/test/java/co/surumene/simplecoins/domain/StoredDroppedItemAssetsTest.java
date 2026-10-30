package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoredDroppedItemAssetsTest {
    private static Map<String,Object> coin(int count) {
        return Map.of("id", "minecraft:paper", "count", count,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 100))));
    }

    private static int[] uuidInts(UUID uuid) {
        return new int[] {
                (int)(uuid.getMostSignificantBits() >>> 32),
                (int)uuid.getMostSignificantBits(),
                (int)(uuid.getLeastSignificantBits() >>> 32),
                (int)uuid.getLeastSignificantBits()
        };
    }

    private static Map<String,Object> drop(UUID uuid, Map<String,Object> item) {
        return Map.of("id", "minecraft:item", "UUID", uuidInts(uuid), "Item", item);
    }

    @Test void readsDroppedCoinByPersistentEntityUuidWithoutCountingOrdinaryEntities() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Map<UUID,BigInteger> assets = StoredDroppedItemAssets.extract(Map.of(
                "Entities", List.of(drop(first, coin(3)),
                        drop(second, Map.of("id", "minecraft:stone", "count", 1)),
                        Map.of("id", "minecraft:pig", "UUID", uuidInts(UUID.randomUUID())))));
        assertEquals(Map.of(first, BigInteger.valueOf(300)), assets);
    }

    @Test void duplicatePhysicalEntityUuidAbortsInsteadOfDoubleCounting() {
        UUID same = UUID.randomUUID();
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(Map.of(
                "Entities", List.of(drop(same, coin(1)), drop(same, coin(2))))));
    }

    @Test void missingOrMalformedDroppedItemAbortsInsteadOfTreatingItAsZero() {
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(Map.of()));
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(Map.of(
                "Entities", List.of(Map.of("id", "minecraft:item",
                        "UUID", uuidInts(UUID.randomUUID()))))));
        assertThrows(IOException.class, () -> StoredDroppedItemAssets.extract(Map.of(
                "Entities", List.of(Map.of("id", "minecraft:item",
                        "UUID", new int[] {1, 2}, "Item", coin(1))))));
    }
}
