package co.surumene.whatawonderfulchicken.service;

import org.bukkit.entity.Chicken;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import co.surumene.whatawonderfulchicken.data.WonderfulChickenData;

/** Exact upper WWC owner-save entrypoint; invokes nested Paper PDC and inventory writes. */
public final class WonderfulChickenStore {
    public void save(Chicken chicken, WonderfulChickenData data) {
        new CraftPersistentDataContainer().set(new NamespacedKey(), new PersistentDataType(), data);
    }
}
