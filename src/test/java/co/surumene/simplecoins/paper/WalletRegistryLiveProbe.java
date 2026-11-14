package co.surumene.simplecoins.paper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Isolated live-server observation, not part of the production SimpleCoins JAR. */
public final class WalletRegistryLiveProbe extends JavaPlugin {
    private static final NamespacedKey ID = new NamespacedKey("simplecoins", "wallet_id");
    private static final NamespacedKey BALANCE = new NamespacedKey("simplecoins", "wallet_balance");

    @Override public void onEnable() {
        try {
            if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Not on main thread");
            var recipe = Bukkit.getRecipe(new NamespacedKey("simplecoins", "wallet"));
            if (!(recipe instanceof ShapedRecipe shaped))
                throw new IllegalStateException("Missing SimpleCoins wallet recipe");
            if (!java.util.Arrays.equals(
                    new String[]{" L ", "LIL", " L "}, shaped.getShape()))
                throw new IllegalStateException("Unexpected wallet shaped recipe");
            verifyFreshWallet(shaped.getResult(), Material.BUNDLE);
            Set<NamespacedKey> keys = new HashSet<>();
            for (DyeColor dye : DyeColor.values()) {
                NamespacedKey key = new NamespacedKey("simplecoins",
                        "wallet_dye_" + dye.name().toLowerCase(java.util.Locale.ROOT));
                if (!keys.add(key)) throw new IllegalStateException("Duplicate dye recipe " + key);
                if (!(Bukkit.getRecipe(key) instanceof ShapelessRecipe dyeRecipe))
                    throw new IllegalStateException("Missing wallet dye recipe " + key);
                Material color = Material.matchMaterial(dye.name() + "_BUNDLE");
                if (color == null) throw new IllegalStateException("Missing colored bundle for " + key);
                verifyFreshWallet(dyeRecipe.getResult(), color);
                ItemStack vanillaBundle = new ItemStack(Material.BUNDLE, 1);
                // At least one predicate must reject a native unmarked bundle.
                if (dyeRecipe.getChoiceList().stream()
                        .anyMatch(choice -> choice.test(vanillaBundle)))
                    throw new IllegalStateException("Ordinary bundle accepted as wallet: " + key);
            }
            if (keys.size() != 16) throw new IllegalStateException("Expected 16 dye colors");
            // Exercise actual Paper ItemStack cloning/PDC for a nonzero wallet.
            WalletItemCodec codec = new WalletItemCodec(UUID::randomUUID, "C");
            ItemStack created = codec.create();
            var original = codec.read(created).orElseThrow();
            ItemStack filled = codec.withBalance(created, 12345L);
            ItemStack recolored = codec.recolor(filled, Material.BLUE_BUNDLE);
            var after = codec.read(recolored).orElseThrow();
            if (!after.id().equals(original.id()) || after.balance() != 12345L
                    || recolored.getMaxStackSize() != 1)
                throw new IllegalStateException("Recolor changed durable wallet identity or balance");
            getLogger().info("PAPER_WALLET_REGISTRY_PASSED: 1 creation, 16 dyes,"
                    + " marked single-stack results; no player GUI assertion");
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "PAPER_WALLET_REGISTRY_FAILED", failure);
            Bukkit.shutdown();
            throw new IllegalStateException("Wallet registry smoke failed", failure);
        }
    }

    private static void verifyFreshWallet(ItemStack item, Material expectedType) {
        if (item.getType() != expectedType || item.getAmount() != 1
                || item.getMaxStackSize() != 1)
            throw new IllegalStateException("Invalid wallet stack/material");
        var meta = item.getItemMeta();
        if (meta == null) throw new IllegalStateException("Missing wallet ItemMeta");
        String id = meta.getPersistentDataContainer().get(ID, PersistentDataType.STRING);
        Long amount = meta.getPersistentDataContainer().get(BALANCE, PersistentDataType.LONG);
        if (id == null || !UUID.fromString(id).toString().equals(id)
                || amount == null || amount != 0L)
            throw new IllegalStateException("Invalid wallet PDC identity or initial balance");
    }
}
