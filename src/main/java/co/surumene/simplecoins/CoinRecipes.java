package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.ExchangeRecipe;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.PaperCurrencyConsumption;
import io.papermc.paper.event.player.CartographyItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.Keyed;

public final class CoinRecipes implements Listener {
    private final SimpleCoinsPlugin plugin;
    private final CoinItems items;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public CoinRecipes(SimpleCoinsPlugin plugin, CoinItems items) {
        this.plugin = plugin; this.items = items;
    }

    public void register() {
        int index = 0;
        for (ExchangeRecipe exchange : ExchangeRecipe.standard()) {
            NamespacedKey key = key("exchange_" + index++);
            ShapelessRecipe recipe = new ShapelessRecipe(key, items.coin(exchange.output(), exchange.outputCount()));
            for (int n = 0; n < exchange.inputCount(); n++) {
                CoinDenomination denomination = exchange.input();
                recipe.addIngredient(RecipeChoice.predicateChoice(
                    stack -> items.denomination(stack) == denomination, items.coin(denomination, 1)));
            }
            add(recipe);
        }
        ShapedRecipe wallet = new ShapedRecipe(key("wallet"), items.newWallet());
        wallet.shape(" L ", "LIL", " L ");
        wallet.setIngredient('L', Material.LEATHER);
        wallet.setIngredient('I', Material.IRON_INGOT);
        add(wallet);
        for (DyeColor dye : DyeColor.values()) {
            Material dyeMaterial = Material.matchMaterial(dye.name() + "_DYE");
            Material bundleMaterial = Material.matchMaterial(dye.name() + "_BUNDLE");
            if (dyeMaterial == null || bundleMaterial == null) continue;
            ShapelessRecipe recolor = new ShapelessRecipe(key("wallet_dye_" + dye.name().toLowerCase()),
                    items.newWallet(bundleMaterial));
            recolor.addIngredient(RecipeChoice.predicateChoice(items::isWallet, items.newWallet()));
            recolor.addIngredient(dyeMaterial);
            add(recolor);
        }
    }

    private NamespacedKey key(String id) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        keys.add(key);
        return key;
    }

    private void add(Recipe recipe) {
        if (!Bukkit.addRecipe(recipe)) throw new IllegalStateException("Recipe registration conflict: " + ((Keyed)recipe).getKey());
    }

    public void unregister() {
        for (NamespacedKey key : keys) Bukkit.removeRecipe(key);
        keys.clear();
    }

    private boolean isExchange(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) return false;
        NamespacedKey key = keyed.getKey();
        return key.getNamespace().equals(plugin.getName().toLowerCase()) && key.getKey().startsWith("exchange_");
    }
    private boolean isNewWallet(Recipe recipe) {
        return recipe instanceof Keyed keyed
                && keyed.getKey().equals(new NamespacedKey(plugin, "wallet"));
    }

    private boolean isWalletDye(Recipe recipe) {
        if (!(recipe instanceof Keyed keyed)) return false;
        NamespacedKey key = keyed.getKey();
        return key.getNamespace().equals(plugin.getName().toLowerCase()) && key.getKey().startsWith("wallet_dye_");
    }
    private boolean anyCoin(ItemStack[] itemsIn) {
        for (ItemStack stack : itemsIn) if (items.denomination(stack) != null) return true;
        return false;
    }
    private ItemStack walletIn(ItemStack[] itemsIn) {
        for (ItemStack stack : itemsIn) if (items.isWallet(stack)) return stack;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepare(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        Recipe recipe = event.getRecipe();
        if (anyCoin(matrix) && !isExchange(recipe)) {
            event.getInventory().setResult(null);
            return;
        }
        if (isNewWallet(recipe)) event.getInventory().setResult(items.newWallet());
        ItemStack previousWallet = walletIn(matrix);
        if (previousWallet != null) {
            if (!isWalletDye(recipe)) { event.getInventory().setResult(null); return; }
            ItemStack original = event.getInventory().getResult();
            if (original == null) return;
            try {
                ItemStack recolored = previousWallet.clone();
                recolored.setType(original.getType());
                event.getInventory().setResult(items.updateWallet(recolored, items.balance(previousWallet)));
            } catch (RuntimeException failure) {
                event.getInventory().setResult(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void craft(CraftItemEvent event) {
        if (isNewWallet(event.getRecipe())) {
            // Vanilla bulk crafting clones a single result, including its UUID.
            // One wallet per craft keeps the persistent identity unique.
            if (event.isShiftClick()) { event.setCancelled(true); return; }
            event.setCurrentItem(items.newWallet());
        }
        ItemStack[] matrix = event.getInventory().getMatrix();
        if ((anyCoin(matrix) && !isExchange(event.getRecipe()))
                || (walletIn(matrix) != null && !isWalletDye(event.getRecipe()))) {
            event.setCancelled(true);
        }
    }

    // Merchant offers may match normal PAPER solely by material, not SimpleCoins PDC.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void merchantResult(InventoryClickEvent event) {
        var inventory = event.getView().getTopInventory();
        if (inventory.getType() != InventoryType.MERCHANT) return;
        if (PaperCurrencyConsumption.blocksResult("MERCHANT", event.getRawSlot(),
                items.denomination(inventory.getItem(0)) != null,
                items.denomination(inventory.getItem(1)) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void cartographyResult(CartographyItemEvent event) {
        var inventory = event.getView().getTopInventory();
        if (PaperCurrencyConsumption.blocksResult("CARTOGRAPHY", event.getRawSlot(),
                items.denomination(inventory.getItem(0)) != null,
                items.denomination(inventory.getItem(1)) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void crafter(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter block)) return;
        ItemStack[] matrix = block.getInventory().getContents();
        if (isNewWallet(event.getRecipe())) event.setResult(items.newWallet());
        if (anyCoin(matrix) && !isExchange(event.getRecipe())) event.setCancelled(true);
        // PDC-bearing wallets must not be recolored by automation until persistence is confirmed.
        if (walletIn(matrix) != null) event.setCancelled(true);
    }
}
