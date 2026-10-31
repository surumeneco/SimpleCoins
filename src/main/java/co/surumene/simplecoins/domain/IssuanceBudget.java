package co.surumene.simplecoins.domain;

/** Upper bound on the number of output stacks created in one synchronous give command. */
public final class IssuanceBudget {
    public static final long MAX_STACKS_PER_COMMAND = 1024L;

    private IssuanceBudget() {}

    public static long coinStacks(long count) {
        if (count <= 0) throw new IllegalArgumentException("Count must be positive");
        return 1 + (count - 1) / 64;
    }

    /**
     * The budget is global to the command, not per target.
     * Wallets count as one stack each (each wallet is non-stackable).
     */
    public static void requireBudget(long stacksPerTarget, int targets) {
        if (stacksPerTarget <= 0 || targets <= 0)
            throw new IllegalArgumentException("Expected positive stacks and targets");
        if (stacksPerTarget > MAX_STACKS_PER_COMMAND / targets)
            throw new IllegalArgumentException("More than 1024 stacks across recipients; split the command");
    }
}
