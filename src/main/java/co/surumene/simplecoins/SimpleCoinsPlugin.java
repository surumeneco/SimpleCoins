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
            recipes.register();
            Bukkit.getPluginManager().registerEvents(recipes, this);
            Bukkit.getPluginManager().registerEvents(walletMenu, this);
            Bukkit.getPluginManager().registerEvents(new TraderDrops(this, items), this);
            var cmd = getCommand("simplecoins");
            if (cmd == null) throw new IllegalStateException("Missing plugin.yml command");
            var admin = new CoinCommand(this, items);
            cmd.setExecutor(admin);
            cmd.setTabCompleter(admin);
        } catch (RuntimeException failure) {
            getLogger().severe("Failed to initialize safely: " + failure.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (recipes != null) recipes.unregister();
    }
}
