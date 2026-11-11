package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import org.junit.jupiter.api.Test;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure schema checks here; actual ItemStack/PDC round trips are exercised
 * on the real Paper 26.2 server rather than a Bukkit-less JUnit process.
 */
class CoinItemCodecSchemaTest {
    @Test
    void allNineFixedDenominationsHaveStableIdentityAndDefaultDisplay() {
        assertEquals(9, CoinDenomination.values().length);
        assertEquals(new NamespacedKey("simplecoins", "coin_denomination"),
                CoinItemCodec.DENOMINATION_KEY);
        Map<CoinDenomination, String> names = CoinItemCodec.defaultNames();
        assertEquals(9, names.size());
        for (CoinDenomination denomination : CoinDenomination.values()) {
            assertEquals(denomination.value() + " Coin", names.get(denomination));
            assertEquals(new NamespacedKey("simplecoins", "coin_" + denomination.value()),
                    CoinItemCodec.modelKey(denomination));
        }
        assertThrows(UnsupportedOperationException.class,
                () -> names.put(CoinDenomination.ONE, "forged"));
    }

    @Test
    void configuredNamesRequireAllDenominationsButDoNotChangeIdentifiers() {
        EnumMap<CoinDenomination, String> custom = new EnumMap<>(CoinDenomination.class);
        custom.putAll(CoinItemCodec.defaultNames());
        custom.put(CoinDenomination.FIFTY, "五十貨");
        assertDoesNotThrow(() -> new CoinItemCodec(custom));
        assertEquals(new NamespacedKey("simplecoins", "coin_50"),
                CoinItemCodec.modelKey(CoinDenomination.FIFTY));
        custom.remove(CoinDenomination.THOUSAND);
        assertThrows(NullPointerException.class, () -> new CoinItemCodec(custom));
        custom.put(CoinDenomination.THOUSAND, "  ");
        assertThrows(IllegalArgumentException.class, () -> new CoinItemCodec(custom));
        assertThrows(NullPointerException.class, () -> CoinItemCodec.modelKey(null));
        assertThrows(NullPointerException.class, () -> new CoinItemCodec(null));
    }
}
