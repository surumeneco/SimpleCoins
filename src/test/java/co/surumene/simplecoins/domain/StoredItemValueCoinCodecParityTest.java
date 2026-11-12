package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Source-bound cross-adapter regression for the Paper 26.2 coin codec.
 * The physical coin codec uses simplecoins:coin_denomination and
 * minecraft:item_model, while pre-integration saved-data tests use
 * simplecoins:denomination. This is a technical candidate, not CAT approval.
 */
class StoredItemValueCoinCodecParityTest {
    private static Map<String, Object> storedCoin(String material, Object denomination,
                                                   Object model, int count) {
        return Map.of("id", material, "count", count, "components",
                Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:coin_denomination", denomination)),
                       "minecraft:item_model", model));
    }

    @Test void actualPaperCoinPdcMustNotDisappearAsZeroDuringSettlement() throws Exception {
        Map<String, Object> coin = storedCoin(
                "minecraft:paper", 100, "simplecoins:coin_100", 3);
        assertEquals(BigInteger.valueOf(300), StoredItemValue.value(coin));
    }

    @Test void actualPaperCoinNestedInSavedShulkerMustRemainAccounted() throws Exception {
        Map<String, Object> coin = storedCoin(
                "minecraft:paper", 5, "simplecoins:coin_5", 2);
        Map<String, Object> box = Map.of("id", "minecraft:shulker_box",
                "components", Map.of("minecraft:container",
                        List.of(Map.of("slot", 0, "item", coin))));
        assertEquals(BigInteger.TEN, StoredItemValue.value(box));
    }

    @Test void activeMarkerWithMissingOrMismatchedModelMustFailClosed() {
        Map<String, Object> wrong = storedCoin(
                "minecraft:paper", 100, "simplecoins:coin_500", 2);
        Map<String, Object> absent = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues",
                                Map.of("simplecoins:coin_denomination", 100))));
        assertThrows(IOException.class, () -> StoredItemValue.value(wrong));
        assertThrows(IOException.class, () -> StoredItemValue.value(absent));
    }

    @Test void counterfeitActiveMarkerMustFailInsteadOfBecomingZero() {
        Map<String, Object> wrongMaterial = storedCoin(
                "minecraft:stone", 100, "simplecoins:coin_100", 1);
        Map<String, Object> invalidDenomination = storedCoin(
                "minecraft:paper", 123, "simplecoins:coin_123", 1);
        Map<String, Object> invalidType = storedCoin(
                "minecraft:paper", "100", "simplecoins:coin_100", 1);
        assertThrows(IOException.class, () -> StoredItemValue.value(wrongMaterial));
        assertThrows(IOException.class, () -> StoredItemValue.value(invalidDenomination));
        assertThrows(IOException.class, () -> StoredItemValue.value(invalidType));
    }

    @Test void conflictingOldAndActiveKeysCannotDefineOnePhysicalCoinTwice() {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 1,
                "components", Map.of(
                        "minecraft:item_model", "simplecoins:coin_100",
                        "minecraft:custom_data", Map.of("PublicBukkitValues", Map.of(
                                "simplecoins:denomination", 5,
                                "simplecoins:coin_denomination", 100))));
        assertThrows(IOException.class, () -> StoredItemValue.value(coin));
    }
}
