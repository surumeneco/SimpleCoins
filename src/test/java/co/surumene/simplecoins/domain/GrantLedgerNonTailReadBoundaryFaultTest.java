package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec Draft fault injection: receipt and pending read boundaries must not
 * trust the in-memory journal when an older durable reservation disappears.
 * These are expected-Red regression tests until the normative CAT Gate permits a fix.
 */
class GrantLedgerNonTailReadBoundaryFaultTest {
    @TempDir Path temporary;

    @Test void pendingMustFailClosedAfterOlderRecordIsDeleted() throws Exception {
        Path root = temporary.resolve("pending-deleted");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "Pending assets must not be reconstructed from memory after durable evidence disappears");
    }

    @Test void validateReceiptMustFailClosedAfterOlderRecordIsCorrupted() throws Exception {
        Path root = temporary.resolve("receipt-corrupt");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.write(root.resolve("1.grant"), new byte[] {0, 1, 2});
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(player, 0),
                "A receipt decision must reject a damaged older journal record");
    }

    @Test void intactJournalAllowsPendingAndReceiptValidation() throws Exception {
        Path root = temporary.resolve("intact");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        assertEquals(2, ledger.pending(player).size());
        assertDoesNotThrow(() -> ledger.validateReceipt(player, 0));
    }
}
