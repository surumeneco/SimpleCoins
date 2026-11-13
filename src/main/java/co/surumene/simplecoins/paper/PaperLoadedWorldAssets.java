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
                         int scannedItemEntities, int unhandledTileEntities,
                         int unhandledOtherEntities, int scannedSlots,
                         List<PaperLoadedChunkAssets.Entry> entries) {
        public Report {
            if (world == null || world.isBlank() || scannedChunks < 0 ||
                    scannedBlockInventories < 0 || scannedItemEntities < 0 ||
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
        Objects.requireNonNull(world, "world");
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
        Set<UUID> walletIds = new HashSet<>();
        Set<UUID> entityIds = new HashSet<>();
        Set<String> physicalSlots = new HashSet<>();
        List<PaperLoadedChunkAssets.Entry> entries = new ArrayList<>();
        int tiles = 0, entities = 0, blocks = 0, itemEntities = 0;
        int otherTiles = 0, otherEntities = 0, slots = 0;

        for (Chunk chunk : loaded) {
            if (chunk == null || chunk.getWorld() != world || !chunk.isLoaded())
                throw new IOException("Invalid or unloaded world chunk");
            long coordinate = ((long) chunk.getX() << 32) | (chunk.getZ() & 0xffffffffL);
            if (!chunkCoordinates.add(coordinate))
                throw new IOException("Duplicate loaded chunk coordinates: " + coordinate);
            PaperLoadedChunkAssets.Slice slice = PaperLoadedChunkAssets.scan(chunk,
                    Math.max(1, maxTiles - tiles),
                    Math.max(1, maxEntities - entities),
                    Math.max(1, maxSlots - slots),
                    maxSerializedBytes, maxInflatedBytes,
                    walletIds, entityIds, physicalSlots);
            int nextTiles = Math.addExact(slice.scannedBlockInventories(), slice.unhandledTileEntities());
            int nextEntities = Math.addExact(slice.scannedItemEntities(), slice.unhandledOtherEntities());
            if (nextTiles > maxTiles - tiles || nextEntities > maxEntities - entities ||
                    slice.scannedSlots() > maxSlots - slots)
                throw new IOException("Loaded-world scan aggregate budget exhausted");
            tiles += nextTiles;
            entities += nextEntities;
            slots += slice.scannedSlots();
            blocks += slice.scannedBlockInventories();
            itemEntities += slice.scannedItemEntities();
            otherTiles += slice.unhandledTileEntities();
            otherEntities += slice.unhandledOtherEntities();
            entries.addAll(slice.entries());
        }
        return new Report(world.getName(), loaded.length, blocks, itemEntities,
                otherTiles, otherEntities, slots, entries);
    }
}
