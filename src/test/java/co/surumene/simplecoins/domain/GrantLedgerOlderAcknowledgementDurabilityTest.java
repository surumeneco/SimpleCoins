package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005, CAT Draft SC-JOURNAL-ACK-01/02/07.
 * Existing code-to-spec Draft fault-injection vectors reused for a correctness
 * prototype. Formal CAT confirmation, Gates, and runtime safety remain separate.
 */
class GrantLedgerOlderAcknowledgementDurabilityTest {
    @TempDir Path temporaryDirectory;

    @Test void missingOlderTargetMustFailClosedWithoutRecreatingRecord() throws Exception {
        Path root = temporaryDirectory.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000071");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Path target = root.resolve(older.sequence() + ".grant");
        Files.delete(target);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()));
        assertFalse(Files.exists(target), "Rejected acknowledgement must not recreate missing bytes");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A corrupt live ledger must remain unhealthy");
    }

    @Test void corruptOlderTargetMustFailClosedWithoutOverwritingEvidence() throws Exception {
        Path root = temporaryDirectory.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000072");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Path target = root.resolve(older.sequence() + ".grant");
        byte[] corrupt = new byte[] {1, 2, 3, 4, 5};
        Files.write(target, corrupt);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()));
        assertArrayEquals(corrupt, Files.readAllBytes(target),
                "Fail-closed acknowledgement must preserve corrupted durable evidence");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
    }

    @Test void repeatedAcknowledgementMustRevalidateDeliveredOlderRecord() throws Exception {
        Path root = temporaryDirectory.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000073");
        var older = ledger.reserve(player, 100, "console");
        var newer = ledger.reserve(player, 200, "console");
        ledger.acknowledge(older, older.sequence());
        Path target = root.resolve(older.sequence() + ".grant");
        Files.delete(target);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()),
                "Idempotent acknowledgement cannot trust only an in-memory delivered flag");
        assertFalse(Files.exists(target));
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertTrue(Files.exists(root.resolve(newer.sequence() + ".grant")),
                "The newer durable reservation must remain untouched");
    }
}
