package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Characterizes existing coin decomposition independently of its greedy implementation.
 * This is observed behavior, not approval of a new normative CAT specification.
 */
class CoinMoneyExhaustiveConservationCharacterizationTest {
    @Test
    void conservesAllAmountsAndMatchesIndependentMinimalCoinOracle() {
        final int maxAmount = 100_000;
        final int[] denominationValues = Arrays.stream(CoinDenomination.values())
                .mapToInt(CoinDenomination::value)
                .toArray();
        final int[] minimumCounts = new int[maxAmount + 1];
        Arrays.fill(minimumCounts, Integer.MAX_VALUE / 2);
        minimumCounts[0] = 0;

        // Dynamic programming is deliberately independent of minimumCoins' greedy algorithm.
        for (int amount = 1; amount <= maxAmount; amount++) {
            for (int denomination : denominationValues) {
                if (denomination <= amount) {
                    minimumCounts[amount] = Math.min(
                            minimumCounts[amount], minimumCounts[amount - denomination] + 1);
                }
            }
        }

        for (int amount = 1; amount <= maxAmount; amount++) {
            Map<CoinDenomination, Long> actual = CoinMoney.minimumCoins(amount);
            assertEquals(BigInteger.valueOf(amount), CoinMoney.totalValue(actual),
                    "Value conservation at " + amount);
            long actualCoinCount = actual.values().stream().mapToLong(Long::longValue).sum();
            assertEquals(minimumCounts[amount], actualCoinCount,
                    "Minimum coin count at " + amount);
        }
    }

    @Test
    void conservesExtremePositiveLongAmountsWithoutOverflow() {
        for (long amount : new long[] {
                1L, 10_001L, 999_999_999L,
                Long.MAX_VALUE - 10_000L, Long.MAX_VALUE - 1L, Long.MAX_VALUE
        }) {
            Map<CoinDenomination, Long> actual = CoinMoney.minimumCoins(amount);
            assertEquals(BigInteger.valueOf(amount), CoinMoney.totalValue(actual),
                    "Value conservation near long boundary at " + amount);
        }
    }
}
