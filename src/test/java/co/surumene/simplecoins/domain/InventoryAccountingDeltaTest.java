package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InventoryAccountingDeltaTest {
    @Test void actualTransferChangesDeadLocationAndFreeOnlyByConfirmedAmount() {
        var id = UUID.randomUUID();
        var dead = MemoryAccounting.Endpoint.dead(id, AssetOwner.DeadReason.MISSING_PLAYERDATA, "world/w/1,2,3:0");
        var free = MemoryAccounting.Endpoint.free();
        var ledger = new MemoryAccounting(Map.of(), BigInteger.ZERO, Map.of(dead, BigInteger.valueOf(5000)));
        var movements = InventoryAccountingDelta.between(Map.of(dead, BigInteger.valueOf(5000)),
                Map.of(dead, BigInteger.valueOf(3000), free, BigInteger.valueOf(2000)));
        assertEquals(MemoryAccounting.Outcome.APPLIED, ledger.record(1, movements));
        assertEquals(BigInteger.valueOf(2000), ledger.snapshot().free());
        assertEquals(Map.of(dead, BigInteger.valueOf(3000)), ledger.snapshot().dead());
    }
    @Test void cancelledAndSameOwnerMovesDoNotChangeBalances() {
        var free = MemoryAccounting.Endpoint.free();
        assertTrue(InventoryAccountingDelta.between(Map.of(free, BigInteger.TEN),
                Map.of(free, BigInteger.TEN)).isEmpty());
    }
    @Test void slotRelocationRemovesOldDeadLocation() {
        var id = UUID.randomUUID();
        var a = MemoryAccounting.Endpoint.dead(id, AssetOwner.DeadReason.CURRENT_BAN, "a");
        var b = MemoryAccounting.Endpoint.dead(id, AssetOwner.DeadReason.CURRENT_BAN, "b");
        var ledger = new MemoryAccounting(Map.of(), BigInteger.ZERO, Map.of(a, BigInteger.TEN));
        assertEquals(MemoryAccounting.Outcome.APPLIED, ledger.record(1,
                InventoryAccountingDelta.between(Map.of(a, BigInteger.TEN), Map.of(b, BigInteger.TEN))));
        assertEquals(Map.of(b, BigInteger.TEN), ledger.snapshot().dead());
    }
    @Test void rejectsNegativePhysicalValue() {
        assertThrows(IllegalArgumentException.class, () -> InventoryAccountingDelta.between(
                Map.of(MemoryAccounting.Endpoint.free(), BigInteger.valueOf(-1)), Map.of()));
    }
    @Test void partialPickupUsesConfirmedPlayerAndGroundBalances() {
        var id = UUID.randomUUID();
        var person = MemoryAccounting.Endpoint.personal(id);
        var free = MemoryAccounting.Endpoint.free();
        var ledger = new MemoryAccounting(Map.of(id, BigInteger.valueOf(62)), BigInteger.TEN, Map.of());
        var batch = new LocalValueChanges<String>();
        batch.observe("player", person, BigInteger.valueOf(62));
        batch.observe("item", free, BigInteger.TEN);
        batch.observe("item", free, BigInteger.valueOf(2)); // temporary event amount must not replace original
        batch.finish("player", person, BigInteger.valueOf(64));
        batch.finish("item", free, BigInteger.valueOf(8));
        assertEquals(MemoryAccounting.Outcome.APPLIED, ledger.record(1, batch.movements()));
        assertEquals(BigInteger.valueOf(64), ledger.snapshot().personal().get(id));
        assertEquals(BigInteger.valueOf(8), ledger.snapshot().free());
    }
    @Test void deathDropAndRetainedInventoryAreCountedOnce() {
        var id = UUID.randomUUID();
        var person = MemoryAccounting.Endpoint.personal(id);
        var free = MemoryAccounting.Endpoint.free();
        var batch = new LocalValueChanges<String>();
        batch.observe("player", person, BigInteger.valueOf(100));
        batch.observe("drop", free, BigInteger.ZERO);
        batch.finish("player", person, BigInteger.valueOf(30));
        batch.finish("drop", free, BigInteger.valueOf(70));
        var ledger = new MemoryAccounting(Map.of(id, BigInteger.valueOf(100)), BigInteger.ZERO, Map.of());
        assertEquals(MemoryAccounting.Outcome.APPLIED, ledger.record(1, batch.movements()));
        assertEquals(BigInteger.valueOf(30), ledger.snapshot().personal().get(id));
        assertEquals(BigInteger.valueOf(70), ledger.snapshot().free());
    }
    @Test void independentlyCommittedGrantIsNotCountedTwice() {
        var person = MemoryAccounting.Endpoint.personal(UUID.randomUUID());
        var batch = new LocalValueChanges<String>();
        batch.observe("player", person, BigInteger.TEN);
        batch.excludeAlreadyRecorded("player", BigInteger.valueOf(50));
        batch.finish("player", person, BigInteger.valueOf(60));
        assertTrue(batch.movements().isEmpty());
    }
    @Test void incompleteObservationCannotBecomeAZeroBalance() {
        var batch = new LocalValueChanges<String>();
        batch.observe("unread", MemoryAccounting.Endpoint.free(), BigInteger.ONE);
        assertThrows(IllegalStateException.class, batch::movements);
    }
}
