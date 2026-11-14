package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, command-neutral preflight for the approved two give forms.
 * Planning does not create an ItemStack, a wallet UUID, a journal reservation,
 * or actual money. Physical issuance requires separate durable accounting.
 */
public final class GrantDistributionPlan {
    public record CoinStack(CoinDenomination denomination, int amount) {
        public CoinStack {
            Objects.requireNonNull(denomination, "coin denomination");
            if (amount < 1 || amount > 64)
                throw new IllegalArgumentException("Coin stack must contain 1..64 items");
        }
    }

    private final List<CoinStack> coinStacks;
    private final long emptyWalletCountPerRecipient;
    private final int recipientCount;
    private final long monetaryValuePerRecipient;
    private final long stacksPerRecipient;

    private GrantDistributionPlan(List<CoinStack> coinStacks,
                                  long emptyWalletCountPerRecipient, int recipientCount,
                                  long monetaryValuePerRecipient, long stacksPerRecipient) {
        IssuanceBudget.requireBudget(stacksPerRecipient, recipientCount);
        this.coinStacks = List.copyOf(coinStacks);
        this.emptyWalletCountPerRecipient = emptyWalletCountPerRecipient;
        this.recipientCount = recipientCount;
        this.monetaryValuePerRecipient = monetaryValuePerRecipient;
        this.stacksPerRecipient = stacksPerRecipient;
    }

    /** Split a positive total into the minimum number of approved denominations. */
    public static GrantDistributionPlan forAmount(long amount, int recipients) {
        return planCoinStacks(CoinMoney.minimumCoins(amount), amount, recipients);
    }

    /** Preserve an explicitly requested denomination, including beyond 64 items. */
    public static GrantDistributionPlan forExactCoins(CoinDenomination denomination,
                                                      long count, int recipients) {
        Objects.requireNonNull(denomination, "coin denomination");
        if (count <= 0) throw new IllegalArgumentException("Coin count must be positive");
        final long value;
        try { value = Math.multiplyExact(count, (long) denomination.value()); }
        catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Coin issuance value overflows long", overflow);
        }
        return planCoinStacks(Map.of(denomination, count), value, recipients);
    }

    /** Wallets are nonstackable and contain no currency when first created. */
    public static GrantDistributionPlan forEmptyWallets(long count, int recipients) {
        IssuanceBudget.requireBudget(count, recipients);
        return new GrantDistributionPlan(List.of(), count, recipients, 0, count);
    }

    private static GrantDistributionPlan planCoinStacks(
            Map<CoinDenomination, Long> coins, long value, int recipients) {
        // Prove the GLOBAL budget before allocating any output stack list.
        long stackCount = 0;
        for (long count : coins.values()) {
            if (count <= 0) throw new IllegalArgumentException("Coin count must be positive");
            stackCount = Math.addExact(stackCount, IssuanceBudget.coinStacks(count));
        }
        IssuanceBudget.requireBudget(stackCount, recipients);
        List<CoinStack> planned = new ArrayList<>((int) stackCount);
        CoinDenomination[] denominations = CoinDenomination.values();
        for (int i = denominations.length - 1; i >= 0; --i) {
            CoinDenomination denomination = denominations[i];
            long remaining = coins.getOrDefault(denomination, 0L);
            while (remaining > 0) {
                int size = (int) Math.min(remaining, 64L);
                planned.add(new CoinStack(denomination, size));
                remaining -= size;
            }
        }
        return new GrantDistributionPlan(planned, 0, recipients, value, stackCount);
    }

    public List<CoinStack> coinStacks() { return coinStacks; }
    public long emptyWalletCountPerRecipient() { return emptyWalletCountPerRecipient; }
    public int recipientCount() { return recipientCount; }
    public long monetaryValuePerRecipient() { return monetaryValuePerRecipient; }
    public long stacksPerRecipient() { return stacksPerRecipient; }

    public long totalOutputStacks() {
        // The constructor already established total <= 1024.
        return Math.multiplyExact(stacksPerRecipient, recipientCount);
    }

    public BigInteger totalMonetaryValue() {
        return BigInteger.valueOf(monetaryValuePerRecipient)
                .multiply(BigInteger.valueOf(recipientCount));
    }
}
