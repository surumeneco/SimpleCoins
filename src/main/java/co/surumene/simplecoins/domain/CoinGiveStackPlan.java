package co.surumene.simplecoins.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure, bounded, per-recipient stack planning for the two give command forms. */
public final class CoinGiveStackPlan {
    private CoinGiveStackPlan() {}

    /** One inventory stack. Creating the actual Paper ItemStack is the adapter's responsibility. */
    public record Stack(CoinDenomination denomination, int quantity) {
        public Stack {
            Objects.requireNonNull(denomination, "denomination");
            if (quantity < 1 || quantity > 64)
                throw new IllegalArgumentException("Coin stack quantity must be 1..64");
        }
    }

    /** Plan a positive amount using the fixed minimum-coin decomposition. */
    public static List<Stack> forAmount(long amount, int recipients) {
        return materialize(CoinMoney.minimumCoins(amount), recipients);
    }

    /** Plan a positive count of exactly one fixed denomination. */
    public static List<Stack> forDenomination(CoinDenomination denomination, long count, int recipients) {
        Objects.requireNonNull(denomination, "denomination");
        return materialize(Map.of(denomination, count), recipients);
    }

    private static List<Stack> materialize(Map<CoinDenomination, Long> counts, int recipients) {
        long stacksPerRecipient = 0;
        for (long count : counts.values())
            stacksPerRecipient = Math.addExact(stacksPerRecipient, IssuanceBudget.coinStacks(count));
        // Check the entire command before allocating potentially huge amounts of memory.
        IssuanceBudget.requireBudget(stacksPerRecipient, recipients);

        var result = new ArrayList<Stack>((int) stacksPerRecipient);
        CoinDenomination[] denominations = CoinDenomination.values();
        for (int i = denominations.length - 1; i >= 0; i--) {
            CoinDenomination denomination = denominations[i];
            long remaining = counts.getOrDefault(denomination, 0L);
            while (remaining > 0) {
                int quantity = (int) Math.min(remaining, 64L);
                result.add(new Stack(denomination, quantity));
                remaining -= quantity;
            }
        }
        return List.copyOf(result);
    }
}
