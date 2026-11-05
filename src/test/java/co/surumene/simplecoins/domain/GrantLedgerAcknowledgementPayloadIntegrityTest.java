package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005, CAT Candidate SC-JOURNAL-ACK-PAYLOAD-01 (NOT Confirmed).
 * Negative cases deliberately characterize a proposed fail-closed invariant;
 * they must not be treated as an approved Gate or a production implementation.
 */
class GrantLedgerAcknowledgementPayloadIntegrityTest {
    @TempDir Path temporaryDirectory;

    @Test void acknowledgementRejectsSameSequenceAndIdWithDifferentRecipient() throws Exception {
        Path root = temporaryDirectory.resolve("recipient-journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000091");
        var canonical = ledger.reserve(player, 100, "console");
        Path record = root.resolve(canonical.sequence() + ".grant");
        byte[] durableBefore = Files.readAllBytes(record);

        var wrongRecipient = new GrantLedger.Grant(
                canonical.sequence(), canonical.id(),
                UUID.fromString("00000000-0000-0000-0000-000000000092"),
                canonical.amount(), canonical.issuer(), canonical.createdMillis(),
                canonical.delivered(), canonical.denomination(), canonical.coinCount());

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(wrongRecipient, canonical.sequence()),
                "An untrusted grant argument must not acknowledge another recipient's grant");
        assertArrayEquals(durableBefore, Files.readAllBytes(record));
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void acknowledgementRejectsSameIdentityWithDifferentMonetaryPayload() throws Exception {
        Path root = temporaryDirectory.resolve("amount-journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000093");
        var canonical = ledger.reserve(player, 100, "console");
        Path record = root.resolve(canonical.sequence() + ".grant");
        byte[] durableBefore = Files.readAllBytes(record);

        var wrongAmountAndIssuer = new GrantLedger.Grant(
                canonical.sequence(), canonical.id(), canonical.player(),
                canonical.amount() + 1, "other-issuer", canonical.createdMillis(),
                canonical.delivered(), canonical.denomination(), canonical.coinCount());

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(wrongAmountAndIssuer, canonical.sequence()),
                "Grant UUID alone must not authenticate a different amount and issuer");
        assertArrayEquals(durableBefore, Files.readAllBytes(record));
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void acknowledgementRejectsSameIdentityWithChangedCreationTimestamp() throws Exception {
        Path root = temporaryDirectory.resolve("timestamp-journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000094");
        var canonical = ledger.reserve(player, 100, "console");
        Path record = root.resolve(canonical.sequence() + ".grant");
        byte[] durableBefore = Files.readAllBytes(record);

        var wrongTimestamp = new GrantLedger.Grant(
                canonical.sequence(), canonical.id(), canonical.player(), canonical.amount(),
                canonical.issuer(), canonical.createdMillis() + 1,
                canonical.delivered(), canonical.denomination(), canonical.coinCount());

        assertThrows(IllegalStateException.class,
                () -> ledger.acknowledge(wrongTimestamp, canonical.sequence()));
        assertArrayEquals(durableBefore, Files.readAllBytes(record));
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void matchingGrantAndDurableReceiptStillAcknowledgeOnce() throws Exception {
        Path root = temporaryDirectory.resolve("control-journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000095");
        var canonical = ledger.reserve(player, 100, "console");

        ledger.acknowledge(canonical, canonical.sequence());
        assertTrue(ledger.pending(player).isEmpty());

        ledger.acknowledge(canonical, canonical.sequence());
        assertTrue(ledger.pending(player).isEmpty());
        assertTrue(new GrantLedger(root).pending(player).isEmpty(),
                "The acknowledged record must survive a journal reopen");
    }
}
