package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005, unconfirmed CAT Draft SC-JOURNAL-UNTRACKED-FUTURE-01.
 * Expected Red characterization; do not treat as approved production requirement.
 */
class GrantLedgerUntrackedFutureRecordTest {
    @TempDir Path temporaryDirectory;

    @Test void untrackedFutureGrantMustInvalidateLivePendingSnapshot() throws Exception {
        Path root = temporaryDirectory.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000081");
        var tracked = ledger.reserve(player, 100, "console");

        Path trackedRecord = root.resolve(tracked.sequence() + ".grant");
        Path untrackedFutureRecord = root.resolve((tracked.sequence() + 2) + ".grant");
        Files.copy(trackedRecord, untrackedFutureRecord);
        assertTrue(Files.exists(untrackedFutureRecord));

        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "An untracked future .grant must not be silently omitted from a live journal view");
        assertTrue(Files.exists(untrackedFutureRecord),
                "Fail-closed detection must preserve unexpected durable evidence");
    }
}
