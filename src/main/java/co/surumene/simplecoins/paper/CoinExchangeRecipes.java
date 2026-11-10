package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.CoinExchangeFormula;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * First Paper registration candidate for 20 equivalent shapeless exchanges.
 *
 * This listener prevents marked coins being consumed by unrelated crafting
 * recipes in the supported prepare/craft/crafter paths. RecipeChoice.ExactChoice
 * is still sensitive to metadata changes and therefore requires a follow-up
 * compatibility check before final 1.0.0 acceptance. A real monetary journal
 * service must additionally observe accepted exchanges.
 */
public final class CoinExchangeRecipes implements Listener {
    private final Plugin plugin;
    private final CoinItemCodec codec;
    private final Map<NamespacedKey, CoinExchangeFormula> formulas = new LinkedHashMap<>();
    private boolean registered;

    public CoinExchangeRecipes(Plugin plugin, CoinItemCodec codec) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /** Register all-or-none on the Paper primary thread; preserve ownership. */
    public void register() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Recipe registration must run on Paper main thread");
        if (registered) throw new IllegalStateException("Exchange recipes already registered");
        List<NamespacedKey> installed = new ArrayList<>();
        try {
            for (CoinExchangeFormula formula : CoinExchangeFormula.all()) {
                NamespacedKey key = new NamespacedKey(plugin, "exchange_" + formula.id());
                ShapelessRecipe recipe = new ShapelessRecipe(
                        key, codec.create(formula.output(), formula.outputCount()));
                for (int i = 0; i < formula.inputCount(); i++) {
                    recipe.addIngredient(new RecipeChoice.ExactChoice(codec.create(formula.input(), 1)));
                }
                if (!Bukkit.addRecipe(recipe))
                    throw new IllegalStateException("Duplicate or rejected coin recipe: " + key);
                installed.add(key);
                formulas.put(key, formula);
            }
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        } catch (RuntimeException | Error failure) {
            for (NamespacedKey key : installed) Bukkit.removeRecipe(key);
            formulas.clear();
            HandlerList.unregisterAll(this);
            throw failure;
        }
    }

    public void unregister() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Recipe removal must run on Paper main thread");
        HandlerList.unregisterAll(this);
        for (NamespacedKey key : formulas.keySet()) Bukkit.removeRecipe(key);
        formulas.clear();
        registered = false;
    }

    public int registeredCount() {
        return formulas.size();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        Recipe recipe = event.getRecipe();
        if (needsGuard(matrix, recipe) && !isApproved(recipe, matrix)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        Recipe recipe = event.getRecipe();
        if (needsGuard(matrix, recipe) && !isApproved(recipe, matrix)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter crafter)) {
            event.setCancelled(true); // Unknown crafter state; do not misreport success.
            return;
        }
        ItemStack[] matrix = crafter.getInventory().getContents();
        Recipe recipe = event.getRecipe();
        if (needsGuard(matrix, recipe) && !isApproved(recipe, matrix)) {
            event.setCancelled(true);
        }
    }

    private boolean needsGuard(ItemStack[] matrix, Recipe recipe) {
        if (isOurRecipe(recipe)) return true;
        for (ItemStack item : matrix) {
            if (codec.hasCoinMarker(item)) return true;
        }
        return false;
    }

    private boolean isOurRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed && formulas.containsKey(keyed.getKey());
    }

    /** Validate every *occupied slot*, not its stack count; Bukkit consumes one/slot. */
    public boolean isApproved(Recipe recipe, ItemStack[] matrix) {
        if (!(recipe instanceof Keyed keyed)) return false;
        CoinExchangeFormula formula = formulas.get(keyed.getKey());
        if (formula == null) return false;
        int found = 0;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (!codec.hasCoinMarker(item)) return false;
            try {
                CoinDenomination value = codec.identify(item).orElse(null);
                if (value != formula.input()) return false;
            } catch (RuntimeException badCurrency) {
                return false;
            }
            found++;
        }
        return found == formula.inputCount();
    }
}
