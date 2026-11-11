package co.surumene.simplecoins.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates and splits one selected trader drop before allocating Paper items.
 * No world mutation, inventory insertion or journal write occurs here.
 */
public final class TraderDropStackPlan {
    /** Operational preparation ceiling, not the game's configurable drop probability. */
    public static final int MAX_STACKS_PER_PREPARATION = 1024;

    private TraderDropStackPlan() {}

    /**
     * Returns an immutable sequence of per-stack counts, each in 1..64.
     * Every precondition and the complete output size are validated before
     * allocating any list items. A caller must explicitly provide its budget.
     */
    public static List<Integer> counts(WanderingTraderDropPolicy.Drop drop, int maxStacks) {
        Objects.requireNonNull(drop, "drop");
        if (maxStacks < 1 || maxStacks > MAX_STACKS_PER_PREPARATION)
            throw new IllegalArgumentException("Stack budget must be in 1.."
                    + MAX_STACKS_PER_PREPARATION);
        long needed = drop.requiredStacks();
        if (needed > maxStacks)
            throw new IllegalArgumentException("Drop exceeds allowed synchronous stack budget");
        List<Integer> prepared = new ArrayList<>((int) needed);
        int remaining = drop.count();
        while (remaining > 0) {
            int count = Math.min(remaining, 64);
            prepared.add(count);
            remaining -= count;
        }
        return List.copyOf(prepared);
    }
}
