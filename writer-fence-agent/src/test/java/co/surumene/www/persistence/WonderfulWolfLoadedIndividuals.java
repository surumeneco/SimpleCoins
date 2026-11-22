package co.surumene.www.persistence;

import org.bukkit.entity.Wolf;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import co.surumene.www.individual.WonderfulWolfIndividual;

/** Exact upper WWW owner-save entrypoint; invokes nested Paper PDC and inventory writes. */
public final class WonderfulWolfLoadedIndividuals {
    public void saveAndRegister(Wolf wolf, WonderfulWolfIndividual data) {
        new CraftPersistentDataContainer().set(new NamespacedKey(), new PersistentDataType(), data);
        var equip = new org.bukkit.craftbukkit.inventory.CraftEntityEquipment();
        equip.setItemInMainHand(new org.bukkit.inventory.ItemStack());
        equip.setItemInMainHand(new org.bukkit.inventory.ItemStack(), true);
    }
}
