package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WalletBalance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Strict physical wallet identity and balance codec. Coin denomination PDC is
 * owned by CoinItemCodec; wallet UUID and value are independent stable keys.
 *
 * This does not deduplicate externally cloned wallet UUIDs across world data.
 * Full-world physical identity reconciliation is owned by the settlement task.
 */
public final class WalletItemCodec {
    public static final NamespacedKey WALLET_ID =
            new NamespacedKey("simplecoins", "wallet_id");
    public static final NamespacedKey WALLET_BALANCE =
            new NamespacedKey("simplecoins", "wallet_balance");

    public record Wallet(UUID id, long balance) {
        public Wallet {
            Objects.requireNonNull(id, "id");
            if (balance < 0) throw new IllegalArgumentException("Negative wallet balance");
        }
    }

    private final Supplier<UUID> newId;
    private final String currencyUnit;

    public WalletItemCodec(Supplier<UUID> newId, String currencyUnit) {
        this.newId = Objects.requireNonNull(newId, "newId");
        this.currencyUnit = Objects.requireNonNull(currencyUnit, "currencyUnit");
        if (currencyUnit.isBlank()) throw new IllegalArgumentException("Empty currency unit");
    }

    public boolean hasWalletMarker(ItemStack item) {
        return item != null && !item.getType().isAir()
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(WALLET_ID);
    }

    public Optional<Wallet> read(ItemStack item) {
        if (!hasWalletMarker(item)) return Optional.empty();
        if (!isBundle(item.getType()) || item.getAmount() != 1)
            throw new IllegalStateException("Currency wallet has invalid material or count");
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        // Native Bundle contents bypass the numeric wallet accounting model.
        // Reject hidden physical assets rather than normalizing or dropping
        // the original items during recoloring or balance updates.
        if (!(meta instanceof BundleMeta bundleMeta) || !bundleMeta.getItems().isEmpty())
            throw new IllegalStateException("Currency wallet must have an empty Bundle container");
        // A custom stack cap above one permits stacking/splitting multiple
        // identical PDC UUIDs and duplicating their numeric balances.
        if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != 1)
            throw new IllegalStateException("Currency wallet must have max stack size one");
        String rawId = meta.getPersistentDataContainer().get(WALLET_ID, PersistentDataType.STRING);
        Long rawBalance = meta.getPersistentDataContainer().get(WALLET_BALANCE, PersistentDataType.LONG);
        if (rawId == null || rawBalance == null || rawBalance < 0)
            throw new IllegalStateException("Invalid currency wallet PDC");
        UUID id;
        try {
            id = UUID.fromString(rawId);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalStateException("Invalid currency wallet UUID", malformed);
        }
        if (!id.toString().equals(rawId))
            throw new IllegalStateException("Noncanonical currency wallet UUID");
        return Optional.of(new Wallet(id, rawBalance));
    }

    public ItemStack create() {
        return create(Material.BUNDLE);
    }

    public ItemStack create(Material material) {
        if (!isBundle(material))
            throw new IllegalArgumentException("Wallet must use a bundle material");
        UUID id = Objects.requireNonNull(newId.get(), "Wallet UUID generator returned null");
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta(), "Missing wallet ItemMeta");
        meta.getPersistentDataContainer().set(WALLET_ID, PersistentDataType.STRING, id.toString());
        meta.getPersistentDataContainer().set(WALLET_BALANCE, PersistentDataType.LONG, 0L);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return withBalance(item, 0L);
    }

    public ItemStack withBalance(ItemStack source, long nextBalance) {
        Wallet identity = read(source).orElseThrow(
                () -> new IllegalArgumentException("Expected marked wallet"));
        if (nextBalance < 0) throw new IllegalArgumentException("Negative wallet balance");
        ItemStack updated = source.clone();
        ItemMeta meta = Objects.requireNonNull(updated.getItemMeta());
        meta.getPersistentDataContainer().set(WALLET_BALANCE, PersistentDataType.LONG, nextBalance);
        meta.displayName(Component.text("Wallet", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Balance: " + nextBalance + currencyUnit)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setMaxStackSize(1);
        updated.setItemMeta(meta);
        updated.setAmount(1);
        if (!read(updated).orElseThrow().id().equals(identity.id()))
            throw new IllegalStateException("Wallet update changed physical identity");
        return updated;
    }

    public ItemStack recolor(ItemStack wallet, Material color) {
        Wallet identity = read(wallet).orElseThrow(
                () -> new IllegalArgumentException("Expected marked wallet"));
        if (!isBundle(color))
            throw new IllegalArgumentException("Invalid wallet dye output");
        ItemStack recolored = wallet.clone();
        recolored.setType(color);
        if (!read(recolored).orElseThrow().equals(identity))
            throw new IllegalStateException("Recolor changed wallet value or identity");
        return recolored;
    }

    public static boolean isBundle(Material material) {
        return material != null && (material == Material.BUNDLE
                || material.name().endsWith("_BUNDLE"));
    }

    public WalletBalance balance(ItemStack wallet) {
        return new WalletBalance(read(wallet).orElseThrow().balance());
    }
}
