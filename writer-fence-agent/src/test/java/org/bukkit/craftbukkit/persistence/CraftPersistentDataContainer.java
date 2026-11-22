package org.bukkit.craftbukkit.persistence;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.persistence.PersistentDataType;

/** Exact Paper PDC set/remove bytecode signatures; setter invokes nested inventory writer. */
public final class CraftPersistentDataContainer {
    public void set(NamespacedKey key, PersistentDataType type, Object value) {
        new CraftInventory().setItem(0, new ItemStack());
    }

    public void remove(NamespacedKey key) {}
}
