package co.surumene.simplecoins.a05probe;

import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.plugin.inventory.WonderfulStorageReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Probe ONLY. Uses shipped WCL/WWC/WWW/WGL distribution JARs on real Paper.
 * Never reads an actual monetary owner or grants admission.
 */
public final class WclServicesProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("PAPER_A05_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                for (String pluginName : new String[]{
                        "WonderfulGenomeLib", "WonderfulCoreLibrary",
                        "WhatAWonderfulChicken", "WhatAWonderfulWolf", "SimpleCoins"}) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
                    if (plugin == null || !plugin.isEnabled())
                        throw new IllegalStateException("Expected active provider dependency: " + pluginName);
                }
                var registrations = Bukkit.getServicesManager()
                        .getRegistrations(WonderfulStorageReader.class);
                Map<String, WonderfulStorageReader> selected = new HashMap<>();
                for (RegisteredServiceProvider<WonderfulStorageReader> registered : registrations) {
                    Plugin owner = registered.getPlugin();
                    WonderfulStorageReader reader = registered.getProvider();
                    if (!owner.isEnabled()) throw new IllegalStateException("Disabled provider owner");
                    String id = reader.providerId();
                    String expectedOwner = switch (id) {
                        case "wwc" -> "WhatAWonderfulChicken";
                        case "www" -> "WhatAWonderfulWolf";
                        default -> throw new IllegalStateException("Unexpected storage provider: " + id);
                    };
                    if (!owner.getName().equals(expectedOwner))
                        throw new IllegalStateException("Wrong owner " + owner.getName() + " for " + id);
                    if (selected.putIfAbsent(id, reader) != null)
                        throw new IllegalStateException("Duplicate providerId " + id);
                }
                if (!selected.keySet().equals(Set.of("wwc", "www"))
                        || registrations.size() != 2)
                    throw new IllegalStateException(
                            "Both registered storage providers are required, got " + selected.keySet());
                getLogger().info("PAPER_A05_BOTH_PACKAGED_STORAGE_SERVICES_REGISTERED_PASSED"
                        + " providers=" + selected.keySet());

                World world = Bukkit.getWorld("world");
                if (world == null) throw new IllegalStateException("Missing test overworld");
                PersistentDataContainer blank = new ItemStack(Material.PAPER)
                        .getItemMeta().getPersistentDataContainer();
                UUID unknown = UUID.fromString("00000000-0000-0000-0000-00000000a005");
                for (String id : Set.of("wwc", "www")) {
                    WonderfulStorageReader provider = selected.get(id);
                    if (!(provider.readLoaded(unknown) instanceof StorageReadResult.NotFound))
                        throw new IllegalStateException("Untracked loaded entity not NotFound: " + id);
                    if (!(provider.readStored(unknown, world.getUID(), Optional.empty(), blank)
                            instanceof StorageReadResult.NotFound))
                        throw new IllegalStateException("Untyped stored entity not NotFound: " + id);
                }
                getLogger().info("PAPER_A05_BOTH_PACKAGED_STORAGE_READERS_EMPTY_ENTITY_PASSED");
                Bukkit.shutdown();
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "PAPER_A05_SERVICES_PROBE_FAILED", failure);
                Bukkit.shutdown();
            }
        }, 50L);
    }
}
