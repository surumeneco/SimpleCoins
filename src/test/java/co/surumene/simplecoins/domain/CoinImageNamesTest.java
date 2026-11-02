package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CoinImageNamesTest {
    @Test void spacesUnderscoresAndHyphensShareOneLookupStem() {
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100 Coin"));
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100_Coin"));
        assertEquals("100_Coin", CoinImageNames.normalizeStem("100-Coin"));
        assertEquals("100-Coin.png", CoinImageNames.uniquePng("100 Coin",
                List.of("1 Coin.png", "100-Coin.png", "unrelated.txt")));
    }

    @Test void ambiguousNormalizedFileNamesMustNotChooseAnArbitraryImage() {
        assertThrows(IllegalStateException.class,
                () -> CoinImageNames.uniquePng("100 Coin",
                        List.of("100 Coin.png", "100_Coin.png")));
        assertThrows(IllegalStateException.class,
                () -> CoinImageNames.uniquePng("100 Coin",
                        List.of("1 Coin.png", "100 Coin.jpg")));
    }

    @Test void invalidOrTraversingImageStemsAreRejected() {
        for (String name : List.of("", " ", "../100 Coin", "..\\100 Coin",
                "folder/100 Coin", "folder\\100 Coin")) {
            assertThrows(IllegalArgumentException.class,
                    () -> CoinImageNames.normalizeStem(name));
        }
        assertThrows(IllegalArgumentException.class,
                () -> CoinImageNames.normalizeStem(null));
    }
}
