package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Strict bounded read of selected loaded worlds and online Player storage.
 * A SINGLE wallet UUID context is shared across every holder, world, Entity,
 * player inventory, armor, extra and EnderChest partition.
 *
 * Still not a coherent or complete settlement: saved worlds/playerdata,
 * BlockLocker, BAN/WWC/WWW, external mutation and journal reconciliation
 * require independent validation. No admission permit is exposed.
 */
public final class PaperLiveUniverseAssets {
    private PaperLiveUniverseAssets() {}

    public record Budget(int maxWorlds, int maxChunks, int maxTileEntities,
                         int maxEntities, int maxSlots, int maxPlayers,
                         int maxSerializedBytes, int maxInflatedBytes) {
        public Budget {
            if (maxWorlds < 1 || maxChunks < 1 || maxTileEntities < 1 ||
                    maxEntities < 1 || maxSlots < 1 || maxPlayers < 1 ||
                    maxSerializedBytes < 1 || maxInflatedBytes < 1)
                throw new IllegalArgumentException("Every live universe resource budget must be positive");
        }
    }

    public record Report(List<PaperLoadedWorldAssets.Report> worlds,
                         PaperLivePlayerAssets.OnlineSet online,
                         int scannedChunks, int scannedTiles,
                         int scannedEntities, int scannedSlots) {
        public Report {
            worlds = List.copyOf(worlds);
            Objects.requireNonNull(online, "online player sources");
            if (scannedChunks < 0 || scannedTiles < 0 || scannedEntities < 0 ||
                    scannedSlots < 0)
                throw new IllegalArgumentException("Negative live source count");

            int chunks = 0, tiles = 0, entities = 0, slots = online.scannedSlots();
            Set<String> names = new HashSet<>();
            for (var world : worlds) {
                if (!names.add(world.world()))
                    throw new IllegalArgumentException("Duplicate reported World: " + world.world());
                chunks = Math.addExact(chunks, world.scannedChunks());
                tiles = Math.addExact(tiles, Math.addExact(
                        world.scannedBlockInventories(), world.unhandledTileEntities()));
                entities = Math.addExact(entities,
                        Math.addExact(Math.addExact(world.scannedItemEntities(),
                                world.scannedDisplayEntities()),
                            Math.addExact(Math.addExact(world.scannedEquipmentEntities(),
                                    world.scannedInventoryEntities()),
                                    world.unhandledOtherEntities())));
                slots = Math.addExact(slots, world.scannedSlots());
            }
            if (scannedChunks != chunks || scannedTiles != tiles ||
                    scannedEntities != entities || scannedSlots != slots)
                throw new IllegalArgumentException("Live source aggregate count mismatch");
        }

        public BigInteger total() {
            BigInteger total = online.total();
            for (var world : worlds) total = total.add(world.total());
            return total;
        }
        public boolean isCompleteSettlementSource() { return false; }
    }

    /**
     * Does not load chunks or worlds. Caller supplies exactly the included
     * World set, chosen under WorldScope and a stable main-thread generation.
     */
    public static Report scan(Collection<World> selectedWorlds, Budget budget)
            throws IOException {
        Objects.requireNonNull(selectedWorlds, "selected Paper Worlds");
        Objects.requireNonNull(budget, "bounded live scan");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("Live universe read requires Paper primary thread");

        List<World> selection = List.copyOf(selectedWorlds);
        if (selection.size() > budget.maxWorlds())
            throw new IOException("Selected World count exceeds scan budget");
        Set<String> worldNames = new HashSet<>();
        Set<UUID> worldUuids = new HashSet<>();
        // Validate the full selection before any actual inventory reads.
        for (World world : selection) {
            if (world.getName() == null || world.getName().isBlank() ||
                    !worldNames.add(world.getName()) ||
                    world.getUID() == null || !worldUuids.add(world.getUID()))
                throw new IOException("Duplicate/invalid selected live World");
        }

        Set<UUID> wallets = new HashSet<>();
        Set<UUID> entitiesSeen = new HashSet<>();
        Set<String> physicalSlots = new HashSet<>();
        List<PaperLoadedWorldAssets.Report> captured = new ArrayList<>();
        int chunks = 0, tiles = 0, entities = 0, slots = 0;

        for (World world : selection) {
            var report = PaperLoadedWorldAssets.scan(world,
                    Math.max(1, budget.maxChunks() - chunks),
                    Math.max(1, budget.maxTileEntities() - tiles),
                    Math.max(1, budget.maxEntities() - entities),
                    Math.max(1, budget.maxSlots() - slots),
                    budget.maxSerializedBytes(), budget.maxInflatedBytes(),
                    wallets, entitiesSeen, physicalSlots);
            int readTiles = Math.addExact(report.scannedBlockInventories(),
                    report.unhandledTileEntities());
            int readEntities = Math.addExact(
                    Math.addExact(report.scannedItemEntities(), report.scannedDisplayEntities()),
                    Math.addExact(Math.addExact(report.scannedEquipmentEntities(),
                            report.scannedInventoryEntities()), report.unhandledOtherEntities()));
            if (report.scannedChunks() > budget.maxChunks() - chunks ||
                    readTiles > budget.maxTileEntities() - tiles ||
                    readEntities > budget.maxEntities() - entities ||
                    report.scannedSlots() > budget.maxSlots() - slots)
                throw new IOException("Aggregate Paper world scan exceeded hard budget");
            chunks = Math.addExact(chunks, report.scannedChunks());
            tiles = Math.addExact(tiles, readTiles);
            entities = Math.addExact(entities, readEntities);
            slots = Math.addExact(slots, report.scannedSlots());
            captured.add(report);
        }

        // Player storage is world-independent, even when selected Worlds is
        // empty due to the accounting WorldScope exclusions.
        var online = PaperLivePlayerAssets.scanOnline(budget.maxPlayers(),
                Math.max(1, budget.maxSlots() - slots),
                budget.maxSerializedBytes(), budget.maxInflatedBytes(), wallets);
        if (online.scannedSlots() > budget.maxSlots() - slots)
            throw new IOException("Aggregate world-plus-Player slot budget exhausted");
        slots = Math.addExact(slots, online.scannedSlots());
        return new Report(captured, online, chunks, tiles, entities, slots);
    }
}
