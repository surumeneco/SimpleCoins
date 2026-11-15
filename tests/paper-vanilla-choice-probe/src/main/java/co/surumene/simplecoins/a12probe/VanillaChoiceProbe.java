package co.surumene.simplecoins.a12probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.event.HandlerList;
import org.bukkit.event.RegisteredListener;
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
                Bukkit.shutdown();
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "PAPER_A12_PROBE_FAILED", error);
                Bukkit.shutdown();
            }
        }, 50L);
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
