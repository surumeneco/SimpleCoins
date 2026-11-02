package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate Red regression: a linked issuance directory must not redirect
 * the real Java GrantLedger writer outside its intended data directory.
 * This is distinct from the Python offline-verifier alias tests.
 */
class GrantLedgerDirectoryBoundaryTest {
    @TempDir Path root;

    @Test
    void ordinaryDirectoryKeepsJournalFilesInsideItsRoot() throws Exception {
        Path directory = root.resolve("issuance");
        GrantLedger ledger = new GrantLedger(directory);
        assertTrue(Files.isRegularFile(directory.resolve("head")));
        var grant = ledger.reserve(UUID.randomUUID(), 25, "console");
        assertTrue(Files.isRegularFile(directory.resolve(grant.sequence() + ".grant")));
        assertTrue(Files.isDirectory(directory));
        assertFalse(Files.isSymbolicLink(directory));
    }

    @Test
    void linkedIssuanceDirectoryMustNotWriteHeadOutsideRoot() throws Exception {
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path alias = root.resolve("issuance");
        try {
            Files.createSymbolicLink(alias, outside);
        } catch (UnsupportedOperationException | FileSystemException | SecurityException unavailable) {
            assumeTrue(false, "Filesystem cannot create a test symlink: " + unavailable);
            return;
        }
        assumeTrue(Files.isSymbolicLink(alias), "Symlink not supported");

        assertAll("Fail closed before writing outside the issuance directory",
                () -> assertThrows(IOException.class, () -> new GrantLedger(alias),
                        "Linked journal root must be rejected"),
                () -> assertFalse(Files.exists(outside.resolve("head")),
                        "Initialization must not write a head into the symlink target"));
    }
}
