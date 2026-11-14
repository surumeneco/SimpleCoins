package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinExchangeFormula;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/** Test-only Paper 26.2 probe: never include in the published SimpleCoins JAR. */
public final class CoinRecipesLiveProbe extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Not on Paper main thread");
            CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
            var formulas = CoinExchangeFormula.all();
            if (formulas.size() != 20) throw new IllegalStateException("Expected 20 exchange formulas");
            Set<NamespacedKey> seen = new HashSet<>();
            for (var formula : formulas) {
                NamespacedKey key = new NamespacedKey("simplecoins", "exchange_" + formula.id());
                if (!seen.add(key)) throw new IllegalStateException("Duplicate formula key " + key);
                Recipe recipe = Bukkit.getRecipe(key);
                if (!(recipe instanceof ShapelessRecipe shapeless))
                    throw new IllegalStateException("Missing or nonshapeless SimpleCoins recipe: " + key);
                if (codec.identify(shapeless.getResult()).orElseThrow() != formula.output()
                        || shapeless.getResult().getAmount() != formula.outputCount())
                    throw new IllegalStateException("Incorrect coin result for " + key);
                if (shapeless.getChoiceList().size() != formula.inputCount())
                    throw new IllegalStateException("Incorrect input slot count for " + key);
                ItemStack genuine = codec.create(formula.input(), 1);
                ItemStack other = codec.create(formula.output(), 1);
                ItemStack ordinary = new ItemStack(Material.PAPER, 1);
                for (RecipeChoice choice : shapeless.getChoiceList()) {
                    if (!choice.test(genuine) || choice.test(ordinary)
                            || (formula.input() != formula.output() && choice.test(other)))
                        throw new IllegalStateException("Unstable coin ingredient identity: " + key);
                }
            }
            getLogger().info("PAPER_COIN_RECIPES_REGISTERED_PASSED: 20 recipes, exact PDC,"
                    + " results and ordinary-paper exclusion; no crafting-player assertion");
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "PAPER_COIN_RECIPES_REGISTERED_FAILED", failure);
            Bukkit.shutdown();
            throw new IllegalStateException("Coin recipe registry probe failed", failure);
        }
    }
}
