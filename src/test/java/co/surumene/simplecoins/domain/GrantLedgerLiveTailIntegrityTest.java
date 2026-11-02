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

    @Test void intactOlderGrantAcknowledgementPreservesNewerPendingAfterRestart() throws Exception {
        // SC-JOURNAL-ACK-06 positive baseline: a non-tail durable target can
        // be acknowledged without acknowledging the newer reservation.
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000049");
        var first = ledger.reserve(player, 100, "console");
        var second = ledger.reserve(player, 200, "console");
        assertEquals(java.util.List.of(first, second), ledger.pending(player));

        ledger.acknowledge(first, first.sequence());
        assertEquals(java.util.List.of(second), ledger.pending(player));
        var recovered = new GrantLedger(root);
        assertEquals(java.util.List.of(second), recovered.pending(player));
        assertTrue(recovered.wasApplied(first, first.sequence()));
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
