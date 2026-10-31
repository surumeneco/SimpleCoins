package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredItemDisplayAssetsTest {
    @Test void countsSavedItemDisplayCurrencyButNotDefaultAirDisplay() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> display = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 50}, "item", coin);
        Map<String, Object> empty = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 51},
                "item", Map.of("id", "minecraft:air", "count", 0));
        assertEquals(Map.of("00000000-0000-0000-0000-000000000032",
                        BigInteger.valueOf(100)),
                StoredItemDisplayAssets.extract(Map.of("Entities", List.of(display, empty))));
    }

    @Test void duplicatedItemDisplayUuidMustFailClosed() {
        Map<String, Object> display = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 50},
                "item", Map.of("id", "minecraft:air", "count", 0));
        assertThrows(java.io.IOException.class, () ->
                StoredItemDisplayAssets.extract(Map.of("Entities", List.of(display, display))));
    }

    @Test void malformedAirDisplayMustNotSilentlyBecomeEmpty() {
        Map<String, Object> display = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 50},
                "item", Map.of("id", "minecraft:air", "count", 1));
        assertThrows(java.io.IOException.class, () ->
                StoredItemDisplayAssets.extract(Map.of("Entities", List.of(display))));
    }
}
