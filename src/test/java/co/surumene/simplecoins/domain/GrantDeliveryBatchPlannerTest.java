package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GrantDeliveryBatchPlannerTest {
    private static GrantLedger.Grant amount(long value) {
        return new GrantLedger.Grant(1, UUID.randomUUID(), UUID.randomUUID(), value,
                "console", 12345, false, null, 0);
    }

    @Test void partial5200ResumesAtExactlySavedDenominationBoundary() {
        var grant = amount(5200);
        var first = GrantDeliveryBatchPlanner.next(grant, 0, 1);
        assertEquals(List.of(new GrantDistributionPlan.CoinStack(
                CoinDenomination.FIVE_THOUSAND, 1)), first.stacks());
        assertEquals(5000, first.nextValue());
        assertFalse(first.complete());
        var second = GrantDeliveryBatchPlanner.next(grant, first.nextValue(), 1);
        assertEquals(List.of(new GrantDistributionPlan.CoinStack(
                CoinDenomination.HUNDRED, 2)), second.stacks());
        assertEquals(5200, second.nextValue());
        assertTrue(second.complete());
        assertTrue(GrantDeliveryBatchPlanner.next(grant, 5200, 1).stacks().isEmpty());
    }

    @Test void explicit129CoinGrantNeverChangesTheChosenDenomination() {
        var grant = new GrantLedger.Grant(5, UUID.randomUUID(), UUID.randomUUID(), 645,
                "console", 12345, false, CoinDenomination.FIVE, 129);
        long progress = 0;
        for (int count : new int[]{64, 64, 1}) {
            var batch = GrantDeliveryBatchPlanner.next(grant, progress, 1);
            assertEquals(List.of(new GrantDistributionPlan.CoinStack(
                    CoinDenomination.FIVE, count)), batch.stacks());
            progress = batch.nextValue();
        }
        assertEquals(645, progress);
        assertTrue(GrantDeliveryBatchPlanner.next(grant, progress, 0).complete());
    }

    @Test void fullInventoryDoesNotEraseOrAdvanceTheReservation() {
        var grant = amount(5200);
        var before = GrantDeliveryBatchPlanner.next(grant, 0, 0);
        assertTrue(before.stacks().isEmpty());
        assertEquals(0, before.nextValue());
        assertFalse(before.complete());
        var afterFirstStack = GrantDeliveryBatchPlanner.next(grant, 5000, 0);
        assertEquals(5000, afterFirstStack.nextValue());
        assertTrue(afterFirstStack.stacks().isEmpty());
        assertFalse(afterFirstStack.complete());
    }

    @Test void arbitraryOrOutOfRangeSavedPartialAmountsAreRejected() {
        var grant = amount(5200);
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(grant, 100, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(grant, 5001, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(grant, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(grant, 5201, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(grant, 0, -1));
        assertThrows(NullPointerException.class,
                () -> GrantDeliveryBatchPlanner.next(null, 0, 1));
    }

    @Test void acknowledgedGrantIsNeverReIssuedAndOversizedPlanFailsSafe() {
        var grant = amount(10);
        var complete = new GrantLedger.Grant(grant.sequence(), grant.id(), grant.player(),
                grant.amount(), grant.issuer(), grant.createdMillis(), true, null, 0);
        assertThrows(IllegalStateException.class,
                () -> GrantDeliveryBatchPlanner.next(complete, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> GrantDeliveryBatchPlanner.next(amount(655360001), 0, 2));
    }
}
