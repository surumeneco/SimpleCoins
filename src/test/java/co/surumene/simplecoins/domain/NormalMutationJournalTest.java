package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NormalMutationJournalTest {
    @TempDir Path root;

    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000001001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000001002");
    private static final UUID TX_A = UUID.fromString("00000000-0000-0000-0000-000000002001");
    private static final UUID TX_B = UUID.fromString("00000000-0000-0000-0000-000000002002");

    @Test void emptyJournalHasNoPhantomIssuanceAndReopensWithStableHead() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            assertTrue(journal.unresolved().isEmpty());
            assertFalse(journal.hasUnreconciledFromStartup());
            journal.verifyAll();
        }
        try (var reopened = new NormalMutationJournal(root)) {
            assertTrue(reopened.unresolved().isEmpty());
            assertFalse(reopened.hasUnreconciledFromStartup());
        }
    }

    @Test void transferIsPreparedDurablyBeforeAnyPaperMutation() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            var prepared = journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.player(BOB), 100);
            assertTrue(prepared.newlyCreated());
            var movement = prepared.mutation();
            assertEquals(1, movement.sequence());
            assertEquals(NormalMutationJournal.Phase.PREPARED, movement.phase());
            assertEquals(0, movement.physicalSupplyDelta());
            assertEquals(movement, journal.unresolved().getFirst());
            journal.verifyAll();
            assertFalse(journal.hasUnreconciledFromStartup());
        }
        try (var reopened = new NormalMutationJournal(root)) {
            assertEquals(NormalMutationJournal.Phase.PREPARED, reopened.unresolved().getFirst().phase());
            assertTrue(reopened.hasUnreconciledFromStartup());
        }
    }

    @Test void issuanceAndDestructionHaveOppositeSupplyDeltas() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            var minted = journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.player(ALICE), 10000).mutation();
            var destroyed = journal.prepare(TX_B, NormalMutationJournal.Account.free(),
                    NormalMutationJournal.Account.external(), 35).mutation();
            assertEquals(10000, minted.physicalSupplyDelta());
            assertEquals(-35, destroyed.physicalSupplyDelta());
            assertEquals(2, destroyed.sequence());
            journal.verifyAll();
        }
    }

    @Test void duplicateEventIdIsIdempotentOnlyWithIdenticalOperation() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            var old = journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.free(), 123);
            assertTrue(old.newlyCreated());
            var repeated = journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.free(), 123);
            assertFalse(repeated.newlyCreated(), "Must not permit a repeated Paper mutation");
            assertEquals(old.mutation(), repeated.mutation());
            assertThrows(IllegalStateException.class,
                    () -> journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                            NormalMutationJournal.Account.free(), 124));
            assertEquals(1, journal.unresolved().size());
            assertEquals(1, journal.unresolved().getFirst().sequence());
        }
    }

    @Test void eventAppliedStillRequiresAuthoritativeReconciliationAfterRestart() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.dead(ALICE), 100);
            assertEquals(NormalMutationJournal.Phase.EVENT_APPLIED,
                    journal.markEventApplied(TX_A).phase());
            assertEquals(NormalMutationJournal.Phase.EVENT_APPLIED,
                    journal.markEventApplied(TX_A).phase());
            journal.verifyAll();
        }
        try (var reopened = new NormalMutationJournal(root)) {
            assertTrue(reopened.hasUnreconciledFromStartup());
            assertEquals(NormalMutationJournal.Phase.EVENT_APPLIED,
                    reopened.unresolved().getFirst().phase());
        }
    }

    @Test void unknownEventAndInvalidMonetaryAccountCannotBeCommitted() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            assertThrows(IllegalArgumentException.class, () -> journal.markEventApplied(TX_A));
            assertThrows(IllegalArgumentException.class, () -> journal.prepare(TX_A,
                    NormalMutationJournal.Account.external(), NormalMutationJournal.Account.external(), 1));
            assertThrows(IllegalArgumentException.class, () -> journal.prepare(TX_A,
                    NormalMutationJournal.Account.player(ALICE), NormalMutationJournal.Account.player(ALICE), 10));
            assertThrows(IllegalArgumentException.class, () -> journal.prepare(TX_A,
                    NormalMutationJournal.Account.player(ALICE), NormalMutationJournal.Account.free(), 0));
            assertThrows(IllegalArgumentException.class, () -> new NormalMutationJournal.Account(
                    NormalMutationJournal.AccountKind.FREE, ALICE));
            assertThrows(IllegalArgumentException.class, () -> new NormalMutationJournal.Account(
                    NormalMutationJournal.AccountKind.PLAYER, null));
            assertTrue(journal.unresolved().isEmpty());
        }
    }

    @Test void concurrentJournalWriterIsRejectedWithoutModifyingCurrentWriter() throws Exception {
        try (var first = new NormalMutationJournal(root)) {
            assertThrows(IOException.class, () -> new NormalMutationJournal(root));
            first.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 5);
            first.verifyAll();
        }
        try (var second = new NormalMutationJournal(root)) {
            assertEquals(1, second.unresolved().size());
        }
    }

    @Test void completedDurableRecordWithLaggingHeadIsRecoveredWithoutAssumingApplication() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 77);
        }
        DurableFile.write(root.resolve("head"), ByteBuffer.allocate(8).putLong(0).array());
        try (var reopened = new NormalMutationJournal(root)) {
            assertTrue(reopened.hasUnreconciledFromStartup());
            assertEquals(1, reopened.unresolved().size());
            assertEquals(NormalMutationJournal.Phase.PREPARED, reopened.unresolved().getFirst().phase());
            reopened.verifyAll();
        }
    }

    @Test void headAheadOfPhysicalRecordsIsRejectedInsteadOfTruncatingHistory() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 77);
        }
        DurableFile.write(root.resolve("head"), ByteBuffer.allocate(8).putLong(2).array());
        assertThrows(IOException.class, () -> new NormalMutationJournal(root));
    }

    @Test void corruptedRecordIsNotDroppedOrTreatedAsZeroAfterRestart() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 5);
        }
        byte[] data = Files.readAllBytes(root.resolve("1.mutation"));
        data[data.length - 1] ^= 0x20;
        Files.write(root.resolve("1.mutation"), data);
        assertThrows(IOException.class, () -> new NormalMutationJournal(root));
    }

    @Test void missingOlderEntryAndGapRejectRecovery() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 5);
            journal.prepare(TX_B, NormalMutationJournal.Account.free(),
                    NormalMutationJournal.Account.player(ALICE), 5);
        }
        Files.delete(root.resolve("1.mutation"));
        assertThrows(IOException.class, () -> new NormalMutationJournal(root));
    }

    @Test void misnamedRecordsFailWithoutImplicitRename() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 5);
        }
        Files.move(root.resolve("1.mutation"), root.resolve("0001.mutation"));
        assertThrows(IOException.class, () -> new NormalMutationJournal(root));
    }

    @Test void collisionAtNextRecordCannotOverwriteAnyPriorData() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            byte[] unexpected = {1, 2, 3, 4, 5};
            Path file = root.resolve("1.mutation");
            Files.write(file, unexpected);
            assertThrows(IOException.class, () -> journal.prepare(TX_A,
                    NormalMutationJournal.Account.external(), NormalMutationJournal.Account.free(), 1));
            assertArrayEquals(unexpected, Files.readAllBytes(file));
            assertThrows(IllegalStateException.class, journal::checkHealthy);
        }
    }

    @Test void verifyAllDetectsPreviouslyWrittenRecordMutationAndPermanentlyFails() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 1);
            byte[] contents = Files.readAllBytes(root.resolve("1.mutation"));
            contents[contents.length - 1] ^= 4;
            Files.write(root.resolve("1.mutation"), contents);
            assertThrows(IllegalStateException.class, journal::verifyAll);
            assertThrows(IllegalStateException.class, journal::checkHealthy);
        }
    }

    @Test void phaseTransitionMustNotEraseChecksumValidExternalRecordReplacement() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.free(), 100);
            Path record = root.resolve("1.mutation");
            byte[] payload = DurableFile.read(record, 512);
            // Change the lowest value byte, then recompute a VALID durable
            // checksum. The HEAD and writer lock remain untouched.
            payload[payload.length - 2] ^= 1;
            DurableFile.write(record, payload);
            byte[] changed = Files.readAllBytes(record);

            assertThrows(IOException.class, () -> journal.markEventApplied(TX_A));
            assertArrayEquals(changed, Files.readAllBytes(record),
                    "The phase transition must not overwrite discrepant evidence");
            assertThrows(IllegalStateException.class, journal::checkHealthy);
        }
    }

    @Test void duplicateOperationMustNotTrustChangedDurableRecord() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.free(), 100);
            Path record = root.resolve("1.mutation");
            byte[] payload = DurableFile.read(record, 512);
            payload[payload.length - 2] ^= 1;
            DurableFile.write(record, payload);
            byte[] changed = Files.readAllBytes(record);

            assertThrows(IOException.class, () -> journal.prepare(TX_A,
                    NormalMutationJournal.Account.player(ALICE),
                    NormalMutationJournal.Account.free(), 100));
            assertArrayEquals(changed, Files.readAllBytes(record));
            assertThrows(IllegalStateException.class, journal::checkHealthy);
        }
    }

    @Test void headTamperingRevokesLiveJournalHealth() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 1);
            DurableFile.write(root.resolve("head"), ByteBuffer.allocate(8).putLong(999).array());
            assertThrows(IllegalStateException.class, journal::checkHealthy);
            assertThrows(IllegalStateException.class, () -> journal.markEventApplied(TX_A));
        }
    }

    @Test void closedJournalCannotAcceptNewOperations() throws Exception {
        NormalMutationJournal journal = new NormalMutationJournal(root);
        journal.close();
        assertThrows(IllegalStateException.class, () -> journal.prepare(TX_A,
                NormalMutationJournal.Account.external(), NormalMutationJournal.Account.free(), 1));
    }

    @Test void unresolvedListIsImmutable() throws Exception {
        try (var journal = new NormalMutationJournal(root)) {
            journal.prepare(TX_A, NormalMutationJournal.Account.external(),
                    NormalMutationJournal.Account.free(), 1);
            assertThrows(UnsupportedOperationException.class, () -> journal.unresolved().clear());
        }
    }
}
