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

            // Physical coin materialization is tested on authentic Paper because
            // standalone domain JUnit has no Bukkit ItemFactory.
            var issuance = new co.surumene.simplecoins.domain.GrantLedger(
                    getDataFolder().toPath().resolve("grant-materializer-proof"));
            var recipient = java.util.UUID.randomUUID();
            var grant = issuance.reserve(recipient, 5200, "ci");
            ItemStack[] deliveryStorage = new ItemStack[36];
            for (int i = 1; i < deliveryStorage.length; i++)
                deliveryStorage[i] = new ItemStack(Material.STONE, 1);
            var originalReceipt = co.surumene.simplecoins.domain.GrantReceiptState.empty();
            var firstDelivery = PaperGrantInventorySlotPlan.stage(
                    issuance, recipient, grant, originalReceipt, deliveryStorage).orElseThrow();
            ItemStack[] projected = PaperGrantInventoryMaterializer.buildCopy(
                    firstDelivery, originalReceipt, deliveryStorage, codec);
            if (deliveryStorage[0] != null || projected[0] == null
                    || codec.identify(projected[0]).orElseThrow()
                            != co.surumene.simplecoins.domain.CoinDenomination.FIVE_THOUSAND
                    || projected[0].getAmount() != 1
                    || firstDelivery.nextReceipt().partialValue() != 5000)
                throw new IllegalStateException("Materializer altered original storage or value");
            if (!deliveryStorage[35].equals(projected[35]))
                throw new IllegalStateException("Materializer overwrote occupied storage");
            boolean refused = false;
            deliveryStorage[0] = new ItemStack(Material.DIRT, 1);
            try {
                PaperGrantInventoryMaterializer.buildCopy(
                        firstDelivery, originalReceipt, deliveryStorage, codec);
            } catch (IllegalStateException expected) {
                refused = true;
            }
            if (!refused)
                throw new IllegalStateException("Stale physical grant plan was accepted");
            deliveryStorage[0] = projected[0];
            deliveryStorage[1] = null;
            var resumed = PaperGrantInventorySlotPlan.stage(issuance, recipient, grant,
                    firstDelivery.nextReceipt(), deliveryStorage).orElseThrow();
            ItemStack[] finalInventory = PaperGrantInventoryMaterializer.buildCopy(
                    resumed, firstDelivery.nextReceipt(), deliveryStorage, codec);
            if (deliveryStorage[1] != null
                    || codec.identify(finalInventory[1]).orElseThrow()
                            != co.surumene.simplecoins.domain.CoinDenomination.HUNDRED
                    || finalInventory[1].getAmount() != 2
                    || resumed.nextReceipt().completedSequence() != grant.sequence())
                throw new IllegalStateException("Materializer lost remaining grant value");
            if (issuance.pending(recipient).size() != 1)
                throw new IllegalStateException("A staged grant was acknowledged or issued");
            getLogger().info("PAPER_GRANT_COIN_MATERIALIZATION_PASSED: copy-only 5200C,"
                    + " genuine coin PDC, full-slot pause, stale-state fail-closed");

            getLogger().info("PAPER_COIN_RECIPES_REGISTERED_PASSED: 20 recipes, exact PDC,"
                    + " results and ordinary-paper exclusion; no crafting-player assertion");
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "PAPER_COIN_RECIPES_REGISTERED_FAILED", failure);
            Bukkit.shutdown();
            throw new IllegalStateException("Coin recipe registry probe failed", failure);
        }
    }
}
