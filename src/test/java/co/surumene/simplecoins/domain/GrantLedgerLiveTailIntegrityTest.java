package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: a live journal cannot trust only its durable head. */
class GrantLedgerLiveTailIntegrityTest {
    @TempDir Path directory;

    @Test void deletedTailMustBlockPendingAndFurtherIssuance() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        Files.delete(root.resolve("1.grant"));
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertThrows(IllegalStateException.class,
                () -> ledger.reserve(player, 100, "console"));
    }

    @Test void corruptedTailMustBlockPendingAndFurtherIssuance() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        Files.write(root.resolve("1.grant"), new byte[]{1, 2, 3});
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertThrows(IllegalStateException.class,
                () -> ledger.reserve(player, 100, "console"));
    }

    @Test void intactTailSupportsReceiptAcknowledgement() throws Exception {
        var ledger = new GrantLedger(directory.resolve("journal"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 100, "console");
        assertEquals(grant, ledger.pending(player).getFirst());
        assertTrue(ledger.wasApplied(grant, grant.sequence()));
        ledger.acknowledge(grant, grant.sequence());
        assertTrue(ledger.pending(player).isEmpty());
    }
}
