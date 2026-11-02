package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-006: independent offline-give output-budget boundary tests.
 * The pure budget check is not evidence of durable, once-only delivery.
 */
class IssuanceBudgetRecipientMatrixTest {
    @Test void allRecipientCountsRespectTheGlobal1024StackLimit() {
        for (int recipients = 1; recipients <= 1025; recipients++) {
            final int targets = recipients;
            final long maxPerTarget = IssuanceBudget.MAX_STACKS_PER_COMMAND / targets;
            if (maxPerTarget > 0) {
                assertDoesNotThrow(() -> IssuanceBudget.requireBudget(maxPerTarget, targets),
                        "Exact global budget must be accepted for " + targets + " recipients");
            } else {
                assertThrows(IllegalArgumentException.class,
                        () -> IssuanceBudget.requireBudget(maxPerTarget, targets));
            }
            assertThrows(IllegalArgumentException.class,
                    () -> IssuanceBudget.requireBudget(maxPerTarget + 1, targets),
                    "One stack above the global budget must be rejected");
            if (targets <= IssuanceBudget.MAX_STACKS_PER_COMMAND) {
                assertDoesNotThrow(() -> IssuanceBudget.requireBudget(1, targets));
            } else {
                assertThrows(IllegalArgumentException.class,
                        () -> IssuanceBudget.requireBudget(1, targets));
            }
        }
    }

    @Test void stackRoundingAtEvery64CoinBoundaryDoesNotOverflow() {
        for (long coins : new long[]{1, 63, 64, 65, 127, 128, 129,
                1024, 1025, Long.MAX_VALUE}) {
            long expected = coins / 64 + (coins % 64 == 0 ? 0 : 1);
            assertEquals(expected, IssuanceBudget.coinStacks(coins));
        }
        assertThrows(IllegalArgumentException.class,
                () -> IssuanceBudget.requireBudget(Long.MAX_VALUE, Integer.MAX_VALUE));
    }
}
