package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: a missing recipient must never look like no pending grants. */
class GrantLedgerPlayerIdentityTest {
    @TempDir Path directory;

    @Test void missingPlayerIdentityCannotYieldAnEmptyPendingQueue() throws Exception {
        var ledger = new GrantLedger(directory.resolve("grants"));
        ledger.reserve(UUID.randomUUID(), 100, "console");
        assertThrows(IllegalArgumentException.class, () -> ledger.pending(null));
    }

    @Test void missingPlayerIdentityCannotValidateZeroReceipt() throws Exception {
        var ledger = new GrantLedger(directory.resolve("grants"));
        ledger.reserve(UUID.randomUUID(), 100, "console");
        assertThrows(IllegalArgumentException.class, () -> ledger.validateReceipt(null, 0));
    }

    @Test void knownPlayerStillReplaysAndAcknowledgesOnce() throws Exception {
        UUID player = UUID.randomUUID();
        Path journal = directory.resolve("grants");
        var ledger = new GrantLedger(journal);
        var grant = ledger.reserve(player, 100, "console");
        var recovered = new GrantLedger(journal);
        assertEquals(grant, recovered.pending(player).getFirst());
        recovered.acknowledge(grant, grant.sequence());
        recovered.acknowledge(grant, grant.sequence());
        assertTrue(new GrantLedger(journal).pending(player).isEmpty());
    }
}
