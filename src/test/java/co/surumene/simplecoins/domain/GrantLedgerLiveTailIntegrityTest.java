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
        // SC-JOURNAL-ACK-03 positive baseline: a non-tail durable target can
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

    @Test void missingNonTailTargetMustNotBeRecreatedByAcknowledgement() throws Exception {
        // SC-JOURNAL-ACK-01: the current durable tail cannot prove an older
        // reservation is still present. Fail closed without repairing it.
        Path root = directory.resolve("missing-older-target");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000050");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Path olderFile = root.resolve(older.sequence() + ".grant");
        Files.delete(olderFile);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()));
        assertFalse(Files.exists(olderFile),
                "Acknowledgement must never recreate a missing durable target");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A missing target must poison the live journal until recovery");
    }

    @Test void corruptNonTailTargetMustNotBeOverwrittenByAcknowledgement() throws Exception {
        // SC-JOURNAL-ACK-02: even a valid head and tail do not authorize
        // overwriting a corrupt older record with in-memory state.
        Path root = directory.resolve("corrupt-older-target");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000051");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Path olderFile = root.resolve(older.sequence() + ".grant");
        byte[] damaged = new byte[]{1, 2, 3, 4};
        Files.write(olderFile, damaged);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()));
        assertArrayEquals(damaged, Files.readAllBytes(olderFile),
                "Acknowledgement must preserve corrupt evidence for recovery");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A corrupt target must poison the live journal until recovery");
    }

    @Test void substitutedNonTailTargetMustNotBeSilentlyAcknowledged() throws Exception {
        // SC-JOURNAL-ACK-02 substitution variant: a well-formed record from a
        // different ledger is not durable evidence for this reservation.
        Path root = directory.resolve("substituted-target");
        var ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000052");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Path otherRoot = directory.resolve("independent-ledger");
        var unrelated = new GrantLedger(otherRoot);
        unrelated.reserve(UUID.fromString("00000000-0000-0000-0000-000000000053"),
                300, "console");
        Path olderFile = root.resolve(older.sequence() + ".grant");
        byte[] substituted = Files.readAllBytes(otherRoot.resolve("1.grant"));
        Files.write(olderFile, substituted);

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(older, older.sequence()));
        assertArrayEquals(substituted, Files.readAllBytes(olderFile),
                "Acknowledgement must preserve the substituted durable evidence");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A substituted non-tail record must poison the live journal");
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
