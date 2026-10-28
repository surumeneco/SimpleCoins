package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import java.math.BigInteger;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only traversal of a SINGLE ItemStack and its nested inventories.
 * Ownership and on-disk region discovery are deliberately handled elsewhere.
 *
 * Never convert unreadable/unknown/corrupt values to zero: a failed settlement
 * must remain uncommitted rather than publish a plausible but incomplete total.
 */
public final class ItemValueWalker {
    private static final int MAX_DEPTH = 32;
    private final CoinItems coins;

    public ItemValueWalker(CoinItems coins) {
        this.coins = Objects.requireNonNull(coins);
    }

    public BigInteger total(ItemStack[] inventory) {
        Objects.requireNonNull(inventory);
        BigInteger result = BigInteger.ZERO;
        for (ItemStack stack : inventory) result = result.add(value(stack, 0));
        return result;
    }

    public BigInteger value(ItemStack item) {
        return value(item, 0);
    }

    private BigInteger value(ItemStack item, int depth) {
        if (item == null || item.getType() == Material.AIR) return BigInteger.ZERO;
        if (depth > MAX_DEPTH) throw new IllegalStateException("Nested container depth exceeds " + MAX_DEPTH);
        int quantity = item.getAmount();
        if (quantity <= 0) throw new IllegalStateException("Non-positive stack quantity");
        BigInteger quantityValue = BigInteger.valueOf(quantity);

        CoinDenomination denomination = coins.denomination(item);
        if (denomination != null)
            return BigInteger.valueOf(denomination.value()).multiply(quantityValue);

        if (coins.isWallet(item)) {
            if (quantity != 1) throw new IllegalStateException("Stacked wallet is invalid");
            return BigInteger.valueOf(coins.balance(item));
        }

        if (!item.hasItemMeta()) return BigInteger.ZERO;
        ItemMeta metadata = item.getItemMeta();
        BigInteger contents = BigInteger.ZERO;
        if (metadata instanceof BundleMeta bundle) {
            for (ItemStack inner : bundle.getItems()) contents = contents.add(value(inner, depth + 1));
        } else if (metadata instanceof BlockStateMeta block &&
                block.getBlockState() instanceof Container container) {
            for (ItemStack inner : container.getSnapshotInventory().getContents())
                contents = contents.add(value(inner, depth + 1));
        }
        return contents.multiply(quantityValue);
    }
}
