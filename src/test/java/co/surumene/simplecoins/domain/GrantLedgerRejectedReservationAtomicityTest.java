package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Characterizes rejected-input non-persistence, not the normative exception type. */
class GrantLedgerRejectedReservationAtomicityTest {
    @TempDir Path directory;

    @Test void overflowedTypedRequestDoesNotConsumeSequenceOrPoisonJournal() throws Exception {
        Path journal = directory.resolve("overflow");
        UUID recipient = UUID.randomUUID();
        var ledger = new GrantLedger(journal);
        assertThrows(ArithmeticException.class, () ->
                ledger.reserveCoins(recipient, CoinDenomination.THOUSAND,
                        Long.MAX_VALUE, "console"));
        assertEquals(0L, grantFileCount(journal));
        var accepted = ledger.reserveCoins(recipient, CoinDenomination.FIVE, 3, "console");
        assertEquals(1L, accepted.sequence());
        assertEquals(15L, accepted.amount());
        assertEquals(accepted, new GrantLedger(journal).pending(recipient).getFirst());
    }

    @Test void rejectedZeroAmountAndBlankIssuerLeaveRestartableEmptyJournal() throws Exception {
        Path journal = directory.resolve("invalid");
        UUID recipient = UUID.randomUUID();
        var ledger = new GrantLedger(journal);
        assertThrows(IllegalArgumentException.class, () -> ledger.reserve(recipient, 0, "console"));
        assertThrows(IllegalArgumentException.class, () -> ledger.reserve(recipient, 10, " "));
        assertEquals(0L, grantFileCount(journal));
        var restarted = new GrantLedger(journal);
        var accepted = restarted.reserve(recipient, 10, "console");
        assertEquals(1L, accepted.sequence());
        assertEquals(1L, grantFileCount(journal));
    }

    private static long grantFileCount(Path journal) throws Exception {
        try (var files = Files.list(journal)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".grant")).count();
        }
    }
}
