package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IssuanceBudgetTest {
    @Test void limitsTotalOutputAcrossAllRecipients() {
        assertEquals(1L, IssuanceBudget.coinStacks(64));
        assertEquals(2L, IssuanceBudget.coinStacks(65));
        assertEquals(1L, IssuanceBudget.coinStacks(1));
        assertDoesNotThrow(() -> IssuanceBudget.requireBudget(512, 2));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.requireBudget(513, 2));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.requireBudget(1, 0));
    }
    @Test void avoidsOverflowOnHugeValuesAndWalletIssuance() {
        assertEquals(144115188075855872L, IssuanceBudget.coinStacks(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.requireBudget(Long.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> IssuanceBudget.requireBudget(1025, 1));
    }
}
