package co.surumene.simplecoins.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Read-only preflight for resuming one persisted offline grant from a physical
 * coin-stack prefix. A partial amount is accepted only if it corresponds to
 * whole, ordered original stacks; an arbitrary intermediate sum may not be
 * treated as an authenticated saved-progress cursor.
 *
 * This pure planner does not persist playerdata, create Paper items, apply a
 * monetary mutation, acknowledge a grant or issue any currency.
 */
public final class GrantDeliveryBatchPlanner {
    private GrantDeliveryBatchPlanner() { }

    public record Batch(List<GrantDistributionPlan.CoinStack> stacks,
                        long previousValue, long nextValue, boolean complete) {
        public Batch {
            stacks = List.copyOf(stacks);
            if (previousValue < 0 || nextValue < previousValue)
                throw new IllegalArgumentException("Invalid grant delivery progress");
        }
    }

    /**
     * A verified playerdata receipt records the cumulative physical amount
     * already present for this grant; the caller must still prove receipt and
     * inventory came from the SAME persisted playerdata generation.
     * Only fully free slots are counted; a later live adapter can separately
     * validate stacking into occupied slots before any physical mutation.
     */
    public static Batch next(GrantLedger.Grant grant, long persistedDeliveredValue,
                             int freeSlots) {
        Objects.requireNonNull(grant, "grant");
        if (grant.delivered())
            throw new IllegalStateException("Acknowledged grant is not a delivery candidate");
        if (freeSlots < 0 || persistedDeliveredValue < 0
                || persistedDeliveredValue > grant.amount())
            throw new IllegalArgumentException("Invalid delivery capacity or saved grant progress");
        GrantDistributionPlan plan = grant.denomination() == null
                ? GrantDistributionPlan.forAmount(grant.amount(), 1)
                : GrantDistributionPlan.forExactCoins(grant.denomination(), grant.coinCount(), 1);
        List<GrantDistributionPlan.CoinStack> original = plan.coinStacks();
        int offset = 0;
        long value = 0;
        while (value < persistedDeliveredValue && offset < original.size()) {
            value = Math.addExact(value, stackValue(original.get(offset++)));
        }
        if (value != persistedDeliveredValue)
            throw new IllegalArgumentException("Saved delivered amount is not a canonical stack prefix");
        int end = offset + Math.min(freeSlots, original.size() - offset);
        List<GrantDistributionPlan.CoinStack> selected = new ArrayList<>(original.subList(offset, end));
        long nextValue = value;
        for (var stack : selected)
            nextValue = Math.addExact(nextValue, stackValue(stack));
        return new Batch(selected, value, nextValue, nextValue == grant.amount());
    }

    private static long stackValue(GrantDistributionPlan.CoinStack stack) {
        return Math.multiplyExact((long) stack.denomination().value(), stack.amount());
    }
}
