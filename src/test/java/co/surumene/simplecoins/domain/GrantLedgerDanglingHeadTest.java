package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: a dangling head link is invalid, not absent. */
class GrantLedgerDanglingHeadTest {
    @TempDir Path directory;

    @Test void danglingHeadCannotBeReplacedByFreshZeroHead() throws Exception {
        Path root = Files.createDirectory(directory.resolve("journal"));
        Path head = Files.createSymbolicLink(root.resolve("head"), root.resolve("missing"));
        assertThrows(IOException.class, () -> new GrantLedger(root));
        assertTrue(Files.isSymbolicLink(head));
    }

    @Test void genuinelyAbsentHeadCanInitializeEmptyJournal() throws Exception {
        Path root = directory.resolve("healthy");
        var ledger = new GrantLedger(root);
        assertTrue(ledger.pending(java.util.UUID.randomUUID()).isEmpty());
        assertTrue(Files.isRegularFile(root.resolve("head")));
    }
}
