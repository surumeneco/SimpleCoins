package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Positive-only commander/follow-target evidence from the current WWC PDC
 * contract. Missing follow_target does not prove ownerlessness. This adapter
 * reads loaded chickens only and does not migrate or modify WWC state.
 */
public final class PaperWonderfulChickenOwnerAdapter {
    private PaperWonderfulChickenOwnerAdapter() {}

    public static CompanionOwnerResolution.Identity readPaper(Chicken chicken) {
        if (chicken == null)
            return CompanionOwnerResolution.unknown();
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("WhatAWonderfulChicken");
            if (plugin == null || !plugin.isEnabled())
                return CompanionOwnerResolution.unknown();
            var pdc = chicken.getPersistentDataContainer();
            return read(
                    pdc.get(new NamespacedKey(plugin, "type"), PersistentDataType.STRING),
                    pdc.get(new NamespacedKey(plugin, "follow_target"), PersistentDataType.STRING),
                    pdc.get(new NamespacedKey(plugin, "data_version"), PersistentDataType.INTEGER));
        } catch (RuntimeException unavailable) {
            return CompanionOwnerResolution.unknown();
        }
    }

    /** Testable evidence boundary for WWC's currently persisted PDC keys. */
    public static CompanionOwnerResolution.Identity read(
            String marker, String followTarget, Integer dataVersion) {
        if (!"wonderful_chicken".equals(marker) || dataVersion == null ||
                dataVersion < 2 || dataVersion > 4 || followTarget == null)
            return CompanionOwnerResolution.unknown();
        try {
            UUID id = UUID.fromString(followTarget);
            return id.toString().equals(followTarget)
                    ? CompanionOwnerResolution.known(id)
                    : CompanionOwnerResolution.unknown();
        } catch (IllegalArgumentException invalid) {
            return CompanionOwnerResolution.unknown();
        }
    }
}
