package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoinImageSeparatorTest {
    @Test void treatsEachSeparatorCharacterAsOnePosition() {
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100-Coin"));
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100 Coin"));
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100_Coin"));
        assertEquals("100__Coin", CoinImageNames.normalizeStem("100--Coin"));
        assertNotEquals(CoinImageNames.normalizeStem("100--Coin"),
                CoinImageNames.normalizeStem("100 Coin"));
    }

    @Test void adjacentSeparatorsMustNotMatchASingleSeparator() {
        assertThrows(IllegalStateException.class, () ->
            CoinImageNames.uniquePng("100 Coin", List.of("100--Coin.png")));
        assertEquals("100--Coin.png",
            CoinImageNames.uniquePng("100__Coin", List.of("100--Coin.png")));
    }
}
