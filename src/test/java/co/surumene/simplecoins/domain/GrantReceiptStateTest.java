package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrantReceiptStateTest {
    @TempDir Path temp;

    @Test void resumeAndFinishRequireCanonicalPersistedStackPrefix() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("ledger"));
        var grant = ledger.reserve(player, 5200, "console");
        var start = GrantReceiptState.empty().begin(ledger, player, grant);
        start.verify(ledger, player);
        var first = start.advance(ledger, player, grant, GrantDeliveryBatchPlanner.next(grant, 0, 1));
        assertEquals(5000, first.partialValue());
        first.verify(new GrantLedger(temp.resolve("ledger")), player);
        assertThrows(IllegalStateException.class, () -> first.complete(ledger, player, grant));
        var full = first.advance(ledger, player, grant, GrantDeliveryBatchPlanner.next(grant, 5000, 1));
        var receipt = full.complete(ledger, player, grant);
        assertEquals(grant.sequence(), receipt.completedSequence());
        assertFalse(receipt.active());
        receipt.verify(ledger, player);
        ledger.acknowledge(grant, receipt.completedSequence());
        receipt.verify(new GrantLedger(temp.resolve("ledger")), player);
    }

    @Test void rejectForeignOwnerForgedIdentityAndSkippedPendingReservation() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("forgery"));
        var first = ledger.reserve(player, 5200, "console");
        var second = ledger.reserve(player, 10, "console");
        assertThrows(IllegalStateException.class,
                () -> new GrantReceiptState(0, first.sequence(), first.id(), 100)
                        .verify(ledger, player));
        assertThrows(IllegalStateException.class,
                () -> new GrantReceiptState(0, first.sequence(), UUID.randomUUID(), 5000)
                        .verify(ledger, player));
        assertThrows(IllegalStateException.class,
                () -> new GrantReceiptState(0, second.sequence(), second.id(), 0)
                        .verify(ledger, player));
        assertThrows(IllegalStateException.class,
                () -> new GrantReceiptState(0, first.sequence(), first.id(), 0)
                        .verify(ledger, UUID.randomUUID()));
        assertThrows(IllegalStateException.class,
                () -> new GrantReceiptState(second.sequence(), 0, null, 0)
                        .verify(ledger, player));
    }

    @Test void forgedGrantAmountCannotAdvanceOrFinishReceipt() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("forged-handle"));
        var authentic = ledger.reserve(player, 5200, "console");
        var forged = new GrantLedger.Grant(authentic.sequence(), authentic.id(),
                player, 5000, authentic.issuer(), authentic.createdMillis(),
                false, CoinDenomination.FIVE_THOUSAND, 1);
        var state = GrantReceiptState.empty().begin(ledger, player, authentic);
        var foreignBatch = GrantDeliveryBatchPlanner.next(forged, 0, 1);
        assertThrows(IllegalStateException.class,
                () -> state.advance(ledger, player, forged, foreignBatch));
    }

    @Test void forgedBatchAndMalformedStateFailClosed() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("batch"));
        var grant = ledger.reserve(player, 5200, "console");
        var state = GrantReceiptState.empty().begin(ledger, player, grant);
        var canonical = GrantDeliveryBatchPlanner.next(grant, 0, 1);
        var forged = new GrantDeliveryBatchPlanner.Batch(canonical.stacks(), 0, 5200, true);
        assertThrows(IllegalStateException.class, () -> state.advance(ledger, player, grant, forged));
        assertThrows(IllegalStateException.class,
                () -> state.advance(ledger, player, grant, GrantDeliveryBatchPlanner.next(grant, 0, 0)));
        var next = state.advance(ledger, player, grant, canonical);
        assertThrows(IllegalStateException.class, () -> next.advance(ledger, player, grant, canonical));
        assertThrows(IllegalArgumentException.class, () -> new GrantReceiptState(-1, 0, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new GrantReceiptState(2, 2, UUID.randomUUID(), 0));
        assertThrows(IllegalArgumentException.class, () -> new GrantReceiptState(0, 0, UUID.randomUUID(), 0));
        assertThrows(IllegalArgumentException.class, () -> new GrantReceiptState(0, 0, null, 1));
    }
}
