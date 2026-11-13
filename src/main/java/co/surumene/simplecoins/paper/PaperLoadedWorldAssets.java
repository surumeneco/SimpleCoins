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

/**
 * Bounded, read-only aggregation of only currently loaded Paper Chunk sources.
 * This is a partial observation, NOT an authoritative world snapshot: missing
 * source kinds, external-plugin mutations, and disk/live reconciliation remain.
 */
public final class PaperLoadedWorldAssets {
    private PaperLoadedWorldAssets() {}

    public record Report(String world, int scannedChunks, int scannedBlockInventories,
                         int scannedItemEntities, int scannedDisplayEntities,
                         int scannedEquipmentEntities, int scannedInventoryEntities,
                         int unhandledTileEntities, int unhandledOtherEntities,
                         int scannedSlots, List<PaperLoadedChunkAssets.Entry> entries) {
        /** Preserve both previous report constructors. */
        public Report(String world, int scannedChunks, int scannedBlockInventories,
                      int scannedItemEntities, int unhandledTileEntities,
                      int unhandledOtherEntities, int scannedSlots,
                      List<PaperLoadedChunkAssets.Entry> entries) {
            this(world, scannedChunks, scannedBlockInventories,
                    scannedItemEntities, 0, 0, 0, unhandledTileEntities,
                    unhandledOtherEntities, scannedSlots, entries);
        }
        public Report(String world, int scannedChunks, int scannedBlockInventories,
                      int scannedItemEntities, int scannedDisplayEntities,
                      int unhandledTileEntities, int unhandledOtherEntities,
                      int scannedSlots, List<PaperLoadedChunkAssets.Entry> entries) {
            this(world, scannedChunks, scannedBlockInventories,
                    scannedItemEntities, scannedDisplayEntities, 0, 0,
                    unhandledTileEntities, unhandledOtherEntities, scannedSlots, entries);
        }
        public Report(String world, int scannedChunks, int scannedBlockInventories,
                      int scannedItemEntities, int scannedDisplayEntities,
                      int scannedEquipmentEntities, int unhandledTileEntities,
                      int unhandledOtherEntities, int scannedSlots,
                      List<PaperLoadedChunkAssets.Entry> entries) {
            this(world, scannedChunks, scannedBlockInventories,
                    scannedItemEntities, scannedDisplayEntities, scannedEquipmentEntities, 0,
                    unhandledTileEntities, unhandledOtherEntities, scannedSlots, entries);
        }
        public Report {
            if (world == null || world.isBlank() || scannedChunks < 0 ||
                    scannedBlockInventories < 0 || scannedItemEntities < 0 ||
                    scannedDisplayEntities < 0 || scannedEquipmentEntities < 0 ||
                    scannedInventoryEntities < 0 ||
                    unhandledTileEntities < 0 || unhandledOtherEntities < 0 ||
                    scannedSlots < 0)
                throw new IllegalArgumentException("Invalid loaded world report");
            entries = List.copyOf(entries);
            for (var entry : entries)
                if (!entry.world().equals(world))
                    throw new IllegalArgumentException("Foreign-world currency entry");
        }

        public BigInteger total() {
            BigInteger result = BigInteger.ZERO;
            for (var entry : entries) result = result.add(entry.amount());
            return result;
        }

        /** This cannot prove saved/live completeness or safe admission. */
        public boolean isCompleteSettlementSource() { return false; }
    }

