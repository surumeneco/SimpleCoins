package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: Draft Red for receipt verification of non-tail durable
 * grant records. A healthy tail alone is not evidence for an older record.
 */
class GrantLedgerWasAppliedNonTailIntegrityTest {
    @TempDir Path directory;

    @Test void missingOlderDurableGrantCannotBeReportedAsApplied() throws Exception {
        Path root = directory.resolve("missing-older");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000061");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Files.delete(root.resolve(older.sequence() + ".grant"));

        assertThrows(IllegalStateException.class,
                () -> ledger.wasApplied(older, older.sequence()));
    }

    @Test void corruptedOlderDurableGrantCannotBeReportedAsApplied() throws Exception {
        Path root = directory.resolve("corrupt-older");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000062");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Files.write(root.resolve(older.sequence() + ".grant"), new byte[]{1, 2, 3});

        assertThrows(IllegalStateException.class,
                () -> ledger.wasApplied(older, older.sequence()));
    }

    @Test void intactOlderDurableGrantStillSupportsReceiptCheck() throws Exception {
        var ledger = new GrantLedger(directory.resolve("intact-older"));
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000063");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        assertTrue(ledger.wasApplied(older, older.sequence()));
    }
}
