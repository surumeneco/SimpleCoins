package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WalletBalanceTest {
    @Test void reducedLimitDoesNotDestroyAnExistingBalance() {
        WalletBalance original = new WalletBalance(650_000);
        assertThrows(IllegalStateException.class, () -> original.deposit(1, 640_000));
        WalletBalance afterWithdrawal = original.withdraw(20_000);
        assertEquals(650_000, original.amount());
        assertEquals(630_000, afterWithdrawal.amount());
        assertEquals(640_000, afterWithdrawal.deposit(10_000, 640_000).amount());
    }

    @Test void depositAndWithdrawalRejectInvalidOrExcessiveAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new WalletBalance(-1));
        WalletBalance balance = new WalletBalance(100);
        assertThrows(IllegalArgumentException.class, () -> balance.deposit(0, 640_000));
        assertThrows(IllegalArgumentException.class, () -> balance.deposit(1, -1));
        assertThrows(IllegalStateException.class, () -> balance.deposit(101, 200));
        assertThrows(IllegalArgumentException.class, () -> balance.withdraw(0));
        assertThrows(IllegalStateException.class, () -> balance.withdraw(101));
        assertEquals(100, balance.amount());
    }

    @Test void arithmeticOverflowCannotWrapWalletValue() {
        assertThrows(ArithmeticException.class,
                () -> new WalletBalance(Long.MAX_VALUE).deposit(1, Long.MAX_VALUE));
        assertEquals(0, new WalletBalance(1).withdraw(1).amount());
    }
}
