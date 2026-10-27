package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CoinItems {
    private final SimpleCoinsPlugin plugin;
    private final NamespacedKey denomKey;
    private final NamespacedKey walletKey;
    private final NamespacedKey walletBalanceKey;

    public CoinItems(SimpleCoinsPlugin plugin) {
        this.plugin = plugin;
        denomKey = new NamespacedKey(plugin, "denomination");
        walletKey = new NamespacedKey(plugin, "wallet_id");
        walletBalanceKey = new NamespacedKey(plugin, "wallet_balance");
    }

    public String currency() {
        return plugin.getConfig().getString("currency.unit", "C");
    }

    public String name(CoinDenomination d) {
        return plugin.getConfig().getString("coins." + d.value() + ".name", d.value() + " Coin");
    }

    public ItemStack coin(CoinDenomination d, int count) {
        if (count < 1 || count > 64) throw new IllegalArgumentException("Coin stack 1..64");
        ItemStack stack = new ItemStack(Material.PAPER, count);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemName(name(d));
        meta.setItemModel(new NamespacedKey(plugin, "coin_" + d.value()));
        meta.setMaxStackSize(64);
        meta.getPersistentDataContainer().set(denomKey, PersistentDataType.INTEGER, d.value());
        stack.setItemMeta(meta);
        return stack;
    }

    public CoinDenomination denomination(ItemStack stack) {
        if (stack == null || stack.getType() != Material.PAPER || !stack.hasItemMeta()) return null;
        Integer value = stack.getItemMeta().getPersistentDataContainer().get(denomKey, PersistentDataType.INTEGER);
        if (value == null) return null;
        try { return CoinDenomination.fromValue(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public boolean isWallet(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) return false;
        String material = stack.getType().name();
        if (!material.equals("BUNDLE") && !material.endsWith("_BUNDLE")) return false;
        String id = stack.getItemMeta().getPersistentDataContainer().get(walletKey, PersistentDataType.STRING);
        return id != null && !id.isEmpty();
    }

    public ItemStack newWallet() { return newWallet(Material.BUNDLE); }

    public ItemStack newWallet(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(walletKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(walletBalanceKey, PersistentDataType.LONG, 0L);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return updateWallet(item, 0L);
    }

    public long balance(ItemStack wallet) {
        if (!isWallet(wallet)) throw new IllegalArgumentException("Not a wallet");
        Long balance = wallet.getItemMeta().getPersistentDataContainer().get(walletBalanceKey, PersistentDataType.LONG);
        if (balance == null || balance < 0) throw new IllegalStateException("Corrupt wallet balance");
        return balance;
    }

    public String walletId(ItemStack wallet) {
        return isWallet(wallet) ? wallet.getItemMeta().getPersistentDataContainer().get(walletKey, PersistentDataType.STRING) : null;
    }

    public ItemStack updateWallet(ItemStack source, long balance) {
        if (!isWallet(source) || balance < 0) throw new IllegalArgumentException("Invalid wallet update");
        ItemStack item = source.clone();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(walletBalanceKey, PersistentDataType.LONG, balance);
        meta.setItemName("Wallet");
        meta.setLore(List.of("Balance: " + balance + currency(), "Capacity: " +
                plugin.getConfig().getLong("wallet.max-balance", 640000L) + currency()));
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    public long maximumBalance() {
        long limit = plugin.getConfig().getLong("wallet.max-balance", 640000L);
        if (limit < 0) throw new IllegalStateException("Negative wallet limit");
        return limit;
    }
}
