package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-006, CAT Draft simplecoins.phase7.issuance-budget-matrix.
 * Code-to-spec of existing global give budget only; not Paper/offline integration.
 */
class IssuanceBudgetMatrixTest {
    private static final BigInteger LIMIT =
            BigInteger.valueOf(IssuanceBudget.MAX_STACKS_PER_COMMAND);

    @Test void globalBudgetMatchesUnboundedProductAcrossOverflowBoundaries() {
        long[] stacks = {1, 2, 3, 7, 8, 16, 32, 63, 64, 65, 127, 128, 129,
                512, 1024, 1025, Long.MAX_VALUE};
        int[] recipients = {1, 2, 3, 4, 7, 8, 16, 32, 64, 127, 128, 256,
                512, 1024, 1025, Integer.MAX_VALUE};

        for (long stackCount : stacks) {
            for (int recipientCount : recipients) {
                BigInteger exact = BigInteger.valueOf(stackCount)
                        .multiply(BigInteger.valueOf(recipientCount));
                String caseName = "stacks=" + stackCount + ", recipients=" + recipientCount;
                if (exact.compareTo(LIMIT) <= 0) {
                    assertDoesNotThrow(
                            () -> IssuanceBudget.requireBudget(stackCount, recipientCount),
                            caseName);
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> IssuanceBudget.requireBudget(stackCount, recipientCount),
                            caseName);
                }
            }
        }
    }

    @Test void coinStackCeilingMatchesUnboundedArithmeticAtLongBoundaries() {
        long[] counts = {1, 2, 63, 64, 65, 127, 128, 129,
                65535, 65536, 65537, Long.MAX_VALUE};

        for (long count : counts) {
            long exact = BigInteger.valueOf(count).add(BigInteger.valueOf(63))
                    .divide(BigInteger.valueOf(64)).longValueExact();
            assertEquals(exact, IssuanceBudget.coinStacks(count), "count=" + count);
        }
    }

    @Test void rejectsNonPositiveCountsAndRecipientNumbers() {
        assertThrows(IllegalArgumentException.class,
                () -> IssuanceBudget.requireBudget(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> IssuanceBudget.requireBudget(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> IssuanceBudget.requireBudget(1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> IssuanceBudget.requireBudget(1, -1));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.coinStacks(0));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.coinStacks(-1));
    }
}
