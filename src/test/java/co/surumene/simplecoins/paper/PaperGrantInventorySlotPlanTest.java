package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperGrantInventorySlotPlanTest {
    @TempDir Path root;

    private static ItemStack[] storageWithFreeSlots(int count) {
        ItemStack[] slots = new ItemStack[36];
        for (int i = count; i < slots.length; i++)
            slots[i] = new ItemStack(Material.STONE, 1);
        return slots;
    }

    @Test void amountGrantAllocatesCanonicalCoinsWithoutTouchingOccupiedStorage() throws Exception {
        var ledger = new GrantLedger(root.resolve("amount"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        ItemStack[] storage = storageWithFreeSlots(2);
        var originalLast = storage[35].clone();
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();

        assertEquals(2, plan.placements().size());
        assertEquals(new PaperGrantInventorySlotPlan.Slot(0,
                CoinDenomination.FIVE_THOUSAND, 1), plan.placements().get(0));
        assertEquals(new PaperGrantInventorySlotPlan.Slot(1,
                CoinDenomination.HUNDRED, 2), plan.placements().get(1));
        assertEquals(grant.sequence(), plan.nextReceipt().completedSequence());
        assertFalse(plan.nextReceipt().active());
        assertEquals(GrantReceiptState.empty(), plan.previousReceipt());
        assertEquals(originalLast, storage[35]);
        assertNull(storage[0]);
        assertNull(storage[1]);
        plan.requireUnchanged(GrantReceiptState.empty(), storage);
        assertThrows(UnsupportedOperationException.class, () -> plan.placements().clear());
    }

    @Test void partialGrantResumesWhenOneMoreRealStorageSlotIsFree() throws Exception {
        var ledger = new GrantLedger(root.resolve("partial"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        ItemStack[] storage = storageWithFreeSlots(1);
        var first = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        assertEquals(1, first.placements().size());
        assertEquals(CoinDenomination.FIVE_THOUSAND, first.placements().getFirst().denomination());
        assertTrue(first.nextReceipt().active());
        assertEquals(5000L, first.nextReceipt().partialValue());

        storage[0] = new ItemStack(Material.PAPER, 1);
        storage[1] = null;
        var second = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                first.nextReceipt(), storage).orElseThrow();
        assertEquals(new PaperGrantInventorySlotPlan.Slot(1,
                CoinDenomination.HUNDRED, 2), second.placements().getFirst());
        assertEquals(grant.sequence(), second.nextReceipt().completedSequence());
        assertFalse(second.nextReceipt().active());
        assertEquals(1, ledger.pending(player).size());
        assertEquals(Material.PAPER, storage[0].getType());
        assertNull(storage[1]);
    }

    @Test void exactCoinsKeepOriginalDenominationAnd64ItemLimit() throws Exception {
        var ledger = new GrantLedger(root.resolve("exact"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserveCoins(player, CoinDenomination.FIVE, 129, "console");
        ItemStack[] storage = storageWithFreeSlots(2);
        var first = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        assertEquals(2, first.placements().size());
        assertEquals(64, first.placements().get(0).count());
        assertEquals(64, first.placements().get(1).count());
        assertEquals(CoinDenomination.FIVE, first.placements().get(1).denomination());
        assertEquals(640L, first.nextReceipt().partialValue());

        storage[0] = new ItemStack(Material.PAPER, 64);
        storage[1] = new ItemStack(Material.PAPER, 64);
        storage[2] = null;
        var last = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                first.nextReceipt(), storage).orElseThrow();
        assertEquals(new PaperGrantInventorySlotPlan.Slot(2,
                CoinDenomination.FIVE, 1), last.placements().getFirst());
        assertTrue(last.nextReceipt().completedSequence() > 0);
    }

    @Test void fullInventoryProducesNoPlanAndDoesNotAdvanceReceipt() throws Exception {
        var ledger = new GrantLedger(root.resolve("full"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        ItemStack[] filled = storageWithFreeSlots(0);
        ItemStack[] before = Arrays.stream(filled).map(ItemStack::clone).toArray(ItemStack[]::new);
        assertTrue(PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), filled).isEmpty());
        assertArrayEquals(before, filled);
        assertEquals(1, ledger.pending(player).size());
    }

    @Test void changedInventoryOrReceiptIsRejectedBeforeAnyLaterCommit() throws Exception {
        var ledger = new GrantLedger(root.resolve("drift"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        ItemStack[] storage = storageWithFreeSlots(2);
        var plan = PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                GrantReceiptState.empty(), storage).orElseThrow();
        storage[35].setAmount(2);
        assertThrows(IllegalStateException.class,
                () -> plan.requireUnchanged(GrantReceiptState.empty(), storage));
        storage[35].setAmount(1);
        storage[0] = new ItemStack(Material.DIRT, 1);
        assertThrows(IllegalStateException.class,
                () -> plan.requireUnchanged(GrantReceiptState.empty(), storage));
        storage[0] = null;
        assertThrows(IllegalStateException.class,
                () -> plan.requireUnchanged(new GrantReceiptState(grant.sequence(), 0, null, 0), storage));
    }

    @Test void foreignAndForgedGrantOrWrongStorageAreaNeverPlansPhysicalIssuance() throws Exception {
        var ledger = new GrantLedger(root.resolve("invalid"));
        UUID player = UUID.randomUUID();
        var grant = ledger.reserve(player, 5200, "console");
        var forged = new GrantLedger.Grant(grant.sequence(), grant.id(), grant.player(), 5000,
                grant.issuer(), grant.createdMillis(), false,
                CoinDenomination.FIVE_THOUSAND, 1);
        ItemStack[] storage = storageWithFreeSlots(2);
        assertThrows(IllegalStateException.class,
                () -> PaperGrantInventorySlotPlan.stage(ledger, player, forged,
                        GrantReceiptState.empty(), storage));
        assertThrows(IllegalStateException.class,
                () -> PaperGrantInventorySlotPlan.stage(ledger, UUID.randomUUID(), grant,
                        GrantReceiptState.empty(), storage));
        assertThrows(IllegalArgumentException.class,
                () -> PaperGrantInventorySlotPlan.stage(ledger, player, grant,
                        GrantReceiptState.empty(), new ItemStack[41]));
        assertThrows(IllegalArgumentException.class,
                () -> new PaperGrantInventorySlotPlan.Slot(36, CoinDenomination.FIVE, 1));
    }
}
