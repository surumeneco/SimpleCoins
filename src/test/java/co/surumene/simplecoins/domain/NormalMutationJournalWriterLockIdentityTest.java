package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Guards the active writer lock against replacement of its on-disk identity. */
class NormalMutationJournalWriterLockIdentityTest {
    @TempDir Path directory;

    private static NormalMutationJournal.Account source() {
        return NormalMutationJournal.Account.external();
    }
    private static NormalMutationJournal.Account destination() {
        return NormalMutationJournal.Account.free();
    }

    @Test void replacingLockedFileWithAnotherInodeMustRevokeWriter() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.prepare(UUID.randomUUID(), source(), destination(), 1);
            Path lock = directory.resolve(".writer.lock");
            Files.move(lock, directory.resolve(".writer.lock.detached"));
            Files.createFile(lock);

            assertThrows(IllegalStateException.class, journal::checkHealthy);
            assertThrows(IllegalStateException.class,
                    () -> journal.prepare(UUID.randomUUID(), source(), destination(), 2));
            assertFalse(Files.exists(directory.resolve("2.mutation")));
        }
    }

    @Test void removedWriterLockMustNotAllowMoreMutations() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.prepare(UUID.randomUUID(), source(), destination(), 1);
            Files.delete(directory.resolve(".writer.lock"));

            assertThrows(IllegalStateException.class, journal::checkHealthy);
            assertThrows(IllegalStateException.class,
                    () -> journal.prepare(UUID.randomUUID(), source(), destination(), 2));
            assertFalse(Files.exists(directory.resolve("2.mutation")));
        }
    }

    @Test void symlinkedReplacementOfWriterLockMustBeRejected() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.prepare(UUID.randomUUID(), source(), destination(), 1);
            Path lock = directory.resolve(".writer.lock");
            Path detached = directory.resolve(".writer.lock.detached");
            Files.move(lock, detached);
            Files.createSymbolicLink(lock, detached);

            assertThrows(IllegalStateException.class, journal::checkHealthy);
            assertFalse(Files.exists(directory.resolve("2.mutation")));
        }
    }

    @Test void unchangedWriterLockContinuesToPermitHealthyOperations() throws Exception {
        try (var journal = new NormalMutationJournal(directory)) {
            journal.checkHealthy();
            journal.prepare(UUID.randomUUID(), source(), destination(), 1);
            journal.checkHealthy();
            journal.verifyAll();
            assertEquals(1, journal.unresolved().size());
        }
    }
}
