package org.bukkit.craftbukkit.inventory;

import org.bukkit.inventory.ItemStack;
import co.surumene.simplecoins.writeragent.WriterEpochHook;

/** Two real Paper method descriptors; actual injected product hook, not a real Paper class. */
public final class CraftEntityEquipment {
    public void setItemInMainHand(ItemStack item) { requireTrackedParent(); }
    public void setItemInMainHand(ItemStack item, boolean silent) { requireTrackedParent(); }
    private static void requireTrackedParent() {
        if (WriterEpochHook.mode() == WriterEpochHook.Mode.LIMITED_TRACKING
                && WriterEpochHook.inFlight() < 2)
            throw new AssertionError("Mainhand setter was not in the admitted WWW scope");
    }
}
