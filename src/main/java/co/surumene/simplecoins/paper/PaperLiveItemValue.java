package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.NbtReader;
import co.surumene.simplecoins.domain.StoredItemValue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Read-only bridge from actual loaded Paper ItemStacks to the same monetary
 * value parser used for authoritative saved NBT. Never trusts display names.
 *
 * This is NOT a source inventory, owner resolver, consistent world snapshot,
 * or an admission gate. The calling coordinator must deduplicate saved/live
 * sources and establish an unchanging source generation before publishing.
 */
public final class PaperLiveItemValue {
    private PaperLiveItemValue() {}

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Live Paper inventory reads require the primary thread");
    }

    private static void limits(int maxSerializedBytes, int maxInflatedBytes) {
        if (maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Positive serialized/inflated budgets required");
    }

    /** A single stack, including a wallet's balance and nested item contents. */
    public static BigInteger stack(ItemStack stack, int maxSerializedBytes,
                                   int maxInflatedBytes) throws IOException {
        return stack(stack, maxSerializedBytes, maxInflatedBytes, new HashSet<>());
    }

    /**
     * A shared wallet-ID generation may be provided for an explicitly
     * selected inventory, without claiming a globally coherent world snapshot.
     */
    private static BigInteger stack(ItemStack stack, int maxSerializedBytes,
                                   int maxInflatedBytes, Set<UUID> seenWallets) throws IOException {
        limits(maxSerializedBytes, maxInflatedBytes);
        requireMainThread();
        if (stack == null || stack.getType().isAir()) return BigInteger.ZERO;
        final byte[] encoded;
        try {
            encoded = stack.serializeAsBytes();
        } catch (RuntimeException invalid) {
            throw new IOException("Paper ItemStack could not be serialized", invalid);
        }
        if (encoded.length < 2 || encoded.length > maxSerializedBytes)
            throw new IOException("Paper ItemStack exceeds serialized byte budget");
        InputStream raw = new ByteArrayInputStream(encoded);
        InputStream decoded = (encoded[0] == (byte) 0x1f && encoded[1] == (byte) 0x8b)
                ? new GZIPInputStream(raw) : raw;
        try (decoded) {
            return StoredItemValue.value(NbtReader.read(decoded, maxInflatedBytes), seenWallets);
        }
    }

    /**
     * Scan an explicit live inventory exactly once. A player, block or entity
     * source must first be uniquely selected by the higher-level coordinator.
     */
    public static BigInteger inventory(Inventory inventory, int maxSlots,
                                       int maxSerializedBytes,
                                       int maxInflatedBytes) throws IOException {
        Objects.requireNonNull(inventory, "inventory");
        if (maxSlots < 1) throw new IllegalArgumentException("Positive slot budget required");
        limits(maxSerializedBytes, maxInflatedBytes);
        requireMainThread();
        ItemStack[] items = Objects.requireNonNull(inventory.getContents(), "Inventory contents missing");
        if (items.length > maxSlots || inventory.getSize() > maxSlots)
            throw new IOException("Live inventory exceeds item slot budget");
        BigInteger sum = BigInteger.ZERO;
        Set<UUID> seenWallets = new HashSet<>();
        for (ItemStack item : items)
            sum = sum.add(stack(item, maxSerializedBytes, maxInflatedBytes, seenWallets));
        return sum;
    }
}
