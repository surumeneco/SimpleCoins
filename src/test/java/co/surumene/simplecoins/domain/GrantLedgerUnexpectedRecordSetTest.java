package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Untracked durable reservation files invalidate an already running ledger. */
class GrantLedgerUnexpectedRecordSetTest {
    @TempDir Path temporary;

    @Test void unexpectedFutureReservationMustInvalidatePendingAndNewIssuance() throws Exception {
        Path root = temporary.resolve("future");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");

        byte[] original = Files.readAllBytes(root.resolve("1.grant"));
        Path unexpected = root.resolve("99.grant");
        Files.write(unexpected, original);
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertThrows(IllegalStateException.class, () -> ledger.reserve(player, 200, "console"));
        assertFalse(Files.exists(root.resolve("2.grant")));
        assertArrayEquals(original, Files.readAllBytes(unexpected));
        assertThrows(java.io.IOException.class, () -> new GrantLedger(root));
        assertEquals(1L, first.sequence());
    }

    @Test void unrecognizedGrantFilenameIsNotIgnoredDuringLiveOperations() throws Exception {
        Path root = temporary.resolve("unrecognized");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 10, "console");

        Path unexpected = root.resolve("unexpected.grant");
        Files.copy(root.resolve("1.grant"), unexpected);
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertThrows(IllegalStateException.class, () -> ledger.reserve(player, 20, "console"));
        assertFalse(Files.exists(root.resolve("2.grant")));
        assertTrue(Files.exists(unexpected));
    }

    @Test void exactKnownRecordSetAllowsOrdinaryPendingAndSequentialIssuance() throws Exception {
        Path root = temporary.resolve("healthy");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 10, "console");
        var second = ledger.reserve(player, 20, "console");

        assertEquals(List.of(first, second), ledger.pending(player));
        assertEquals(List.of(first, second), new GrantLedger(root).pending(player));
    }
}
