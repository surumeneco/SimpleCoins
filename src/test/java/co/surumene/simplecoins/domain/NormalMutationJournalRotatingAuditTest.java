package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Bounded audit: no scan may silently trust a journal with changed durable history. */
class NormalMutationJournalRotatingAuditTest {
    @TempDir Path temp;

    private static NormalMutationJournal.Preparation issue(NormalMutationJournal journal, UUID id)
            throws Exception {
        return journal.prepare(id, NormalMutationJournal.Account.external(),
                NormalMutationJournal.Account.free(), 1L);
    }

    @Test void walksEveryExistingRecordAndWrapsWithoutScanningAnEmptyJournal() throws Exception {
        try (var journal = new NormalMutationJournal(temp.resolve("empty"))) {
            assertFalse(journal.auditNextRecord());
            var first = issue(journal, UUID.randomUUID());
            var second = issue(journal, UUID.randomUUID());
            assertTrue(journal.auditNextRecord());
            assertTrue(journal.auditNextRecord());
            assertTrue(journal.auditNextRecord());
            assertEquals(1, first.mutation().sequence());
            assertEquals(2, second.mutation().sequence());
        }
    }

    @Test void inMemoryLegitimatePhaseAndNewTailAreReadFromCurrentJournalState() throws Exception {
        try (var journal = new NormalMutationJournal(temp.resolve("mutable"))) {
            var operation = UUID.randomUUID();
            issue(journal, operation);
            assertTrue(journal.auditNextRecord());
            journal.markEventApplied(operation);
            // No stale record snapshot from the prior audit is allowed.
            assertTrue(journal.auditNextRecord());
            issue(journal, UUID.randomUUID());
            assertTrue(journal.auditNextRecord());
            assertTrue(journal.auditNextRecord());
        }
    }

    @Test void validButOlderRecordBytesAreDetectedAndJournalIsPoisoned() throws Exception {
        Path dir = temp.resolve("record-tamper");
        try (var journal = new NormalMutationJournal(dir)) {
            UUID operation = UUID.randomUUID();
            issue(journal, operation);
            byte[] originalValidBytes = Files.readAllBytes(dir.resolve("1.mutation"));
            journal.markEventApplied(operation);
            Files.write(dir.resolve("1.mutation"), originalValidBytes);
            var failure = assertThrows(IllegalStateException.class, journal::auditNextRecord);
            assertTrue(failure.getMessage().contains("integrity"));
            assertThrows(IllegalStateException.class,
                    () -> issue(journal, UUID.randomUUID()), "audit failure must reject further writes");
        }
    }

    @Test void missingNonTailRecordCannotPassJustBecauseHeadAndLockAreValid() throws Exception {
        Path dir = temp.resolve("missing");
        try (var journal = new NormalMutationJournal(dir)) {
            issue(journal, UUID.randomUUID());
            issue(journal, UUID.randomUUID());
            assertTrue(journal.auditNextRecord());
            Files.delete(dir.resolve("2.mutation"));
            assertThrows(IllegalStateException.class, journal::auditNextRecord);
            assertThrows(IllegalStateException.class, journal::auditNextRecord);
        }
    }
}
