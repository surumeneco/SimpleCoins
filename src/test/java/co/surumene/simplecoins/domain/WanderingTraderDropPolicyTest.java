package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Source-bound domain tests; ItemStack issuance and journal integration are separate. */
class WanderingTraderDropPolicyTest {
    @Test
    void disabledAndZeroChanceNeverIssueAndFixedWeightIsDeterministic() {
        var disabled = new WanderingTraderDropPolicy(false, 1.0, 1, 1, Map.of());
        assertTrue(disabled.draw(new Random(1)).isEmpty());
        var never = new WanderingTraderDropPolicy(true, 0, 1, 1, Map.of(CoinDenomination.ONE, 1));
        assertTrue(never.draw(new Random(1)).isEmpty());
        var fixed = new WanderingTraderDropPolicy(true, 1, 1, 1, Map.of(CoinDenomination.FIVE, 9));
        var chosen = fixed.draw(new Random(1)).orElseThrow();
        assertEquals(CoinDenomination.FIVE, chosen.denomination());
        assertEquals(1, chosen.count());
        assertEquals(5L, chosen.value());
    }

    @Test
    void positiveWeightsControlChoiceAndRangeIsInclusive() {
        var draw = new WanderingTraderDropPolicy(true, 1, 1, 64, Map.of(
                CoinDenomination.ONE, 1, CoinDenomination.FIVE, 3, CoinDenomination.TEN, 0));
        Random random = new Random(0xC01F00DL);
        int ones = 0, fives = 0;
        boolean sawMin = false, sawMax = false;
        for (int i = 0; i < 10_000; i++) {
            var output = draw.draw(random).orElseThrow();
            assertTrue(output.count() >= 1 && output.count() <= 64);
            assertEquals((long) output.denomination().value() * output.count(), output.value());
            if (output.count() == 1) sawMin = true;
            if (output.count() == 64) sawMax = true;
            if (output.denomination() == CoinDenomination.ONE) ones++;
            else if (output.denomination() == CoinDenomination.FIVE) fives++;
            else fail("Zero-weight denomination selected");
        }
        assertTrue(ones > 2000 && ones < 3000, "Fixed-seed weighted selection imbalance: " + ones);
        assertEquals(10_000, ones + fives);
        assertTrue(sawMin && sawMax);
    }

    @Test
    void constructorDefensivelyCopiesWeightsAndAcceptsAllNineDenominations() {
        var mutable = new EnumMap<CoinDenomination, Integer>(CoinDenomination.class);
        mutable.put(CoinDenomination.THOUSAND, 1);
        var draw = new WanderingTraderDropPolicy(true, 1, 3, 3, mutable);
        mutable.put(CoinDenomination.ONE, 999);
        var result = draw.draw(new Random(5)).orElseThrow();
        assertEquals(CoinDenomination.THOUSAND, result.denomination());
        assertEquals(3, result.count());
        assertEquals(3000L, result.value());
        for (CoinDenomination d : CoinDenomination.values()) {
            var single = new WanderingTraderDropPolicy(true, 1, 1, 1, Map.of(d, 1));
            assertEquals(d, single.draw(new Random(5)).orElseThrow().denomination());
        }
    }

    @Test
    void amountsAboveAStackAndIntegerMaxAreNotTruncated() {
        var twoStacks = new WanderingTraderDropPolicy(true, 1, 65, 65,
                Map.of(CoinDenomination.HUNDRED, 1));
        var result = twoStacks.draw(new Random(7)).orElseThrow();
        assertEquals(65, result.count());
        assertEquals(2L, result.requiredStacks());
        assertEquals(6500L, result.value());
        var largest = new WanderingTraderDropPolicy(true, 1,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Map.of(CoinDenomination.TEN_THOUSAND, 1))
                .draw(new Random(7)).orElseThrow();
        assertEquals(Integer.MAX_VALUE, largest.count());
        assertEquals(33_554_432L, largest.requiredStacks());
        assertEquals(21_474_836_470_000L, largest.value());
    }

    @Test
    void malformedConfigAndDropFailClosed() {
        var valid = Map.of(CoinDenomination.ONE, 1);
        for (double probability : new double[]{-0.1, 1.1, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new WanderingTraderDropPolicy(true, probability, 1, 1, valid));
        }
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy(true, 1, 0, 1, valid));
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy(true, 1, 2, 1, valid));
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy(true, 1, 1, 0, valid));
        assertThrows(IllegalArgumentException.class,
                () -> new WanderingTraderDropPolicy(true, 1, 1, 1, Map.of(CoinDenomination.ONE, -1)));
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy(true, 1, 1, 1, Map.of()));
        assertThrows(NullPointerException.class, () -> new WanderingTraderDropPolicy(true, 1, 1, 1, null));
        assertThrows(NullPointerException.class, () -> new WanderingTraderDropPolicy.Drop(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy.Drop(CoinDenomination.ONE, 0));
        assertThrows(IllegalArgumentException.class, () -> new WanderingTraderDropPolicy.Drop(CoinDenomination.ONE, -1));
        assertThrows(NullPointerException.class, () -> new WanderingTraderDropPolicy(true, 1, 1, 1, valid).draw(null));
    }
}
