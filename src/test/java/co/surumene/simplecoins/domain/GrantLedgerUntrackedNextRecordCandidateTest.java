package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: code-to-spec characterization Candidate.
 * This records observed fail-closed behavior; it is not a Confirmed CAT requirement.
 */
class GrantLedgerUntrackedNextRecordCandidateTest {
    @TempDir Path temporary;

    @Test void untrackedNextSequenceMustNotBeOverwrittenOrIssued() throws Exception {
        Path root = temporary.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.randomUUID();
        var first = ledger.reserve(player, 100, "console");

        // Simulate an interrupted or competing writer leaving a durable file
        // at the next sequence, without advancing this process's in-memory head.
        byte[] unexpected = Files.readAllBytes(root.resolve(first.sequence() + ".grant"));
        Path next = root.resolve((first.sequence() + 1) + ".grant");
        Files.write(next, unexpected);

        assertThrows(IOException.class, () -> ledger.reserve(player, 200, "console"));
        assertArrayEquals(unexpected, Files.readAllBytes(next),
                "An untracked durable record must not be overwritten");
        assertThrows(IllegalStateException.class, () -> ledger.pending(player),
                "The same live ledger must remain failed after the collision");
        assertThrows(IOException.class, () -> new GrantLedger(root),
                "Restart must reject the misnamed durable record rather than accept it");
    }
}
