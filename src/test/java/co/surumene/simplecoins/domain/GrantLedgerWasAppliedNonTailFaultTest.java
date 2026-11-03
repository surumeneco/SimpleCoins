package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Code-to-spec Draft: non-tail durability evidence at the wasApplied read boundary. */
class GrantLedgerWasAppliedNonTailFaultTest {
    @TempDir Path temporary;

    @Test void deletedNonTailReservationCannotBeTreatedAsApplied() throws Exception {
        Path root = temporary.resolve("deleted");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.delete(root.resolve("1.grant"));
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.wasApplied(first, 1),
                "An intact head and tail do not prove an older reservation survives");
    }

    @Test void corruptedNonTailReservationCannotBeTreatedAsApplied() throws Exception {
        Path root = temporary.resolve("corrupt");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Files.write(root.resolve("1.grant"), new byte[]{0, 1, 2});
        assertTrue(Files.exists(root.resolve("2.grant")));
        assertThrows(IllegalStateException.class, () -> ledger.wasApplied(first, 1),
                "Corrupted older evidence must not authorize a receipt decision");
    }

    @Test void intactNonTailReservationCanBeRecognizedAsApplied() throws Exception {
        Path root = temporary.resolve("intact");
        var ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        assertTrue(ledger.wasApplied(first, 1));
        assertFalse(ledger.wasApplied(first, 0));
    }
}
