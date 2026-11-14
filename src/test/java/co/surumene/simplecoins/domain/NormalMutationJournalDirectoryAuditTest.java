package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NormalMutationJournalDirectoryAuditTest {
    @TempDir Path temp;

    private static void reserve(NormalMutationJournal journal) throws Exception {
        journal.prepare(UUID.randomUUID(), NormalMutationJournal.Account.external(),
                NormalMutationJournal.Account.free(), 5);
    }

    private static int scanOnePass(NormalMutationJournal journal) {
        int entries = 0;
        while (journal.auditNextDirectoryEntry()) {
            entries++;
            if (entries > 100) fail("Directory audit cursor never exhausted");
        }
        return entries;
    }

    @Test void emptySourceSetAndKnownRecordsAreCheckedWithoutScanningHead() throws Exception {
        try (var journal = new NormalMutationJournal(temp.resolve("normal"))) {
            assertEquals(0, scanOnePass(journal));
            reserve(journal);
            reserve(journal);
            assertEquals(2, scanOnePass(journal));
            assertEquals(2, scanOnePass(journal));
        }
    }

    @Test void unknownExtraValidLookingRecordIsNeverAssumedToBeCommitted() throws Exception {
        Path dir = temp.resolve("rogue");
        try (var journal = new NormalMutationJournal(dir)) {
            reserve(journal);
            assertEquals(1, scanOnePass(journal));
            Files.copy(dir.resolve("1.mutation"), dir.resolve("2.mutation"));
            assertThrows(IllegalStateException.class, () -> scanOnePass(journal));
            assertThrows(IllegalStateException.class, () -> reserve(journal));
        }
    }

    @Test void nonCanonicalSequenceNamesAndMalformedFileNamesAreFatal() throws Exception {
        for (String suffix : new String[]{"0001.mutation", "bogus.mutation", "0.mutation"}) {
            Path dir = temp.resolve(suffix.replace(".", "_"));
            try (var journal = new NormalMutationJournal(dir)) {
                reserve(journal);
                Files.copy(dir.resolve("1.mutation"), dir.resolve(suffix));
                assertThrows(IllegalStateException.class,
                        () -> scanOnePass(journal), suffix);
            }
        }
    }

    @Test void subsequentLegitimateReservationsDoNotLookLikeExternalRecords() throws Exception {
        try (var journal = new NormalMutationJournal(temp.resolve("legit"))) {
            reserve(journal);
            assertEquals(1, scanOnePass(journal));
            reserve(journal);
            assertEquals(2, scanOnePass(journal));
        }
    }

    @Test void closedJournalMustNotReopenDirectoryCursor() throws Exception {
        var journal = new NormalMutationJournal(temp.resolve("closed"));
        reserve(journal);
        assertTrue(journal.auditNextDirectoryEntry());
        journal.close();
        assertThrows(IllegalStateException.class, journal::auditNextDirectoryEntry);
    }
}
