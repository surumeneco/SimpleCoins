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
}
