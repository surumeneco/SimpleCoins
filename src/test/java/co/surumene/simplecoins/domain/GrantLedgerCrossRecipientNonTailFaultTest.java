package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Draft expected-Red characterization: global durable journal corruption cannot be
 * ignored merely because the caller belongs to another recipient.
 * Not a Confirmed CAT requirement.
 */
class GrantLedgerCrossRecipientNonTailFaultTest {
    @TempDir Path temporary;

    @Test void missingOlderOtherPlayerRecordMustBlockPending() throws Exception {
        Path root = temporary.resolve("missing-other-player");
        var ledger = new GrantLedger(root);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        ledger.reserve(alice, 100, "console");
        ledger.reserve(bob, 200, "console");

        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.pending(bob),
                "A different recipient's intact tail cannot validate a broken global journal");
    }

    @Test void corruptOlderOtherPlayerRecordMustBlockReceiptValidation() throws Exception {
        Path root = temporary.resolve("corrupt-other-player");
        var ledger = new GrantLedger(root);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        ledger.reserve(alice, 100, "console");
        ledger.reserve(bob, 200, "console");

        Files.write(root.resolve("1.grant"), new byte[] {0, 1, 2});
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(bob, 0),
                "A recipient-specific receipt must not bypass global journal corruption");
    }

    @Test void intactGlobalJournalRetainsRecipientIsolation() throws Exception {
        Path root = temporary.resolve("intact");
        var ledger = new GrantLedger(root);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        ledger.reserve(alice, 100, "console");
        ledger.reserve(bob, 200, "console");

        assertEquals(1, ledger.pending(alice).size());
        assertEquals(1, ledger.pending(bob).size());
        assertDoesNotThrow(() -> ledger.validateReceipt(alice, 0));
        assertDoesNotThrow(() -> ledger.validateReceipt(bob, 0));
    }
}
