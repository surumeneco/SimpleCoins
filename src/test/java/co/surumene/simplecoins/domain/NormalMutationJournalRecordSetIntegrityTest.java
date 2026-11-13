package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A full authoritative journal verification must close its physical file set,
 * not merely compare the already known in-memory entries.
 */
class NormalMutationJournalRecordSetIntegrityTest {
    @TempDir Path directory;

    private static final NormalMutationJournal.Account SUPPLY =
            NormalMutationJournal.Account.external();
    private static final NormalMutationJournal.Account FREE =
            NormalMutationJournal.Account.free();

    @Test void untrackedFutureMutationFileInvalidatesFullVerification() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.prepare(UUID.randomUUID(), SUPPLY, FREE, 50);
            byte[] existing = Files.readAllBytes(directory.resolve("1.mutation"));
            Path unexpected = directory.resolve("99.mutation");
            Files.write(unexpected, existing);

            assertThrows(IllegalStateException.class, journal::verifyAll);
            assertThrows(IllegalStateException.class, journal::checkHealthy,
                    "Failed full verification must poison this journal instance");
            assertArrayEquals(existing, Files.readAllBytes(unexpected),
                    "Failed verification must not overwrite unknown monetary evidence");
            assertFalse(Files.exists(directory.resolve("2.mutation")));
        }
    }

    @Test void misnamedAdditionalMutationFileInvalidatesFullVerification() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.prepare(UUID.randomUUID(), SUPPLY, FREE, 50);
            Path unexpected = directory.resolve("unexpected.mutation");
            Files.copy(directory.resolve("1.mutation"), unexpected);

            assertThrows(IllegalStateException.class, journal::verifyAll);
            assertThrows(IllegalStateException.class, journal::checkHealthy);
            assertTrue(Files.exists(unexpected));
        }
    }
}
