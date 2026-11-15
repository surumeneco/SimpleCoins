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
            // Real Paper ItemFactory/ItemStack serialization smoke for the
            // offline grant planner. Standalone JUnit cannot construct Paper
            // item stacks without the server's ItemFactory.
            ItemStack[] storage = new ItemStack[36];
            storage[6] = codec.create(co.surumene.simplecoins.domain.CoinDenomination.FIVE, 1);
            var originalSlots = PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(storage);
            if (!originalSlots.equals(PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(
                    storage.clone())))
                throw new IllegalStateException("Unstable Paper ItemStack fingerprint");
            storage[6] = storage[6].clone();
            storage[6].setAmount(2);
            if (originalSlots.equals(PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(storage)))
                throw new IllegalStateException("Grant slot count drift was not detected");
            storage[6] = codec.create(
                    co.surumene.simplecoins.domain.CoinDenomination.HUNDRED, 1);
            if (originalSlots.equals(PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(storage)))
                throw new IllegalStateException("Grant slot PDC/denomination drift was not detected");
            storage[6] = null;
            if (originalSlots.equals(PaperGrantInventorySlotPlan.StorageSnapshot.fromPaper(storage)))
                throw new IllegalStateException("Grant slot removal was not detected");
            getLogger().info("PAPER_GRANT_STORAGE_FINGERPRINT_PASSED: genuine coin count,"
                    + " PDC denomination, disappearance and stable snapshots");

            getLogger().info("PAPER_COIN_RECIPES_REGISTERED_PASSED: 20 recipes, exact PDC,"
                    + " results and ordinary-paper exclusion; no crafting-player assertion");
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "PAPER_COIN_RECIPES_REGISTERED_FAILED", failure);
            Bukkit.shutdown();
            throw new IllegalStateException("Coin recipe registry probe failed", failure);
        }
    }
}
