package org.bukkit.craftbukkit.inventory;

import org.bukkit.inventory.ItemStack;
import co.surumene.simplecoins.writeragent.WriterEpochHook;

/** Exact Paper setter bytecode signature, with a nested-writer count assertion. */
public final class CraftInventory {
    private static int addItemBodyExecutions;

    /** Exact real Paper26.2 addItem JVM descriptor and non-void return shape. */
    public java.util.HashMap<Integer, ItemStack> addItem(ItemStack... items) {
        ++addItemBodyExecutions;
        return new java.util.HashMap<>();
    }

    public static int addItemBodyExecutions() { return addItemBodyExecutions; }

    public void setItem(int slot, ItemStack item) {
        if (WriterEpochHook.mode() == WriterEpochHook.Mode.LIMITED_TRACKING
                && WriterEpochHook.inFlight() < 2)
            throw new AssertionError("Injected Inventory setter was not nested in PDC scope");
    }
}
