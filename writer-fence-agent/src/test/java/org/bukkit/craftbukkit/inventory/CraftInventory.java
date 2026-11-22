package org.bukkit.craftbukkit.inventory;

import org.bukkit.inventory.ItemStack;
import co.surumene.simplecoins.writeragent.WriterEpochHook;

/** Exact Paper setter bytecode signature, with a nested-writer count assertion. */
public final class CraftInventory {
    public void setItem(int slot, ItemStack item) {
        if (WriterEpochHook.mode() == WriterEpochHook.Mode.LIMITED_TRACKING
                && WriterEpochHook.inFlight() != 2)
            throw new AssertionError("Injected Inventory setter was not nested in PDC scope");
    }
}
