package co.surumene.simplecoins.a12probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.CoinExchangeFormula;
import co.surumene.simplecoins.paper.CoinExchangeRecipes;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.Keyed;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bounded CI oracle: a vanilla PAPER ingredient can match a physical coin,
 * while SimpleCoins' three crafting guards are registered.
 *
 * Does NOT simulate a successful craft, prove cancellation or trading safety.
 */
public final class VanillaChoiceProbe extends JavaPlugin {
    private static final String GUARD = "co.surumene.simplecoins.paper.CoinExchangeRecipes";

    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("PAPER_A12_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                ItemStack genuine = new CoinItemCodec(CoinItemCodec.defaultNames())
                        .create(CoinDenomination.fromValue(5000), 1);
                CoinDenomination observed = new CoinItemCodec(CoinItemCodec.defaultNames())
                        .identify(genuine).orElseThrow();
                if (observed.value() != 5000)
                    throw new IllegalStateException("Authentic currency codec not present");
                List<String> vanillaMatches = new ArrayList<>();
                var recipes = Bukkit.recipeIterator();
                while (recipes.hasNext()) {
                    Recipe recipe = recipes.next();
                    if (!(recipe instanceof Keyed keyed)
                            || !"minecraft".equals(keyed.getKey().getNamespace())) continue;
                    List<RecipeChoice> choices = new ArrayList<>();
                    if (recipe instanceof ShapedRecipe shaped) {
                        choices.addAll(shaped.getChoiceMap().values());
                    } else if (recipe instanceof ShapelessRecipe shapeless) {
                        choices.addAll(shapeless.getChoiceList());
                    }
                    for (RecipeChoice choice : choices) {
                        if (choice instanceof RecipeChoice.MaterialChoice && choice.test(genuine)) {
                            vanillaMatches.add(keyed.getKey().toString());
                            break;
                        }
                    }
                }
                if (vanillaMatches.isEmpty())
                    throw new IllegalStateException("No vanilla PAPER MaterialChoice matched genuine coin");
                getLogger().info("PAPER_A12_VANILLA_MATERIAL_CHOICE_MATCHES_CURRENCY_PASSED"
                        + " count=" + vanillaMatches.size()
                        + " recipes=" + vanillaMatches.stream().limit(12).toList());

                requireListener("PrepareItemCraftEvent", PrepareItemCraftEvent.getHandlerList());
                requireListener("CraftItemEvent", CraftItemEvent.getHandlerList());
                requireListener("CrafterCraftEvent", CrafterCraftEvent.getHandlerList());
                getLogger().info("PAPER_A12_THREE_EXISTING_CRAFT_GUARDS_REGISTERED_PASSED");
                verifyRecipeIdentityBoundary();
                Bukkit.shutdown();
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "PAPER_A12_PROBE_FAILED", error);
                Bukkit.shutdown();
            }
        }, 50L);
    }

    /**
     * Paper's actual recipe registry and the already installed production
     * CraftItemEvent listener. No product recipe or wallet changes survive
     * this bounded disposable server test.
     */
    private void verifyRecipeIdentityBoundary() {
        Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        if (product == null || !product.isEnabled())
            throw new IllegalStateException("Product plugin not active in Paper registry");
        CoinExchangeRecipes guard = null;
        for (RegisteredListener registration :
                CraftItemEvent.getHandlerList().getRegisteredListeners()) {
            if (registration.getPlugin() == product
                    && registration.getListener() instanceof CoinExchangeRecipes listener) {
                if (guard != null) throw new IllegalStateException("Duplicate registered currency guard");
                guard = listener;
            }
        }
        if (guard == null) throw new IllegalStateException("Actual product recipe guard not registered");
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack[] adversarialMatrix = null;
        Recipe replaceTarget = null;
        NamespacedKey replaceKey = null;

        int matched = 0;
        for (CoinExchangeFormula formula : CoinExchangeFormula.all()) {
            NamespacedKey key = new NamespacedKey(product, "exchange_" + formula.id());
            Recipe actual = Bukkit.getRecipe(key);
            if (!(actual instanceof ShapelessRecipe)
                    || actual.getResult().getAmount() != formula.outputCount()
                    || coins.identify(actual.getResult()).orElse(null) != formula.output()) {
                throw new IllegalStateException("Registered monetary result differs from formula " + key);
            }
            ItemStack[] matrix = new ItemStack[9];
            for (int i = 0; i < formula.inputCount(); i++) {
                matrix[i] = coins.create(formula.input(), 1);
            }
            if (!guard.isApproved(actual, matrix))
                throw new IllegalStateException("Legitimate registered exchange rejected: " + key);
            ItemStack[] badMatrix = matrix.clone();
            badMatrix[0] = coins.create(
                    formula.input() == CoinDenomination.TEN_THOUSAND
                            ? CoinDenomination.ONE : CoinDenomination.TEN_THOUSAND, 1);
            if (guard.isApproved(actual, badMatrix))
                throw new IllegalStateException("Wrong denomination authorized: " + key);
            if (replaceTarget == null) {
                replaceTarget = actual;
                replaceKey = key;
                adversarialMatrix = matrix;
            }
            matched++;
        }
        if (matched != 20 || replaceTarget == null)
            throw new IllegalStateException("Expected all 20 actual exchange recipes");
        getLogger().info("PAPER_A12_ALL_TWENTY_REAL_EXCHANGE_INPUT_OUTPUT_APPROVALS_PASSED");

        // The only recipe removed is restored in finally. No actual crafting
        // or player resources: this is a key/result trust-boundary experiment.
        if (!Bukkit.removeRecipe(replaceKey))
            throw new IllegalStateException("Cannot remove disposable copy of recipe " + replaceKey);
        try {
            ShapelessRecipe bogus = new ShapelessRecipe(replaceKey, new ItemStack(Material.STONE, 1));
            for (ItemStack slot : adversarialMatrix) {
                if (slot != null) bogus.addIngredient(Material.PAPER);
            }
            boolean installed = Bukkit.addRecipe(bogus);
            if (!installed) {
                getLogger().info("PAPER_A12_RECIPE_SAME_KEY_SUBSTITUTION_REJECTED_BY_PAPER");
                return;
            }
            Recipe observed = Bukkit.getRecipe(replaceKey);
            if (observed == null || observed.getResult().getType() != Material.STONE)
                throw new IllegalStateException("Paper recipe registry did not expose substituted result");
            boolean approved = guard.isApproved(observed, adversarialMatrix);
            if (approved) {
                getLogger().warning("PAPER_A12_KEY_ONLY_APPROVAL_VALUE_LOSS_COUNTEREXAMPLE"
                        + " key=" + replaceKey + " result=" + observed.getResult().getType()
                        + " guardedInputCoins=" + adversarialMatrix[0].getType());
            } else {
                getLogger().info("PAPER_A12_SUBSTITUTED_OUTPUT_REJECTED_BY_GUARD");
            }
        } finally {
            Bukkit.removeRecipe(replaceKey);
            if (!Bukkit.addRecipe(replaceTarget))
                throw new IllegalStateException("Failed to restore exact original recipe " + replaceKey);
            Recipe restored = Bukkit.getRecipe(replaceKey);
            if (restored == null
                    || restored.getResult().getType() == Material.STONE
                    || !guard.isApproved(restored, adversarialMatrix))
                throw new IllegalStateException("Monetary recipe not restored after bounded probe");
            getLogger().info("PAPER_A12_DISPOSABLE_RECIPE_RESTORED_PASSED");
        }
        getLogger().info("PAPER_A12_RECIPE_REGISTRY_OUTPUT_INTEGRITY_OBSERVED");
    }

    private static void requireListener(String name, HandlerList list) {
        for (RegisteredListener registration : list.getRegisteredListeners()) {
            if (GUARD.equals(registration.getListener().getClass().getName())
                    && registration.getPlugin().isEnabled()) {
                return;
            }
        }
        throw new IllegalStateException("Missing registered SimpleCoins guard for " + name);
    }
}
