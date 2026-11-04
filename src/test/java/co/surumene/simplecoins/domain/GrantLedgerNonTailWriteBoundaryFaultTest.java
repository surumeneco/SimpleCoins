package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SC-JOURNAL-WRITE-01..02: expected-Red Draft fault injection.
 * An intact head and tail must not mask loss of a non-tail durable reservation.
 */
class GrantLedgerNonTailWriteBoundaryFaultTest {
    @TempDir Path temporary;

    @Test void reserveMustNotAppendAfterEarlierDurableRecordDisappears() throws Exception {
        Path root = temporary.resolve("reserve");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");
        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class,
                () -> ledger.reserve(player, 300, "console"));
        assertFalse(Files.exists(root.resolve("3.grant")),
                "Failed closure must not persist an additional reservation");
    }

    @Test void acknowledgeMustNotRewriteTailAfterEarlierDurableRecordDisappears()
            throws Exception {
        Path root = temporary.resolve("acknowledge");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        ledger.acknowledge(first, first.sequence());
        var second = ledger.reserve(player, 200, "console");
        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(second, second.sequence()));
    }
}
