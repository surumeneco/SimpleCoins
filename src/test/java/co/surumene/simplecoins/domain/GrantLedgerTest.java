package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrantLedgerTest {
    @TempDir Path directory;

    @Test void reservationSurvivesRestartUntilPlayerReceiptConfirmsDelivery() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(directory.resolve("grants"));
        var grant = ledger.reserve(player, 5200, "console");
        var recovered = new GrantLedger(directory.resolve("grants"));
        assertEquals(grant, recovered.pending(player).get(0));
        assertFalse(recovered.wasApplied(grant, 0));
        assertTrue(recovered.wasApplied(grant, grant.sequence()));
        recovered.acknowledge(grant, grant.sequence());
        assertTrue(new GrantLedger(directory.resolve("grants")).pending(player).isEmpty());
    }

    @Test void denominationReservationPreservesPhysicalCoinDenominationAcrossRestart() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(directory.resolve("grants"));
        var grant = ledger.reserveCoins(player, CoinDenomination.ONE, 25, "console");
        assertEquals(25, grant.amount());
        assertEquals(CoinDenomination.ONE, grant.denomination());
        assertEquals(25, grant.coinCount());

        var recovered = new GrantLedger(directory.resolve("grants"));
        assertEquals(grant, recovered.pending(player).getFirst());
        assertEquals(CoinDenomination.ONE, recovered.pending(player).getFirst().denomination());
        recovered.acknowledge(grant, grant.sequence());
        assertTrue(new GrantLedger(directory.resolve("grants")).pending(player).isEmpty());
    }

    @Test void crashAfterPlayerSaveBeforeAcknowledgementDoesNotRequireReissuance() throws Exception {
        UUID player=UUID.randomUUID();
        var path=directory.resolve("grants");
        var first=new GrantLedger(path);
        var grant=first.reserve(player,100,"console");
        // Inventory and receipt are already saved, journal is still pending.
        var restarted=new GrantLedger(path);
        assertTrue(restarted.wasApplied(grant,grant.sequence()));
        restarted.acknowledge(grant,grant.sequence());
        restarted.acknowledge(grant,grant.sequence());
        assertTrue(restarted.pending(player).isEmpty());
        var next=restarted.reserve(player,50,"console");
        assertTrue(next.sequence()>grant.sequence());
        assertFalse(restarted.wasApplied(next,grant.sequence()));
    }

    @Test void incompleteOrOtherPlayersReceiptCannotAcknowledgeGrant() throws Exception {
        UUID player=UUID.randomUUID();
        var ledger=new GrantLedger(directory.resolve("grants"));
        var grant=ledger.reserve(player,100,"console");
        var other=ledger.reserve(UUID.randomUUID(),5,"console");
        assertThrows(IllegalStateException.class,()->ledger.acknowledge(grant,0));
        assertThrows(IllegalStateException.class,()->ledger.acknowledge(grant,other.sequence()));
        assertEquals(1,ledger.pending(player).size());
    }

    @Test void acknowledgedPlayerdataRollbackFailsClosed() throws Exception {
        UUID player=UUID.randomUUID();
        var ledger=new GrantLedger(directory.resolve("grants"));
        var grant=ledger.reserve(player,100,"console");
        ledger.acknowledge(grant,grant.sequence());
        assertThrows(IllegalStateException.class,()->ledger.validateReceipt(player,0));
    }

    @Test void aLaterReceiptCannotSkipAnEarlierUndeliveredGrant() throws Exception {
        UUID player = UUID.randomUUID();
        var ledger = new GrantLedger(directory.resolve("ordered"));
        var first = ledger.reserve(player, 100, "console");
        var second = ledger.reserveCoins(player, CoinDenomination.FIVE, 3, "console");
        assertThrows(IllegalStateException.class, () -> ledger.validateReceipt(player, second.sequence()));
        assertThrows(IllegalStateException.class, () -> ledger.acknowledge(first, second.sequence()));
        assertEquals(2, ledger.pending(player).size());
        ledger.acknowledge(first, first.sequence());
        assertTrue(ledger.wasApplied(second, second.sequence()));
    }

    @Test void aLegacyMoneyOnlyJournalRemainsReadableAfterSchemaUpgrade() throws Exception {
        Path folder = directory.resolve("legacy");
        java.nio.file.Files.createDirectories(folder);
        UUID id = UUID.randomUUID(), player = UUID.randomUUID();
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.DataOutputStream(bytes)) {
            out.writeInt(0x53434731);
            out.writeLong(1);
            out.writeUTF(id.toString());
            out.writeUTF(player.toString());
            out.writeLong(100);
            out.writeUTF("console");
            out.writeLong(12345L);
            out.writeBoolean(false);
        }
        DurableFile.write(folder.resolve("1.grant"), bytes.toByteArray());
        DurableFile.write(folder.resolve("head"),
                java.nio.ByteBuffer.allocate(8).putLong(1L).array());
        var recovered = new GrantLedger(folder);
        var grant = recovered.pending(player).getFirst();
        assertEquals(100L, grant.amount());
        assertNull(grant.denomination());
        assertEquals(0L, grant.coinCount());
        recovered.acknowledge(grant, grant.sequence());
        assertTrue(new GrantLedger(folder).pending(player).isEmpty());
    }

    @Test void corruptOrMissingJournalNeverBecomesAnEmptyReservation() throws Exception {
        Path path=directory.resolve("grants");
        var ledger=new GrantLedger(path);
        ledger.reserve(UUID.randomUUID(),100,"console");
        ledger.reserve(UUID.randomUUID(),50,"console");
        byte[] bytes=java.nio.file.Files.readAllBytes(path.resolve("1.grant"));
        bytes[bytes.length-2]^=1;
        java.nio.file.Files.write(path.resolve("1.grant"),bytes);
        assertThrows(java.io.IOException.class,()->new GrantLedger(path));
        java.nio.file.Files.delete(path.resolve("1.grant"));
        assertThrows(java.io.IOException.class,()->new GrantLedger(path));
    }
    @Test void losingLastReservationIsDetectedEvenWhenSequenceHasNoGap() throws Exception {
        Path path = directory.resolve("grants");
        var ledger = new GrantLedger(path);
        ledger.reserve(UUID.randomUUID(), 100, "console");
        java.nio.file.Files.delete(path.resolve("1.grant"));
        assertThrows(java.io.IOException.class, () -> new GrantLedger(path));
    }

    @Test void reservationWrittenBeforeHeadUpdateIsRecovered() throws Exception {
        Path path = directory.resolve("grants");
        var ledger = new GrantLedger(path);
        byte[] initialHead = java.nio.file.Files.readAllBytes(path.resolve("head"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 100, "console");
        java.nio.file.Files.write(path.resolve("head"), initialHead);
        assertEquals(grant, new GrantLedger(path).pending(player).getFirst());
    }
}
