package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

class TraderIssuanceJournalTest {
    @TempDir Path temp;
    private static final UUID TRADER = UUID.fromString("d4ba7a98-2ff2-43da-b76c-426ab843f1bb");
    private static WanderingTraderDropPolicy.Drop drop() {
        return new WanderingTraderDropPolicy.Drop(CoinDenomination.HUNDRED, 65);
    }

    @Test void noDropDecisionIsDurablyDeduplicatedWithoutUnresolvedMoney() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.begin(TRADER, Optional.empty()));
            assertFalse(journal.begin(TRADER, Optional.of(drop())));
            assertTrue(journal.unresolved().isEmpty());
            assertEquals(0, journal.receipt(TRADER).orElseThrow().value());
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.has(TRADER));
            assertFalse(journal.begin(TRADER, Optional.of(drop())));
            assertEquals(TraderIssuanceJournal.Status.NO_DROP,
                    journal.receipt(TRADER).orElseThrow().status());
        }
    }

    @Test void positiveEventReceiptSurvivesReloadAndNeverClaimsWorldCommit() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.begin(TRADER, Optional.of(drop())));
            assertEquals(1, journal.unresolved().size());
            assertEquals(6500, journal.unresolved().getFirst().value());
            journal.markEventRecorded(TRADER);
            journal.markEventRecorded(TRADER);
            assertEquals(TraderIssuanceJournal.Status.EVENT_RECORDED,
                    journal.receipt(TRADER).orElseThrow().status());
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertFalse(journal.begin(TRADER, Optional.of(drop())));
            assertEquals(1, journal.unresolved().size());
            assertEquals(TraderIssuanceJournal.Status.EVENT_RECORDED,
                    journal.receipt(TRADER).orElseThrow().status());
        }
    }

    @Test void midEventCrashKeepsPreparedReceiptForReconciliation() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            journal.begin(TRADER, Optional.of(drop()));
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertEquals(TraderIssuanceJournal.Status.PREPARED,
                    journal.receipt(TRADER).orElseThrow().status());
            assertFalse(journal.begin(TRADER, Optional.of(drop())));
            assertThrows(IllegalStateException.class,
                    () -> journal.markEventRecorded(UUID.randomUUID()));
        }
    }

    @Test void completedEventOnDiskBlocksNextStartupButNotSameSession() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertFalse(journal.hasUnreconciledFromStartup());
            assertTrue(journal.begin(TRADER, Optional.of(drop())));
            assertFalse(journal.hasUnreconciledFromStartup(),
                    "A live receipt must not retroactively alter the startup snapshot");
            journal.markEventRecorded(TRADER);
            assertEquals(1, journal.unresolved().size());
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.hasUnreconciledFromStartup(),
                    "An event receipt cannot certify saved entity/ledger state");
            assertTrue(journal.receipt(TRADER).orElseThrow().needsReconciliation());
        }
    }

    @Test void preparedReceiptBlocksRestartButNoDropDoesNot() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.begin(TRADER, Optional.empty()));
            assertFalse(journal.hasUnreconciledFromStartup());
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertFalse(journal.hasUnreconciledFromStartup());
            assertTrue(journal.begin(UUID.randomUUID(), Optional.of(drop())));
        }
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertTrue(journal.hasUnreconciledFromStartup());
        }
    }

    @Test void rejectsConcurrentWriterAndCorruptRecord() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertThrows(IOException.class, () -> new TraderIssuanceJournal(temp));
            assertTrue(journal.begin(TRADER, Optional.of(drop())));
        }
        Path path = temp.resolve(TRADER + ".trader");
        byte[] saved = Files.readAllBytes(path);
        saved[saved.length - 1] ^= 0x7F;
        Files.write(path, saved);
        assertThrows(IOException.class, () -> new TraderIssuanceJournal(temp));
    }

    @Test void rejectReceiptTransitionsWithoutValidReserve() throws Exception {
        try (var journal = new TraderIssuanceJournal(temp)) {
            assertThrows(IllegalStateException.class, () -> journal.markEventRecorded(TRADER));
            assertTrue(journal.begin(TRADER, Optional.empty()));
            assertThrows(IllegalStateException.class, () -> journal.markEventRecorded(TRADER));
            assertThrows(NullPointerException.class, () -> journal.begin(TRADER, null));
        }
    }
}