    /** Signed 32-bit Chunk coordinates form a unique 64-bit source key. */
    static long chunkCoordinate(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** Disk/source publication must not trust a changed loaded-Chunk set. */
    static void requireStableChunkCoordinates(Set<Long> initial, Set<Long> finalSet)
            throws IOException {
        if (!initial.equals(finalSet))
            throw new IOException("Loaded Paper Chunk set changed during monetary scan");
    }

    /**
     * No chunk loading or generation is performed. The entire selected set
     * is inspected on the primary thread, under explicit hard resource budgets.
     * For large worlds use smaller externally selected windows instead of
     * passing an unbounded budget and blocking the Paper tick.
     */
    public static Report scan(World world, int maxChunks, int maxTiles,
                              int maxEntities, int maxSlots,
                              int maxSerializedBytes, int maxInflatedBytes)
            throws IOException {
        return scan(world, maxChunks, maxTiles, maxEntities, maxSlots,
                maxSerializedBytes, maxInflatedBytes,
                new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    /**
     * Reuse a single wallet/entity/physical-slot identity generation across
     * all selected worlds, rather than resetting it for every World.
     * These mutable sets MUST be discarded on any scan failure.
     */
    /** Receive the exact authoritative loaded Chunk Slice during this one scan. */
    @FunctionalInterface
    interface SliceObserver {
        void accept(PaperLoadedChunkAssets.Slice slice) throws IOException;
    }

    static Report scan(World world, int maxChunks, int maxTiles,
                       int maxEntities, int maxSlots,
                       int maxSerializedBytes, int maxInflatedBytes,
                       Set<UUID> walletIds, Set<UUID> entityIds,
                       Set<String> physicalSlots) throws IOException {
        return scan(world, maxChunks, maxTiles, maxEntities, maxSlots,
                maxSerializedBytes, maxInflatedBytes, walletIds, entityIds,
                physicalSlots, slice -> {});
    }

    static Report scan(World world, int maxChunks, int maxTiles,
                       int maxEntities, int maxSlots,
                       int maxSerializedBytes, int maxInflatedBytes,
                       Set<UUID> walletIds, Set<UUID> entityIds,
                       Set<String> physicalSlots, SliceObserver observed) throws IOException {
        Objects.requireNonNull(observed, "loaded Chunk source observer");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(walletIds, "shared wallet identities");
        Objects.requireNonNull(entityIds, "shared Entity identities");
        Objects.requireNonNull(physicalSlots, "shared physical-slot identities");
        if (maxChunks < 1 || maxTiles < 1 || maxEntities < 1 ||
                maxSlots < 1 || maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("All loaded-world scan budgets must be positive");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Loaded-world scan requires Paper primary thread");

        Chunk[] loaded = Objects.requireNonNull(world.getLoadedChunks(),
                "Loaded chunk inventory unavailable");
        if (loaded.length > maxChunks)
            throw new IOException("Loaded world chunk count exceeds scan budget");

        Set<Long> chunkCoordinates = new HashSet<>();
        List<PaperLoadedChunkAssets.Entry> entries = new ArrayList<>();
        int tiles = 0, entities = 0, blocks = 0, itemEntities = 0;
        int displayEntities = 0, equippedEntities = 0, inventoryEntities = 0;
        int otherTiles = 0, otherEntities = 0, slots = 0;

        for (Chunk chunk : loaded) {
            if (chunk == null || chunk.getWorld() != world || !chunk.isLoaded())
                throw new IOException("Invalid or unloaded world chunk");
            long coordinate = chunkCoordinate(chunk.getX(), chunk.getZ());
            if (!chunkCoordinates.add(coordinate))
                throw new IOException("Duplicate loaded chunk coordinates: " + coordinate);
            PaperLoadedChunkAssets.Slice slice = PaperLoadedChunkAssets.scan(chunk,
                    Math.max(1, maxTiles - tiles),
                    Math.max(1, maxEntities - entities),
                    Math.max(1, maxSlots - slots),
                    maxSerializedBytes, maxInflatedBytes,
                    walletIds, entityIds, physicalSlots);
            int nextTiles = Math.addExact(slice.scannedBlockInventories(), slice.unhandledTileEntities());
            int nextEntities = Math.addExact(Math.addExact(
                    Math.addExact(slice.scannedItemEntities(), slice.scannedDisplayEntities()),
                    slice.scannedEquipmentEntities()),
                    Math.addExact(slice.scannedInventoryEntities(), slice.unhandledOtherEntities()));
            if (nextTiles > maxTiles - tiles || nextEntities > maxEntities - entities ||
                    slice.scannedSlots() > maxSlots - slots)
                throw new IOException("Loaded-world scan aggregate budget exhausted");
            // Capture monetary source details from the same bounded scan;
            // never re-query chunks to infer their IDs after the fact.
            observed.accept(slice);
            tiles += nextTiles;
            entities += nextEntities;
            slots += slice.scannedSlots();
            blocks += slice.scannedBlockInventories();
            itemEntities += slice.scannedItemEntities();
            displayEntities += slice.scannedDisplayEntities();
            equippedEntities += slice.scannedEquipmentEntities();
            inventoryEntities += slice.scannedInventoryEntities();
            otherTiles += slice.unhandledTileEntities();
            otherEntities += slice.unhandledOtherEntities();
            entries.addAll(slice.entries());
        }
        // Verify coverage at the end, not just the first World.getLoadedChunks
        // snapshot. Even an equal-sized replacement means that currency in
        // one source was missed and a different source was counted stale.
        Chunk[] afterScan = Objects.requireNonNull(world.getLoadedChunks(),
                "Loaded world Chunk set missing at scan completion");
        Set<Long> afterCoordinates = new HashSet<>();
        for (Chunk candidate : afterScan) {
            if (candidate == null || candidate.getWorld() != world ||
                    !candidate.isLoaded() ||
                    !afterCoordinates.add(chunkCoordinate(candidate.getX(), candidate.getZ())))
                throw new IOException("Invalid/duplicate loaded Chunk after scan");
        }
        requireStableChunkCoordinates(chunkCoordinates, afterCoordinates);
        return new Report(world.getName(), loaded.length, blocks, itemEntities,
                displayEntities, equippedEntities, inventoryEntities,
                otherTiles, otherEntities, slots, entries);
    }
}
