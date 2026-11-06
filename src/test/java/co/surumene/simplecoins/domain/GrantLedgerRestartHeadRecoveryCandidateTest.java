package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: code-to-spec Candidate for restart-time journal-head
 * recovery. This test does not confer CAT specification confirmation.
 */
class GrantLedgerRestartHeadRecoveryCandidateTest {
    @TempDir Path temporary;

    @Test void persistedReservationAheadOfHeadIsRecoveredWithoutDuplicateIssuance() throws Exception {
        Path root = temporary.resolve("interrupted-head");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        var second = ledger.reserve(player, 200, "console");

        // Model a crash after the second durable reservation but before head update.
        Files.write(root.resolve("head"), ByteBuffer.allocate(8).putLong(1).array());
        var recovered = new GrantLedger(root);
        assertEquals(List.of(first, second), recovered.pending(player));
        assertEquals(2, ByteBuffer.wrap(Files.readAllBytes(root.resolve("head"))).getLong());
        assertEquals(3, recovered.reserve(player, 300, "console").sequence());
        assertEquals(3, new GrantLedger(root).pending(player).size());
    }

    @Test void headAheadOfDurableRecordsMustNotBeSilentlyRepaired() throws Exception {
        Path root = temporary.resolve("missing-record");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        byte[] inconsistentHead = ByteBuffer.allocate(8).putLong(2).array();
        Files.write(root.resolve("head"), inconsistentHead);

        assertThrows(IOException.class, () -> new GrantLedger(root));
        assertArrayEquals(inconsistentHead, Files.readAllBytes(root.resolve("head")),
                "A rejected recovery must preserve the inconsistent evidence");
        assertFalse(Files.exists(root.resolve("2.grant")));
    }
}
