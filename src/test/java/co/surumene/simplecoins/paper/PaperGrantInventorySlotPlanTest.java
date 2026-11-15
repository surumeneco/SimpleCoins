package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pure preflight tests; real Paper ItemStack serialization needs Paper smoke. */
class PaperGrantInventorySlotPlanTest {
    @TempDir Path root;

    private static PaperGrantInventorySlotPlan.StorageSnapshot storageWithFreeSlots(int free) {
        List<String> contents = new ArrayList<>(Collections.nCopies(36, "stone:1"));
        for (int i = 0; i < free; i++) contents.set(i, null);
        return new PaperGrantInventorySlotPlan.StorageSnapshot(contents);
    }

    private static PaperGrantInventorySlotPlan.StorageSnapshot withSlot(
            PaperGrantInventorySlotPlan.StorageSnapshot prior, int index, String item) {
        var changed = new ArrayList<>(prior.fingerprints());
        changed.set(index, item);
        return new PaperGrantInventorySlotPlan.StorageSnapshot(changed);
    }

    @Test void amountGrantAllocatesCanonicalCoinsWithoutTouchingOccupiedStorage() throws Exception {
        var ledger = new GrantLedger(root.resolve("amount"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var storage = storageWithFreeSlots(2);
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();

        assertEquals(List.of(
                new PaperGrantInventorySlotPlan.Slot(0, CoinDenomination.FIVE_THOUSAND, 1),
                new PaperGrantInventorySlotPlan.Slot(1, CoinDenomination.HUNDRED, 2)),
                plan.placements());
        assertEquals(grant.sequence(), plan.nextReceipt().completedSequence());
        assertFalse(plan.nextReceipt().active());
        assertEquals(GrantReceiptState.empty(), plan.previousReceipt());
        assertEquals("stone:1", storage.fingerprints().get(35));
        assertTrue(storage.vacant(0));
        assertTrue(storage.vacant(1));
        plan.requireUnchanged(GrantReceiptState.empty(), storage);
        assertThrows(UnsupportedOperationException.class,
                () -> plan.placements().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> storage.fingerprints().set(0, "fake"));
    }

    @Test void partialGrantResumesWhenAnotherPhysicalStorageSlotBecomesFree() throws Exception {
        var ledger = new GrantLedger(root.resolve("partial"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var storage = storageWithFreeSlots(1);
        var first = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        assertEquals(List.of(new PaperGrantInventorySlotPlan.Slot(0,
                CoinDenomination.FIVE_THOUSAND, 1)), first.placements());
        assertTrue(first.nextReceipt().active());
        assertEquals(5000L, first.nextReceipt().partialValue());

        var afterFirstSave = withSlot(storage, 0, "issued-coin-5000:1");
        afterFirstSave = withSlot(afterFirstSave, 1, null);
        var second = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                first.nextReceipt(), afterFirstSave).orElseThrow();
        assertEquals(List.of(new PaperGrantInventorySlotPlan.Slot(1,
                CoinDenomination.HUNDRED, 2)), second.placements());
        assertEquals(grant.sequence(), second.nextReceipt().completedSequence());
        assertFalse(second.nextReceipt().active());
        assertEquals(1, ledger.pending(player).size());
        assertEquals("issued-coin-5000:1", afterFirstSave.fingerprints().get(0));
        assertTrue(afterFirstSave.vacant(1));
    }

    @Test void exactCoinsKeepChosenDenominationAnd64ItemLimit() throws Exception {
        var ledger = new GrantLedger(root.resolve("exact"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE, 129, "console");
        var storage = storageWithFreeSlots(2);
        var first = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        assertEquals(List.of(
                new PaperGrantInventorySlotPlan.Slot(0, CoinDenomination.FIVE, 64),
                new PaperGrantInventorySlotPlan.Slot(1, CoinDenomination.FIVE, 64)),
                first.placements());
        assertEquals(640L, first.nextReceipt().partialValue());

        var nextStorage = withSlot(storage, 0, "coin-5:64");
        nextStorage = withSlot(nextStorage, 1, "coin-5:64");
        nextStorage = withSlot(nextStorage, 2, null);
        var last = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                first.nextReceipt(), nextStorage).orElseThrow();
        assertEquals(List.of(new PaperGrantInventorySlotPlan.Slot(2,
                CoinDenomination.FIVE, 1)), last.placements());
        assertEquals(grant.sequence(), last.nextReceipt().completedSequence());
    }

    @Test void fullInventoryCreatesNoPlanAndKeepsGrantPending() throws Exception {
        var ledger = new GrantLedger(root.resolve("full"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var filled = storageWithFreeSlots(0);
        assertTrue(PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), filled).isEmpty());
        assertEquals("stone:1", filled.fingerprints().get(0));
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void changedStorageOrReceiptIsRejectedBeforeFutureCommit() throws Exception {
        var ledger = new GrantLedger(root.resolve("drift"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var storage = storageWithFreeSlots(2);
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        assertThrows(IllegalStateException.class, () -> plan.requireUnchanged(
                GrantReceiptState.empty(), withSlot(storage, 35, "stone:2")));
        assertThrows(IllegalStateException.class, () -> plan.requireUnchanged(
                GrantReceiptState.empty(), withSlot(storage, 0, "dirt:1")));
        assertThrows(IllegalStateException.class, () -> plan.requireUnchanged(
                new GrantReceiptState(grant.sequence(), 0, null, 0), storage));
        plan.requireUnchanged(GrantReceiptState.empty(), storage);
    }

    @Test void foreignAndForgedGrantOrInvalidStorageCannotPlanIssuance() throws Exception {
        var ledger = new GrantLedger(root.resolve("invalid"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var forged = new GrantLedger.Grant(grant.sequence(), grant.id(), grant.player(), 5000,
                grant.issuer(), grant.createdMillis(), false,
                CoinDenomination.FIVE_THOUSAND, 1);
        var storage = storageWithFreeSlots(2);
        assertThrows(IllegalStateException.class,
                () -> PaperGrantInventorySlotPlan.stage(ledger, player, forged,
                        GrantReceiptState.empty(), storage));
        assertThrows(IllegalStateException.class,
                () -> PaperGrantInventorySlotPlan.stage(ledger, UUID.randomUUID(), grant,
                        GrantReceiptState.empty(), storage));
        assertThrows(IllegalArgumentException.class,
                () -> new PaperGrantInventorySlotPlan.StorageSnapshot(
                        Collections.nCopies(41, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new PaperGrantInventorySlotPlan.Slot(36, CoinDenomination.FIVE, 1));
    }
}
