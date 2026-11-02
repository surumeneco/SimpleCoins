package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005 / GRANT-PENDING-01..04.
 * Draft-only fault injection for the pending() durable read boundary.
 */
class GrantLedgerPendingNonTailEvidenceTest {
    @TempDir Path directory;
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000075");

    private GrantLedger twoReservations() throws Exception {
        GrantLedger ledger = new GrantLedger(directory.resolve("journal"));
        ledger.reserve(PLAYER, 1, "fixture");
        ledger.reserve(PLAYER, 2, "fixture");
        return ledger;
    }

    @Test void missingNonTailRecordMustNotYieldPendingList() throws Exception {
        GrantLedger ledger = twoReservations();
        Files.delete(directory.resolve("journal/1.grant"));
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
    }

    @Test void corruptNonTailRecordMustNotYieldPendingList() throws Exception {
        GrantLedger ledger = twoReservations();
        Files.writeString(directory.resolve("journal/1.grant"), "corrupted");
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
    }

    @Test void intactJournalReturnsBothPendingGrants() throws Exception {
        GrantLedger ledger = twoReservations();
        assertEquals(2, ledger.pending(PLAYER).size());
    }

    @Test void missingTailAlreadyFailsClosed() throws Exception {
        GrantLedger ledger = twoReservations();
        Files.delete(directory.resolve("journal/2.grant"));
        assertThrows(IllegalStateException.class, () -> ledger.pending(PLAYER));
    }
}
