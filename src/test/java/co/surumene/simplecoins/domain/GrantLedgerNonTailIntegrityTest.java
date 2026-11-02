package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec Candidate: test whether an active journal protects older
 * durable records, not just the head and newest tail. The first two cases
 * are expected Red until a normative recovery policy is confirmed.
 */
class GrantLedgerNonTailIntegrityTest {
    @TempDir Path temporary;

    @Test void deletedNonTailRecordMustInvalidateLiveLedgerBeforeFurtherIssuance() throws Exception {
        Path root = temporary.resolve("deleted-old");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertTrue(Files.exists(root.resolve("head")));

        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A valid tail cannot establish the integrity of an absent earlier grant");
        assertThrows(IllegalStateException.class, () -> ledger.reserve(player, 300, "console"));
        assertFalse(Files.exists(root.resolve("3.grant")));
    }

    @Test void corruptedNonTailRecordMustInvalidateLiveLedgerBeforeFurtherIssuance() throws Exception {
        Path root = temporary.resolve("corrupted-old");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.write(root.resolve("1.grant"), new byte[]{0, 1, 2});
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertTrue(Files.exists(root.resolve("head")));

        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "A valid tail cannot establish the integrity of a corrupted earlier grant");
        assertThrows(IllegalStateException.class, () -> ledger.reserve(player, 300, "console"));
        assertFalse(Files.exists(root.resolve("3.grant")));
    }

    @Test void intactNonTailRecordsAllowNewIssuanceAndDurableReplay() throws Exception {
        Path root = temporary.resolve("intact");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        var second = ledger.reserve(player, 200, "console");

        assertEquals(List.of(first, second), ledger.pending(player));
        var third = ledger.reserve(player, 300, "console");
        assertEquals(3, third.sequence());

        var reloaded = new GrantLedger(root);
        assertEquals(List.of(first, second, third), reloaded.pending(player));
    }
}
