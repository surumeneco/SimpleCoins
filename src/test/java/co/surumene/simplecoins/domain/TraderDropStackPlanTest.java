package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Verification independent of Bukkit. Paper ItemStack PDC/model materialization
 * must additionally be asserted on a running Paper server.
 */
class TraderDropStackPlanTest {
    private static WanderingTraderDropPolicy.Drop drop(int amount) {
        return new WanderingTraderDropPolicy.Drop(CoinDenomination.TEN_THOUSAND, amount);
    }

    @Test
    void exactBoundaryAndRemainderCountsPreserveValue() {
        assertEquals(List.of(1), TraderDropStackPlan.counts(drop(1), 1));
        assertEquals(List.of(63), TraderDropStackPlan.counts(drop(63), 1));
        assertEquals(List.of(64), TraderDropStackPlan.counts(drop(64), 1));
        assertEquals(List.of(64, 1), TraderDropStackPlan.counts(drop(65), 2));
        assertEquals(List.of(64, 64), TraderDropStackPlan.counts(drop(128), 2));
        assertEquals(List.of(64, 64, 1), TraderDropStackPlan.counts(drop(129), 3));
        List<Integer> maximum = TraderDropStackPlan.counts(drop(65_536), 1024);
        assertEquals(1024, maximum.size());
        assertTrue(maximum.stream().allMatch(n -> n == 64));
        assertEquals(655_360_000L, (long) maximum.stream().mapToInt(Integer::intValue).sum()
                * CoinDenomination.TEN_THOUSAND.value());
        assertThrows(UnsupportedOperationException.class, () -> maximum.add(1));
    }

    @Test
    void rejectsUnboundedOrPartialAllocationBeforePreparingAnything() {
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(65), 1));
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(65_537), 1024));
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(Integer.MAX_VALUE), 1024));
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(1), 0));
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(1), -1));
        assertThrows(IllegalArgumentException.class, () -> TraderDropStackPlan.counts(drop(1), 1025));
        assertThrows(NullPointerException.class, () -> TraderDropStackPlan.counts(null, 1));
    }

    @Test
    void fixedSeedRandomizedValuesAreConserved() {
        Random random = new Random(0x434F494E20261110L);
        for (int i = 0; i < 10_000; i++) {
            int total = 1 + random.nextInt(65_536);
            int budget = (int) (1 + (total - 1L) / 64);
            List<Integer> counts = TraderDropStackPlan.counts(drop(total), budget);
            assertEquals(budget, counts.size());
            assertTrue(counts.stream().allMatch(n -> n >= 1 && n <= 64));
            long sum = counts.stream().mapToLong(Integer::longValue).sum();
            assertEquals(total, sum);
            assertEquals((long) total * CoinDenomination.TEN_THOUSAND.value(),
                    sum * CoinDenomination.TEN_THOUSAND.value());
        }
    }
