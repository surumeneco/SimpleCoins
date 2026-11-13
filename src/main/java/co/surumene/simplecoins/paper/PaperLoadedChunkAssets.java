package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Strict, read-only loaded-chunk currency source candidate. This covers only
 * physical block inventories and dropped Item entities; it is not a complete
 * snapshot or a classification of BlockLocker/Mob/Player ownership.
 *
 * No chunk is loaded by this API. The caller must freeze or reconcile source
 * generations and aggregate unresolved holder categories before admission.
 */
public final class PaperLoadedChunkAssets {
    private PaperLoadedChunkAssets() {}

    public record Entry(String physicalKey, String world, BigInteger amount) {
        public Entry {
            if (physicalKey == null || physicalKey.isBlank() ||
                    world == null || world.isBlank() || amount == null ||
                    amount.signum() <= 0)
                throw new IllegalArgumentException("Invalid live source entry");
        }
    }

    public record Slice(String world, int chunkX, int chunkZ,
                        int scannedBlockInventories, int scannedItemEntities,
                        int unhandledTileEntities, int unhandledOtherEntities,
                        int scannedSlots, List<Entry> entries) {
        public Slice {
            world = Objects.requireNonNull(world);
            entries = List.copyOf(entries);
        }
        public BigInteger total() {
            BigInteger sum = BigInteger.ZERO;
            for (Entry entry : entries) sum = sum.add(entry.amount());
            return sum;
        }
        /** No caller can use this subset as a full-settlement completeness proof. */
        public boolean isCompleteSettlementSource() { return false; }
    }

    public static Slice scan(Chunk chunk, int maxTiles, int maxEntities,
                             int maxSlots, int maxSerializedBytes,
                             int maxInflatedBytes) throws IOException {
        return scan(chunk, maxTiles, maxEntities, maxSlots,
                maxSerializedBytes, maxInflatedBytes,
                new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * Used by a bounded loaded-world scan: wallet, entity, and physical-slot
     * identities must be shared across every chunk in that scan. On failure
     * the entire calling scan must be discarded, including these sets.
     */
    static Slice scan(Chunk chunk, int maxTiles, int maxEntities,
                      int maxSlots, int maxSerializedBytes, int maxInflatedBytes,
                      Set<UUID> walletIds, Set<UUID> entityIds,
                      Set<String> keys) throws IOException {
        Objects.requireNonNull(chunk, "loaded chunk");
        Objects.requireNonNull(walletIds, "shared wallet identities");
        Objects.requireNonNull(entityIds, "shared entity identities");
        Objects.requireNonNull(keys, "shared physical-slot identities");
        if (maxTiles < 1 || maxEntities < 1 || maxSlots < 1 ||
                maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Live scan budgets must be positive");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Loaded chunk reads require Paper primary thread");
        if (!chunk.isLoaded())
            throw new IOException("Requested chunk is not loaded");

        World world = Objects.requireNonNull(chunk.getWorld());
        String worldId = world.getName();
        int cx = chunk.getX(), cz = chunk.getZ();
        BlockState[] tiles = Objects.requireNonNull(chunk.getTileEntities(), "Chunk tile entities unavailable");
        Entity[] entities = Objects.requireNonNull(chunk.getEntities(), "Chunk entities unavailable");
        if (tiles.length > maxTiles || entities.length > maxEntities)
            throw new IOException("Loaded chunk source count exceeds budget");

        int blockInventories = 0, items = 0, otherTiles = 0, otherEntities = 0;
        int slots = 0;
        List<Entry> found = new ArrayList<>();

        for (BlockState tile : tiles) {
            if (tile == null) throw new IOException("Null block state");
            Inventory inventory;
            // A double chest exposes a shared 54-slot Inventory in Bukkit,
            // but each physical Chest block owns only 27 slots. Reading both
            // halves' shared inventory would double count stored currency.
            if (tile instanceof Chest chest) inventory = chest.getBlockInventory();
            else if (tile instanceof Container container) inventory = container.getInventory();
            else {
                otherTiles++;
                continue;
            }
            if (inventory == null) throw new IOException("Missing live block inventory");
            int x = tile.getX(), y = tile.getY(), z = tile.getZ();
            if (Math.floorDiv(x, 16) != cx || Math.floorDiv(z, 16) != cz)
                throw new IOException("Live tile outside requested chunk");
            String key = "world/" + worldId + "/" + x + "," + y + "," + z;
            ItemStack[] contents = Objects.requireNonNull(inventory.getContents());
            if (contents.length != inventory.getSize() || contents.length > maxSlots - slots)
                throw new IOException("Block inventory slot budget/size mismatch");
            slots += contents.length;
            blockInventories++;
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack stack = contents[slot];
                BigInteger amount = PaperLiveItemValue.stack(
                        stack, maxSerializedBytes, maxInflatedBytes, walletIds);
                if (amount.signum() > 0) {
                    String physical = key + ":" + slot;
                    if (!keys.add(physical))
                        throw new IOException("Duplicate physical live block slot: " + physical);
                    found.add(new Entry(physical, worldId, amount));
                }
            }
        }

        for (Entity entity : entities) {
            if (entity == null) throw new IOException("Null chunk entity");
            if (!entityIds.add(entity.getUniqueId()))
                throw new IOException("Duplicate live entity UUID: " + entity.getUniqueId());
            if (!(entity instanceof Item dropped)) {
                otherEntities++;
                continue;
            }
            if (++slots > maxSlots)
                throw new IOException("Loaded chunk Item Entity slot budget exhausted");
            String physical = "world/" + worldId + "/entity/" + entity.getUniqueId();
            BigInteger amount = PaperLiveItemValue.stack(
                    dropped.getItemStack(), maxSerializedBytes, maxInflatedBytes, walletIds);
            items++;
            if (amount.signum() > 0) {
                if (!keys.add(physical))
                    throw new IOException("Duplicate physical live Item UUID: " + physical);
                found.add(new Entry(physical, worldId, amount));
            }
        }

        if (!chunk.isLoaded())
            throw new IOException("Loaded chunk became unavailable during scan");
        return new Slice(worldId, cx, cz, blockInventories, items,
                otherTiles, otherEntities, slots, found);
    }
}
