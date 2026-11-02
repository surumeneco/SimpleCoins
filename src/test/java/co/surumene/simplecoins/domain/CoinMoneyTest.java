package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoinMoneyTest {
    @Test void decomposesAmountsUsingTheAgreedMinimumCoinSeries() {
        assertEquals(Map.of(CoinDenomination.FIVE_THOUSAND, 1L,
                CoinDenomination.HUNDRED, 2L), CoinMoney.minimumCoins(5200));
        assertEquals(Map.of(CoinDenomination.ONE, 1L), CoinMoney.minimumCoins(1));
        assertEquals(Map.of(CoinDenomination.TEN_THOUSAND, 1L,
                CoinDenomination.ONE, 1L), CoinMoney.minimumCoins(10001));
        assertThrows(IllegalArgumentException.class, () -> CoinMoney.minimumCoins(0));
        assertThrows(IllegalArgumentException.class, () -> CoinMoney.minimumCoins(-1));
    }

    @Test void decompositionIsImmutableAndRetainsExactLongMaximumValue() {
        Map<CoinDenomination, Long> coins = CoinMoney.minimumCoins(Long.MAX_VALUE);
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), CoinMoney.totalValue(coins));
        assertThrows(UnsupportedOperationException.class,
                () -> coins.put(CoinDenomination.ONE, 2L));
    }

    @Test void totalValueUsesUnboundedArithmeticAndRejectsInvalidCounts() {
        BigInteger huge = BigInteger.ONE.shiftLeft(80);
        assertEquals(huge.multiply(BigInteger.valueOf(10000)),
                CoinMoney.totalValue(Map.of(CoinDenomination.TEN_THOUSAND, huge)));
        assertThrows(IllegalArgumentException.class,
                () -> CoinMoney.totalValue(Map.of(CoinDenomination.ONE, -1)));
        assertThrows(IllegalArgumentException.class,
                () -> CoinMoney.totalValue(Map.of(CoinDenomination.ONE, new BigDecimal("1.5"))));
        Map<CoinDenomination, Number> invalid = new HashMap<>();
        invalid.put(CoinDenomination.ONE, null);
        assertThrows(IllegalArgumentException.class, () -> CoinMoney.totalValue(invalid));
    }
}
