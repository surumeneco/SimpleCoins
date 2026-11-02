package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-006: test-only journal replay evidence.
 * These tests do not simulate Paper playerdata or live inventory mutation.
 */
class OfflineGrantInterleavingContractTest {
    @TempDir Path directory;

    @Test void interleavedRecipientsHaveIndependentReceipts() throws Exception {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        var ledger = new GrantLedger(directory.resolve("interleaved"));
        var a1 = ledger.reserve(alice, 100, "console");
        var b1 = ledger.reserve(bob, 50, "console");
        var a2 = ledger.reserveCoins(alice, CoinDenomination.FIVE, 3, "console");

        assertEquals(2, ledger.pending(alice).size());
        assertEquals(1, ledger.pending(bob).size());
        assertEquals(a1.sequence(), ledger.pending(alice).get(0).sequence());
        assertEquals(a2.sequence(), ledger.pending(alice).get(1).sequence());
        assertTrue(ledger.wasApplied(b1, b1.sequence()));
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(alice, b1.sequence()));
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(alice, a2.sequence()));

        ledger.acknowledge(b1, b1.sequence());
        ledger.acknowledge(a1, a1.sequence());
        ledger.acknowledge(a2, a2.sequence());
        var recovered = new GrantLedger(directory.resolve("interleaved"));
        assertTrue(recovered.pending(alice).isEmpty());
        assertTrue(recovered.pending(bob).isEmpty());
    }

    @Test void restartPreservesOtherRecipientsPendingOrder() throws Exception {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Path path = directory.resolve("restart");
        var ledger = new GrantLedger(path);
        var a1 = ledger.reserve(alice, 100, "console");
        var b1 = ledger.reserve(bob, 200, "console");
        var a2 = ledger.reserve(alice, 300, "console");
        ledger.acknowledge(b1, b1.sequence());

        var restarted = new GrantLedger(path);
        assertEquals(2, restarted.pending(alice).size());
        assertEquals(a1.id(), restarted.pending(alice).get(0).id());
        assertEquals(a2.id(), restarted.pending(alice).get(1).id());
        assertTrue(restarted.pending(bob).isEmpty());
        assertThrows(IllegalStateException.class, () -> restarted.acknowledge(a2, a2.sequence()));
        restarted.acknowledge(a1, a1.sequence());
        restarted.acknowledge(a2, a2.sequence());
        assertTrue(new GrantLedger(path).pending(alice).isEmpty());
    }

    @Test void repeatedPostSaveAcknowledgementIsIdempotentAcrossRestarts() throws Exception {
        UUID alice = UUID.randomUUID();
        Path path = directory.resolve("idempotent");
        var ledger = new GrantLedger(path);
        var grant = ledger.reserve(alice, 100, "console");
        var firstRestart = new GrantLedger(path);
        assertTrue(firstRestart.wasApplied(grant, grant.sequence()));
        firstRestart.acknowledge(grant, grant.sequence());
        var secondRestart = new GrantLedger(path);
        secondRestart.acknowledge(grant, grant.sequence());
        assertTrue(secondRestart.pending(alice).isEmpty());
        var next = secondRestart.reserve(alice, 50, "console");
        assertEquals(grant.sequence() + 1, next.sequence());
        assertEquals(1, new GrantLedger(path).pending(alice).size());
    }
}
