package co.surumene.simplecoins;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleCoinsPlugin extends JavaPlugin {
    private CoinItems items;
    private CoinRecipes recipes;
    private WalletMenu walletMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        items = new CoinItems(this);
        recipes = new CoinRecipes(this, items);
        walletMenu = new WalletMenu(items, this);
        try {
            if (Bukkit.getPluginManager().getPlugin("Nexo") == null) {
                var archive = new JavaPackBuilder(this, items).build();
                Bukkit.getPluginManager().registerEvents(new PackDelivery(this, archive), this);
                getLogger().info("Java resource pack generated: " + archive);
                if (getConfig().getString("resource-pack.public-url", "").isBlank()) {
                    getLogger().warning("Java pack not distributed: host the ZIP and set resource-pack.public-url.");
                }
            } else {
                getLogger().warning("Nexo detected. SimpleCoins does not send its own Java pack. "
                    + "Nexo import/mapping must be verified before this build is production ready.");
            }
            recipes.register();
            Bukkit.getPluginManager().registerEvents(recipes, this);
            Bukkit.getPluginManager().registerEvents(walletMenu, this);
            Bukkit.getPluginManager().registerEvents(new TraderDrops(this, items), this);
            var cmd = getCommand("simplecoins");
            if (cmd == null) throw new IllegalStateException("Missing plugin.yml command");
            var grants = new OfflineGrants(this, items);
            Bukkit.getPluginManager().registerEvents(grants, this);
            var admin = new CoinCommand(this, items, grants);
            cmd.setExecutor(admin);
            cmd.setTabCompleter(admin);
        } catch (Exception failure) {
            getLogger().severe("Failed to initialize safely: " + failure.getMessage());
            // Disabling only this plugin leaves its money usable without protection.
            Bukkit.shutdown();
        }
    }

    @Override
    public void onDisable() {
        if (recipes != null) recipes.unregister();
    }
}
