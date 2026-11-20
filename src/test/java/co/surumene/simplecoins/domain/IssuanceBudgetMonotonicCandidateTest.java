package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/**
 * Candidate characterization of the global stack budget for offline/multi-recipient give.
 * Not a substitute for confirmed CAT gates or Paper acceptance.
 */
class IssuanceBudgetMonotonicCandidateTest {
    @Test void stackConversionMatchesOverflowSafeCeilingDivisionAtLongBoundaries() {
        long[] counts = {1, 63, 64, 65, 127, 128, 129,
                Long.MAX_VALUE - 64, Long.MAX_VALUE - 1, Long.MAX_VALUE};
        for (long count : counts) {
            long expected = BigInteger.valueOf(count)
                    .add(BigInteger.valueOf(63))
                    .divide(BigInteger.valueOf(64))
                    .longValueExact();
            assertEquals(expected, IssuanceBudget.coinStacks(count),
                    "count=" + count);
        }
    }

    @Test void recipientMultiplicationCannotOverflowAndAdmitOversizedGive() {
        int[] recipients = {1, 2, 3, 1024, 1025, Integer.MAX_VALUE};
        long[] stacks = {1, 2, 512, 1024, 1025, Long.MAX_VALUE};
        for (int recipientCount : recipients) {
            for (long stacksPerRecipient : stacks) {
                boolean fits = BigInteger.valueOf(stacksPerRecipient)
                        .multiply(BigInteger.valueOf(recipientCount))
                        .compareTo(BigInteger.valueOf(IssuanceBudget.MAX_STACKS_PER_COMMAND)) <= 0;
                if (fits) {
                    assertDoesNotThrow(
                            () -> IssuanceBudget.requireBudget(stacksPerRecipient, recipientCount),
                            "stacks=" + stacksPerRecipient + ", recipients=" + recipientCount);
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> IssuanceBudget.requireBudget(stacksPerRecipient, recipientCount),
                            "stacks=" + stacksPerRecipient + ", recipients=" + recipientCount);
                }
            }
        }
    }

    @Test void invalidStackAndRecipientInputsAreRejectedBeforeArithmetic() {
        for (long stacks : new long[] {Long.MIN_VALUE, -1, 0}) {
            assertThrows(IllegalArgumentException.class,
                    () -> IssuanceBudget.requireBudget(stacks, Integer.MAX_VALUE));
        }
        for (int recipients : new int[] {Integer.MIN_VALUE, -1, 0}) {
            assertThrows(IllegalArgumentException.class,
                    () -> IssuanceBudget.requireBudget(Long.MAX_VALUE, recipients));
        }
    }

    @Test void increasingRecipientCountNeverTurnsRejectedGiveIntoAcceptedGive() {
        long[] stackCounts = {1, 2, 512, 1024, 1025, Long.MAX_VALUE};
        for (long stacksPerRecipient : stackCounts) {
            boolean rejected = false;
            for (int recipients : new int[] {1, 2, 3, 512, 1024, 1025, Integer.MAX_VALUE}) {
                boolean accepted;
                try {
                    IssuanceBudget.requireBudget(stacksPerRecipient, recipients);
                    accepted = true;
                } catch (IllegalArgumentException expected) {
                    accepted = false;
                }
                assertFalse(rejected && accepted,
                        "budget acceptance must be monotonic: stacks="
                                + stacksPerRecipient + ", recipients=" + recipients);
                rejected |= !accepted;
            }
        }
    }

    @Test void coinStackConversionIsMonotonicAcrossEverySixtyFourBoundary() {
        for (long boundary : new long[] {64, 128, 1024, 4096, Long.MAX_VALUE - 63}) {
            long before = IssuanceBudget.coinStacks(boundary - 1);
            long at = IssuanceBudget.coinStacks(boundary);
            long after = IssuanceBudget.coinStacks(boundary + 1);
            assertTrue(before <= at && at <= after, "boundary=" + boundary);
            assertTrue(at - before <= 1 && after - at <= 1,
                    "one additional coin cannot consume more than one stack");
        }
    }
}
