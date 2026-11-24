package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** A saved same-player receipt is the cumulative floor, not a second full inventory WAL. */
final class GrantSavedProgressTest {
    @TempDir Path directory;
    private static void confirm(GrantLedger ledger, GrantLedger.Grant grant, GrantReceiptState saved)
            throws Exception {
        try {
            GrantLedger.class.getMethod("confirmSavedProgress", GrantLedger.Grant.class,
                    GrantReceiptState.class).invoke(ledger, grant, saved);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        }
    }

    @Test void partialAndCompleteReceiptSurviveLedgerReopenWithoutBeforeImage() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(directory);
        var grant = ledger.reserveCoins(player, CoinDenomination.ONE, 65, "test");
        var start = GrantReceiptState.empty().begin(ledger, player, grant);
        var first = start.advance(ledger, player, grant, GrantDeliveryBatchPlanner.next(grant, 0, 1));
        confirm(ledger, grant, first);
        var recovered = new GrantLedger(directory);
        var remaining = recovered.pending(player).getFirst();
        assertEquals(64, remaining.confirmedPartial());
        assertEquals(65, remaining.amount());
        confirm(recovered, remaining, first);
        assertEquals(64, recovered.pending(player).getFirst().confirmedPartial());
        assertThrows(IllegalStateException.class, () -> confirm(recovered, remaining,
                GrantReceiptState.empty()));
        var last = first.advance(recovered, player, remaining,
                GrantDeliveryBatchPlanner.next(remaining, 64, 1)).complete(recovered, player, remaining);
        confirm(recovered, remaining, last);
        var again = new GrantLedger(directory);
        assertTrue(again.pending(player).isEmpty());
        assertTrue(again.historySnapshot().getFirst().delivered());
    }
}
