package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredItemFrameAssetsTest {
    @Test void extractsSavedNormalAndGlowFrameCurrencyWithoutCountingEmptyFrames() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 50))));
        Map<String, Object> frame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 10}, "Item", coin);
        Map<String, Object> glow = Map.of("id", "minecraft:glow_item_frame",
                "UUID", new int[]{0, 0, 0, 11}, "Item", coin);
        Map<String, Object> empty = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 12});
        assertEquals(Map.of(
                "00000000-0000-0000-0000-00000000000a", BigInteger.valueOf(100),
                "00000000-0000-0000-0000-00000000000b", BigInteger.valueOf(100)),
                StoredItemFrameAssets.extract(Map.of("Entities", List.of(frame, glow, empty))));
    }
}
