package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredDroppedItemAssetsTest {
    @Test void extractsPhysicalCurrencyFromUnloadedDroppedItemEntity() throws Exception {
        Map<String,Object> coin = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String,Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 7}, "Item", coin);
        assertEquals(Map.of("00000000-0000-0000-0000-000000000007",
                        BigInteger.valueOf(200)),
                StoredDroppedItemAssets.extract(Map.of("Entities", List.of(dropped))));
    }
}
