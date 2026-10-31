package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoredItemValueTest {
    private static Map<String,Object> item(String type, int count, Map<String,Object> pdc) {
        return Map.of("id", type, "count", count, "components",
                Map.of("minecraft:custom_data", Map.of("PublicBukkitValues", pdc)));
    }

    @Test void accountsForCustomDataCoinAndWalletWithoutTreatingOrdinaryPaperAsCurrency() throws Exception {
        Map<String,Object> coin = item("minecraft:paper", 3,
                Map.of("simplecoins:denomination", 100));
        Map<String,Object> wallet = item("minecraft:bundle", 1,
                Map.of("simplecoins:wallet_id", "stable-id", "simplecoins:wallet_balance", 975L));
        assertEquals(BigInteger.valueOf(300), StoredItemValue.value(coin));
        assertEquals(BigInteger.valueOf(975), StoredItemValue.value(wallet));
        assertEquals(BigInteger.ZERO, StoredItemValue.value(Map.of("id", "minecraft:paper", "count", 64)));
    }

    @Test void corruptCurrencyCannotQuietlyBecomeAnUncountedItem() {
        Map<String,Object> invalid = item("minecraft:paper", 1,
                Map.of("simplecoins:denomination", 123));
        assertThrows(java.io.IOException.class, () -> StoredItemValue.value(invalid));
    }
    @Test void includesAssetsInNestedBundleAndShulkerContents() throws Exception {
        Map<String,Object> coin = item("minecraft:paper", 2,
                Map.of("simplecoins:denomination", 5));
        Map<String,Object> wallet = item("minecraft:bundle", 1,
                Map.of("simplecoins:wallet_id", "nested-wallet", "simplecoins:wallet_balance", 21L));
        Map<String,Object> bundle = Map.of("id", "minecraft:bundle", "count", 1,
                "components", Map.of("minecraft:bundle_contents", java.util.List.of(coin, wallet)));
        Map<String,Object> shulker = Map.of("id", "minecraft:shulker_box", "count", 1,
                "components", Map.of("minecraft:container", java.util.List.of(
                        Map.of("slot", 0, "item", bundle))));
        assertEquals(BigInteger.valueOf(31), StoredItemValue.value(shulker));
    }

    @Test void walletWithHiddenBundleContentsMustNotSilentlyLoseAssetValue() {
        Map<String,Object> hidden = item("minecraft:paper", 1,
                Map.of("simplecoins:denomination", 100));
        Map<String,Object> wallet = Map.of("id", "minecraft:bundle", "count", 1,
                "components", Map.of(
                        "minecraft:custom_data", Map.of("PublicBukkitValues",
                                Map.of("simplecoins:wallet_id", "wallet",
                                       "simplecoins:wallet_balance", 1000L)),
                        "minecraft:bundle_contents", java.util.List.of(hidden)));
        assertThrows(java.io.IOException.class, () -> StoredItemValue.value(wallet));
    }

    @Test void coinWithNestedContainerMustNotHideAssets() {
        Map<String,Object> inner = item("minecraft:paper", 1,
                Map.of("simplecoins:denomination", 100));
        Map<String,Object> malformedCoin = Map.of("id", "minecraft:paper", "count", 1,
                "components", Map.of(
                        "minecraft:custom_data", Map.of("PublicBukkitValues",
                                Map.of("simplecoins:denomination", 5)),
                        "minecraft:container", java.util.List.of(
                                Map.of("slot", 0, "item", inner))));
        assertThrows(java.io.IOException.class, () -> StoredItemValue.value(malformedCoin));
    }


    @Test void omittedModernItemCountMeansOneNotCorruptOrZero() throws Exception {
        // The modern item codec omits count for a single-item stack.
        Map<String,Object> singleCoin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        assertEquals(BigInteger.valueOf(100), StoredItemValue.value(singleCoin));
    }

    @Test void impossibleModernStackCountCannotMultiplyNestedCurrency() {
        Map<String,Object> coin = item("minecraft:paper", 1,
                Map.of("simplecoins:denomination", 100));
        Map<String,Object> impossible = Map.of("id", "minecraft:bundle", "count", 100,
                "components", Map.of("minecraft:bundle_contents", java.util.List.of(coin)));
        assertThrows(java.io.IOException.class, () -> StoredItemValue.value(impossible));
    }

    @Test void defaultCountOneAlsoAppliesToNestedContainerItems() throws Exception {
        Map<String,Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String,Object> bundle = Map.of("id", "minecraft:bundle",
                "components", Map.of("minecraft:bundle_contents", java.util.List.of(coin)));
        assertEquals(BigInteger.valueOf(100), StoredItemValue.value(bundle));
    }
}
