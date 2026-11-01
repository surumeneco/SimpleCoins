package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: monetary journal root must be a real directory. */
class GrantLedgerRootIntegrityTest {
    @TempDir Path directory;

    @Test void symlinkedJournalRootCannotRedirectGrantReplayOrWrites() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real"));
        Path alias = Files.createSymbolicLink(directory.resolve("alias"), real.toAbsolutePath());
        assertThrows(IOException.class, () -> new GrantLedger(alias));
        assertFalse(Files.exists(real.resolve("head")),
                "Reject the alias before creating monetary journal state at its target");
    }

    @Test void ordinaryJournalDirectoryStillReplaysReservations() throws Exception {
        Path real = directory.resolve("real");
        UUID player = UUID.randomUUID();
        var first = new GrantLedger(real);
        var grant = first.reserve(player, 100, "console");
        assertEquals(grant, new GrantLedger(real).pending(player).getFirst());
    }
}
