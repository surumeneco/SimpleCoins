package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: never overwrite a grant unknown to the live ledger. */
class GrantLedgerUntrackedNextRecordTest {
    @TempDir Path directory;

    @Test void unexpectedNextRecordMustRemainUntouched() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        byte[] unexpected = new byte[]{11, 22, 33};
        Files.write(root.resolve("1.grant"), unexpected);
        UUID player = UUID.randomUUID();
        assertThrows(IOException.class, () -> ledger.reserve(player, 100, "console"));
        assertArrayEquals(unexpected, Files.readAllBytes(root.resolve("1.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
    }

    @Test void danglingNextRecordLinkMustNotBeReplaced() throws Exception {
        Path root = directory.resolve("journal");
        var ledger = new GrantLedger(root);
        Path link = Files.createSymbolicLink(root.resolve("1.grant"), root.resolve("missing"));
        assertThrows(IOException.class,
                () -> ledger.reserve(UUID.randomUUID(), 100, "console"));
        assertTrue(Files.isSymbolicLink(link));
    }

    @Test void absentNextRecordAllowsNormalReservation() throws Exception {
        var ledger = new GrantLedger(directory.resolve("journal"));
        UUID player = UUID.randomUUID();
        assertEquals(1L, ledger.reserve(player, 100, "console").sequence());
        assertEquals(1, ledger.pending(player).size());
    }
}
