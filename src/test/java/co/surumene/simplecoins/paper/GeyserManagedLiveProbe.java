package co.surumene.simplecoins.paper;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * CI-only replacement for the Geyser-Spigot plugin data-folder contract.
 * It is NOT a Geyser protocol implementation nor BE client verification.
 */
public final class GeyserManagedLiveProbe extends JavaPlugin {
    @Override public void onEnable() {
        getConfig().set("gameplay.enable-custom-content", true);
        saveConfig();
        getLogger().info("SIMPLECOINS_GEYSER_TEST_PROVIDER_READY: config written");
    }
}
