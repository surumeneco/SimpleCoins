package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantReceiptState;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Convert a verified offline-grant slot plan into a detached player-storage
 * candidate. This is NOT a Paper inventory commit: no source slot, Player PDC,
 * grant journal or world data is modified or saved here. A separate main-thread
 * coordinator must persist actual inventory and receipt together, verify their
 * saved playerdata generation and only then acknowledge issuance.
 */
public final class PaperGrantInventoryMaterializer {
    private PaperGrantInventoryMaterializer() {}

    /**
     * Recheck ALL physical slots and the receipt before creating any coin.
     * Authentic money comes only from the fixed-denomination CoinItemCodec;
     * all occupied items are cloned rather than repurposed or overwritten.
     */
    public static ItemStack[] buildCopy(PaperGrantInventorySlotPlan plan,
                                        GrantReceiptState observedReceipt,
                                        ItemStack[] observedStorage, CoinItemCodec coins) {
        Objects.requireNonNull(plan, "delivery plan");
        Objects.requireNonNull(observedReceipt, "observed grant receipt");
        Objects.requireNonNull(observedStorage, "player storage");
        Objects.requireNonNull(coins, "canonical Paper currency codec");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Grant ItemStack creation requires Paper main thread");

        plan.requireUnchanged(observedReceipt, observedStorage);
        ItemStack[] next = new ItemStack[PaperGrantInventorySlotPlan.STORAGE_SLOTS];
        for (int i = 0; i < next.length; i++)
            next[i] = observedStorage[i] == null ? null : observedStorage[i].clone();

        for (PaperGrantInventorySlotPlan.Slot placement : plan.placements()) {
            int slot = placement.index();
            if (next[slot] != null && !next[slot].getType().isAir())
                throw new IllegalStateException("Grant tried to overwrite occupied storage slot " + slot);

            ItemStack created = coins.create(placement.denomination(), placement.count());
            if (created.getType() != Material.PAPER
                    || created.getAmount() != placement.count()
                    || created.getAmount() > created.getMaxStackSize()
                    || coins.identify(created).orElseThrow() != placement.denomination())
                throw new IllegalStateException(
                        "New physical grant coin differs from canonical reservation");
            next[slot] = created.clone();
        }
        return next;
    }
}
