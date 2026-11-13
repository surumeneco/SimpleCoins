package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261108-003 / code-to-spec Candidate.
 * Reproduces conservation across a deterministic history of successful and
 * rejected wallet transitions, independently from the Paper inventory adapter.
 */
class WalletBalanceStatefulConservationCandidateTest {
    @Test void balanceAndRejectedOperationsPreserveValueAcrossCapChanges() {
        var random = new Random(0x5EEDBEEFL);
        long expected = 10_000L;
        WalletBalance balance = new WalletBalance(expected);
        long cap = 640_000L;
        int successful = 0;
        int rejected = 0;

        for (int step = 0; step < 12_000; step++) {
            if (step % 23 == 0) cap = 1 + random.nextInt(150_000);
            long value = 1 + random.nextInt(4_000);
            WalletBalance before = balance;
            long priorValue = expected;

            if (random.nextBoolean()) {
                if (expected + value <= cap) {
                    balance = balance.deposit(value, cap);
                    expected += value;
                    successful++;
                } else {
                    long currentCap = cap;
                    assertThrows(IllegalStateException.class,
                            () -> before.deposit(value, currentCap),
                            "deposit must reject over-cap or a newly reduced cap");
                    rejected++;
                }
            } else if (value <= expected) {
                balance = balance.withdraw(value);
                expected -= value;
                successful++;
            } else {
                assertThrows(IllegalStateException.class, () -> before.withdraw(value));
                rejected++;
            }

            assertEquals(priorValue, before.amount(), "original immutable value at step " + step);
            assertEquals(expected, balance.amount(), "reference-value parity at step " + step);
            assertTrue(balance.amount() >= 0, "wallet cannot become negative at step " + step);
        }

        assertTrue(successful > 1_000, "exercise enough valid transitions");
        assertTrue(rejected > 100, "exercise cap and insufficient-funds rejections");
    }
    /**
     * Observed WalletBalance arithmetic contract: overflow is rejected before
     * a replacement immutable balance is constructed, including a cap of MAX_VALUE.
     */
    @Test void overflowAndInvalidAmountsNeverMutateOriginalBalance() {
        WalletBalance nearlyFull = new WalletBalance(Long.MAX_VALUE - 1);
        assertThrows(ArithmeticException.class,
                () -> nearlyFull.deposit(2, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE - 1, nearlyFull.amount());

        WalletBalance full = nearlyFull.deposit(1, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, full.amount());
        assertEquals(Long.MAX_VALUE - 1, nearlyFull.amount());
        assertThrows(ArithmeticException.class,
                () -> full.deposit(1, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, full.amount());

        WalletBalance empty = new WalletBalance(0);
        assertThrows(IllegalArgumentException.class, () -> empty.deposit(0, 100));
        assertThrows(IllegalArgumentException.class, () -> empty.deposit(-1, 100));
        assertThrows(IllegalArgumentException.class, () -> empty.deposit(1, -1));
        assertThrows(IllegalArgumentException.class, () -> empty.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> empty.withdraw(-1));
        assertThrows(IllegalStateException.class, () -> empty.withdraw(1));
        assertEquals(0, empty.amount());
    }

}
