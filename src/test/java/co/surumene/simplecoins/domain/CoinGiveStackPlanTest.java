package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pure Phase 7 give-stack planning tests; no Paper items or durable grant writes. */
class CoinGiveStackPlanTest {
    private static BigInteger value(List<CoinGiveStackPlan.Stack> plan) {
        return plan.stream()
                .map(s -> BigInteger.valueOf(s.denomination().value())
                        .multiply(BigInteger.valueOf(s.quantity())))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    @Test
    void amount5200UsesFewestCoinsWithExactValue() {
        var stacks = CoinGiveStackPlan.forAmount(5200L, 1);
        assertEquals(List.of(
                new CoinGiveStackPlan.Stack(CoinDenomination.FIVE_THOUSAND, 1),
                new CoinGiveStackPlan.Stack(CoinDenomination.HUNDRED, 2)), stacks);
        assertEquals(BigInteger.valueOf(5200L), value(stacks));
    }

    @Test
    void amountAbove64CoinsSplitsIntoValidStacks() {
        var stacks = CoinGiveStackPlan.forAmount(650000L, 1);
        assertEquals(List.of(
                new CoinGiveStackPlan.Stack(CoinDenomination.TEN_THOUSAND, 64),
                new CoinGiveStackPlan.Stack(CoinDenomination.TEN_THOUSAND, 1)), stacks);
        assertEquals(BigInteger.valueOf(650000), value(stacks));
    }

    @Test
    void specificDenominationSplits129Into64_64_1() {
        var stacks = CoinGiveStackPlan.forDenomination(CoinDenomination.FIFTY, 129L, 1);
        assertEquals(List.of(
                new CoinGiveStackPlan.Stack(CoinDenomination.FIFTY, 64),
                new CoinGiveStackPlan.Stack(CoinDenomination.FIFTY, 64),
                new CoinGiveStackPlan.Stack(CoinDenomination.FIFTY, 1)), stacks);
        assertEquals(BigInteger.valueOf(6450), value(stacks));
    }

    @Test
    void recipientBudgetUsesWholeCommandNotEachPlayer() {
        assertEquals(1024, CoinGiveStackPlan.forDenomination(CoinDenomination.ONE, 65536L, 1).size());
        assertThrows(IllegalArgumentException.class,
                () -> CoinGiveStackPlan.forDenomination(CoinDenomination.ONE, 65536L, 2));
        assertDoesNotThrow(() -> CoinGiveStackPlan.forDenomination(CoinDenomination.ONE, 32768L, 2));
        assertDoesNotThrow(() -> CoinGiveStackPlan.forAmount(1L, 1024));
        assertThrows(IllegalArgumentException.class, () -> CoinGiveStackPlan.forAmount(1L, 1025));
    }

    @Test
    void invalidAndExtremeRequestsFailBeforeExpansion() {
        assertThrows(IllegalArgumentException.class, () -> CoinGiveStackPlan.forAmount(0, 1));
        assertThrows(IllegalArgumentException.class, () -> CoinGiveStackPlan.forAmount(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> CoinGiveStackPlan.forAmount(1, 0));
        assertThrows(IllegalArgumentException.class, () -> CoinGiveStackPlan.forAmount(Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> CoinGiveStackPlan.forDenomination(CoinDenomination.TEN, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> CoinGiveStackPlan.forDenomination(CoinDenomination.TEN, -1, 1));
        assertThrows(NullPointerException.class,
                () -> CoinGiveStackPlan.forDenomination(null, 1, 1));
    }

    @Test
    void returnedPlanIsImmutable() {
        var stacks = CoinGiveStackPlan.forAmount(5200, 1);
        assertThrows(UnsupportedOperationException.class,
                () -> stacks.add(new CoinGiveStackPlan.Stack(CoinDenomination.ONE, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new CoinGiveStackPlan.Stack(CoinDenomination.ONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new CoinGiveStackPlan.Stack(CoinDenomination.ONE, 65));
    }
}
