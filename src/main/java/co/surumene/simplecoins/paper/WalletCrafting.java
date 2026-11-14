package co.surumene.simplecoins.paper;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wallet crafting and recoloring are main-thread, single-craft transactions.
 * Automated and shift-mass wallet crafting remain forbidden until independent
 * durable identity replay and crash consistency are verified.
 */
public final class WalletCrafting implements Listener {
    private final Plugin plugin;
    private final WalletItemCodec codec;
    private final NamespacedKey newWalletKey;
    private final Map<NamespacedKey, Material> dyeOutputs = new LinkedHashMap<>();
    private final List<NamespacedKey> owned = new ArrayList<>();
    private boolean registered;

    public WalletCrafting(Plugin plugin, WalletItemCodec codec) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.codec = Objects.requireNonNull(codec, "codec");
        newWalletKey = new NamespacedKey(plugin, "wallet");
    }

    public void register() {
        if (!Bukkit.isPrimaryThread() || registered)
            throw new IllegalStateException("Wallet registration requires unused main-thread context");
        try {
            ShapedRecipe creation = new ShapedRecipe(newWalletKey, codec.create());
            creation.shape(" L ", "LIL", " L ");
            creation.setIngredient('L', Material.LEATHER);
            creation.setIngredient('I', Material.IRON_INGOT);
            add(creation);
            for (DyeColor dye : DyeColor.values()) {
                Material dyeItem = Material.matchMaterial(dye.name() + "_DYE");
                Material color = Material.matchMaterial(dye.name() + "_BUNDLE");
                if (dyeItem == null || color == null) continue;
                NamespacedKey key = new NamespacedKey(plugin,
                        "wallet_dye_" + dye.name().toLowerCase(java.util.Locale.ROOT));
                ShapelessRecipe recipe = new ShapelessRecipe(key, codec.create(color));
                recipe.addIngredient(RecipeChoice.predicateChoice(
                        item -> validWallet(item), codec.create()));
                recipe.addIngredient(dyeItem);
                add(recipe);
                dyeOutputs.put(key, color);
            }
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        } catch (RuntimeException | Error fail) {
            unregister();
            throw fail;
        }
    }

    private void add(Recipe recipe) {
        NamespacedKey key = ((Keyed) recipe).getKey();
        if (!Bukkit.addRecipe(recipe))
            throw new IllegalStateException("Rejected wallet recipe " + key);
        owned.add(key);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        for (NamespacedKey key : owned) Bukkit.removeRecipe(key);
        owned.clear();
        dyeOutputs.clear();
        registered = false;
    }

    public int registeredCount() { return owned.size(); }

    private boolean validWallet(ItemStack item) {
        try { return codec.read(item).isPresent(); }
        catch (RuntimeException invalid) { return false; }
    }

    private boolean hasMarkedWallet(ItemStack[] matrix) {
        for (ItemStack item : matrix) if (codec.hasWalletMarker(item)) return true;
        return false;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private static boolean newWalletMatrix(ItemStack[] matrix) {
        if (matrix.length != 9) return false;
        for (int i = 0; i < matrix.length; i++) {
            Material expected = switch (i) {
                case 1, 3, 5, 7 -> Material.LEATHER;
                case 4 -> Material.IRON_INGOT;
                default -> Material.AIR;
            };
            if (expected == Material.AIR) {
                if (!empty(matrix[i])) return false;
            } else if (empty(matrix[i]) || matrix[i].getType() != expected
                    || matrix[i].getAmount() < 1) return false;
        }
        return true;
    }

    private ItemStack sourceWallet(ItemStack[] matrix, Material color) {
        ItemStack wallet = null;
        int dyeCount = 0;
        for (ItemStack item : matrix) {
            if (empty(item)) continue;
            if (codec.hasWalletMarker(item)) {
                if (wallet != null || !validWallet(item)) return null;
                wallet = item;
            } else if (item.getType().name().endsWith("_DYE") &&
                    Material.matchMaterial(item.getType().name().replace("_DYE", "_BUNDLE")) == color) {
                dyeCount++;
            } else return null;
        }
        return wallet != null && dyeCount == 1 ? wallet : null;
    }

    private NamespacedKey keyOf(Recipe recipe) {
        return recipe instanceof Keyed keyed ? keyed.getKey() : null;
    }

    private ItemStack prepared(Recipe recipe, ItemStack[] matrix) {
        NamespacedKey key = keyOf(recipe);
        if (newWalletKey.equals(key) && recipe instanceof ShapedRecipe) {
            return newWalletMatrix(matrix) ? codec.create() : null;
        }
        Material color = dyeOutputs.get(key);
        if (color != null && recipe instanceof ShapelessRecipe) {
            ItemStack existing = sourceWallet(matrix, color);
            return existing == null ? null : codec.recolor(existing, color);
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        NamespacedKey key = keyOf(event.getRecipe());
        if (newWalletKey.equals(key) || dyeOutputs.containsKey(key)
                || hasMarkedWallet(matrix))
            event.getInventory().setResult(prepared(event.getRecipe(), matrix));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        NamespacedKey key = keyOf(event.getRecipe());
        if (newWalletKey.equals(key) || dyeOutputs.containsKey(key) || hasMarkedWallet(matrix)) {
            // Never clone one UUID via mass crafting or allow unrelated recipes
            // to convert a wallet into an ordinary material.
            if (event.isShiftClick()) { event.setCancelled(true); return; }
            try {
                ItemStack output = prepared(event.getRecipe(), matrix);
                if (output == null) { event.setCancelled(true); return; }
                event.setCurrentItem(output);
            } catch (RuntimeException invalid) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (newWalletKey.equals(keyOf(recipe)) || dyeOutputs.containsKey(keyOf(recipe))) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlock().getState() instanceof Crafter crafter
                && hasMarkedWallet(crafter.getInventory().getContents()))
            event.setCancelled(true);
    }
}
