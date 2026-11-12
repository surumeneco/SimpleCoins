package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
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
     * Immutable per-source monetary inputs verified against the SAME read's
     * World/Player aggregate. This is only a checked source capture and cannot
     * prove freeze, external ownership or authoritative economic settlement.
     */
    public record Observed(Report report, List<HybridLiveSourceStage.Source> sources) {
        public Observed {
            Objects.requireNonNull(report, "Paper universe aggregate");
            sources = List.copyOf(sources);
            Map<String, Integer> expectedChunks = new HashMap<>();
            Map<String, Map<String, BigInteger>> expectedWorldAssets = new HashMap<>();
            Set<String> expectedPhysicalKeys = new HashSet<>();
            for (var world : report.worlds()) {
                expectedChunks.put(world.world(), world.scannedChunks());
                Map<String, BigInteger> slots = new HashMap<>();
                for (var physical : world.entries()) {
                    if (!expectedPhysicalKeys.add(physical.physicalKey()) ||
                            slots.putIfAbsent(physical.physicalKey(), physical.amount()) != null)
                        throw new IllegalArgumentException("Duplicate physical slot in scanned World");
                }
                expectedWorldAssets.put(world.world(), slots);
            }
            Map<String, Integer> countedChunks = new HashMap<>();
            Map<String, Map<String, BigInteger>> actualWorldAssets = new HashMap<>();
            Map<UUID, Map<String, BigInteger>> expectedPlayerAssets = new HashMap<>();
            Set<UUID> expectedPlayers = new HashSet<>();
            for (var player : report.online().players()) {
                expectedPlayers.add(player.owner());
                Map<String, BigInteger> slots = new HashMap<>();
                for (var physical : player.entries()) {
                    if (!expectedPhysicalKeys.add(physical.physicalKey()) ||
                            slots.putIfAbsent(physical.physicalKey(), physical.amount()) != null)
                        throw new IllegalArgumentException("Duplicate physical slot in scanned Player");
                }
                expectedPlayerAssets.put(player.owner(), slots);
            }
            Map<UUID, Map<String, BigInteger>> actualPlayerAssets = new HashMap<>();
            Set<UUID> countedPlayers = new HashSet<>();
            Set<String> sourceIds = new HashSet<>();
            Set<String> physicalKeys = new HashSet<>();
            BigInteger total = BigInteger.ZERO;

            for (var source : sources) {
                if (!sourceIds.add(source.id()))
                    throw new IllegalArgumentException("Repeated Paper LIVE monetary source");
                if (source instanceof HybridLiveSourceStage.ChunkSource chunk) {
                    if (!expectedChunks.containsKey(chunk.chunk().world()))
                        throw new IllegalArgumentException("Foreign Paper loaded Chunk");
                    countedChunks.merge(chunk.chunk().world(), 1, Math::addExact);
                    actualWorldAssets.computeIfAbsent(chunk.chunk().world(),
                            ignored -> new HashMap<>());
                } else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                    if (!expectedPlayers.contains(player.player()) ||
                            !countedPlayers.add(player.player()))
                        throw new IllegalArgumentException("Foreign or repeated online Player");
                    actualPlayerAssets.put(player.player(), new HashMap<>());
                } else {
                    throw new IllegalArgumentException("Unknown Paper LIVE source");
                }
                for (var entry : source.values()) {
                    if (!physicalKeys.add(entry.physicalKey()))
                        throw new IllegalArgumentException("Repeated physical monetary slot");
                    if (source instanceof HybridLiveSourceStage.ChunkSource chunk) {
                        if (!chunk.chunk().world().equals(entry.world()) ||
                                !entry.physicalKey().startsWith(
                                        "world/" + chunk.chunk().world() + "/"))
                            throw new IllegalArgumentException("Chunk monetary source mismatch");
                    } else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                        if (entry.world() != null || !entry.physicalKey().startsWith(
                                "player/" + player.player() + "/"))
                            throw new IllegalArgumentException("Online Player monetary source mismatch");
                    }
                    if (source instanceof HybridLiveSourceStage.ChunkSource chunk)
                        actualWorldAssets.get(chunk.chunk().world())
                                .put(entry.physicalKey(), entry.amount());
                    else if (source instanceof HybridLiveSourceStage.PlayerSource player)
                        actualPlayerAssets.get(player.player())
                                .put(entry.physicalKey(), entry.amount());
                    total = total.add(entry.amount());
                }
            }
            for (var world : expectedChunks.entrySet()) {
                if (!world.getValue().equals(countedChunks.getOrDefault(world.getKey(), 0)))
                    throw new IllegalArgumentException("Paper LIVE Chunk count disagrees with source details");
            }
            // A matching grand total is NOT enough. Exchanging 25C between
            // two holders or replacing a slot key would preserve the sum yet
            // misattribute economic ownership. Compare the exact physical
            // key and value for every observed world and every online player.
            if (!countedPlayers.equals(expectedPlayers) || !total.equals(report.total()) ||
                    !expectedWorldAssets.equals(actualWorldAssets) ||
                    !expectedPlayerAssets.equals(actualPlayerAssets))
                throw new IllegalArgumentException(
                        "Paper LIVE monetary source/key/value disagrees with read-once aggregate");
        }

        public boolean isCompleteSettlementSource() { return false; }
    }

    /**
     * Does not load chunks or worlds. Caller supplies exactly the included
     * World set, chosen under WorldScope and a stable main-thread generation.
     */
    public static Report scan(Collection<World> selectedWorlds, Budget budget)
            throws IOException {
        return scan(selectedWorlds, budget, new HashSet<>());
    }

    /**
     * A single wallet UUID context can be shared with authoritative saved NBT
     * staging. The caller owns the Set and must discard it and ALL partial
     * stage results on any failure. This does not establish source temporal
     * coherence or prove all saved/live holders were read.
     */
    public static Report scan(Collection<World> selectedWorlds, Budget budget,
                              Set<UUID> sharedWalletIds) throws IOException {
        return scanWithSources(selectedWorlds, budget, sharedWalletIds).report();
    }

    /**
     * Capture each selected Paper Chunk and online Player at the point of
     * scanning. Does not revisit later to reconstruct Entity positions.
     */
    public static Observed scanWithSources(Collection<World> selectedWorlds,
                                           Budget budget,
                                           Set<UUID> sharedWalletIds) throws IOException {
        Objects.requireNonNull(sharedWalletIds, "shared physical wallet identity generation");
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

        Set<UUID> wallets = sharedWalletIds;
        Set<UUID> entitiesSeen = new HashSet<>();
        Set<String> physicalSlots = new HashSet<>();
        List<PaperLoadedWorldAssets.Report> captured = new ArrayList<>();
        List<HybridLiveSourceStage.Source> sources = new ArrayList<>();
        int chunks = 0, tiles = 0, entities = 0, slots = 0;

        for (World world : selection) {
            var report = PaperLoadedWorldAssets.scan(world,
                    Math.max(1, budget.maxChunks() - chunks),
                    Math.max(1, budget.maxTileEntities() - tiles),
                    Math.max(1, budget.maxEntities() - entities),
                    Math.max(1, budget.maxSlots() - slots),
                    budget.maxSerializedBytes(), budget.maxInflatedBytes(),
                    wallets, entitiesSeen, physicalSlots,
                    slice -> {
                        var chunk = new SavedLiveSourcePartition.Chunk(
                                slice.world(), slice.chunkX(), slice.chunkZ());
                        var values = slice.entries().stream()
                                .map(entry -> new HybridLiveSourceStage.Value(
                                        entry.physicalKey(), entry.world(), entry.amount()))
                                .toList();
                        sources.add(new HybridLiveSourceStage.ChunkSource(chunk, values));
                    });
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
        for (var player : online.players()) {
            var values = player.entries().stream()
                    .map(entry -> new HybridLiveSourceStage.Value(
                            entry.physicalKey(), null, entry.amount()))
                    .toList();
            sources.add(new HybridLiveSourceStage.PlayerSource(player.owner(), values));
        }
        return new Observed(new Report(captured, online,
                chunks, tiles, entities, slots), sources);
    }
}
