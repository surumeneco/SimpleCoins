package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: in-memory grant state is not valid after losing durable root. */
class GrantLedgerRootLifecycleTest {
    @TempDir Path directory;
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test void removedJournalRootBlocksPendingDeliveryAndRemainsFailed() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        Files.delete(root.resolve("head"));
        Files.delete(root);
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
        Files.createDirectories(root);
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
    }

    @Test void replacedJournalRootBlocksReceiptChecks() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        Files.delete(root.resolve("head"));
        Files.delete(root);
        Files.writeString(root, "not a journal directory");
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(PLAYER, 0));
    }

    @Test void missingJournalHeadBlocksPendingDelivery() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        Files.delete(root.resolve("head"));
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
    }

    @Test void samePathDirectoryReplacementWithCopiedHeadCannotReuseLiveLedger() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        ledger.reserve(PLAYER, 100, "admin");
        Path original = directory.resolve("original-journal");
        Files.move(root, original);
        Files.createDirectory(root);
        Files.copy(original.resolve("head"), root.resolve("head"));
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
        assertThrows(IllegalStateException.class, () -> ledger.reserve(PLAYER, 100, "admin"));
        assertFalse(Files.exists(root.resolve("2.grant")));
        assertEquals(1, new GrantLedger(original).pending(PLAYER).size());
    }

    @Test void intactJournalStillPermitsEmptyPendingAndZeroReceipt() throws Exception {
        var ledger = new GrantLedger(directory.resolve("healthy"));
        assertTrue(ledger.pending(PLAYER).isEmpty());
        assertDoesNotThrow(() -> ledger.validateReceipt(PLAYER, 0));
    }
}
