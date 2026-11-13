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

    /** World identity and exactly the Chunk coordinates captured in that World scan. */
    static record WorldCapture(World world, Set<Long> chunkCoordinates) {
        WorldCapture {
            Objects.requireNonNull(world, "captured world");
            chunkCoordinates = Set.copyOf(chunkCoordinates);
        }
    }

    /** Final cross-World source check, after all World and Player inventory reads. */
    static void requireWorldSourceSetsStillLoaded(List<WorldCapture> captures)
            throws IOException {
        for (WorldCapture capture : captures) {
            PaperLoadedWorldAssets.requireStableChunkCoordinates(
                    capture.chunkCoordinates(),
                    PaperLoadedWorldAssets.requireLoadedChunkCoordinates(capture.world()));
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
     * Validate physical block slot provenance against its declared Chunk.
     * Entity UUID slots do not encode coordinates and require separate evidence.
     */
    private static void requireBlockSlotInChunk(SavedLiveSourcePartition.Chunk chunk,
                                                String physicalKey) {
        String prefix = "world/" + chunk.world() + "/";
        if (!physicalKey.startsWith(prefix))
            throw new IllegalArgumentException("Foreign World monetary source");
        String location = physicalKey.substring(prefix.length());
        if (location.startsWith("entity/")) return;
        int delimiter = location.indexOf(':');
        if (delimiter <= 0 || delimiter != location.lastIndexOf(':'))
            throw new IllegalArgumentException("Malformed live block slot: " + physicalKey);
        String[] coordinates = location.substring(0, delimiter).split(",", -1);
        if (coordinates.length != 3)
            throw new IllegalArgumentException("Malformed live block position: " + physicalKey);
        try {
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            int z = Integer.parseInt(coordinates[2]);
            String rawSlot = location.substring(delimiter + 1);
            int slot = Integer.parseInt(rawSlot);
            if (slot < 0 || !coordinates[0].equals(Integer.toString(x)) ||
                    !coordinates[1].equals(Integer.toString(y)) ||
                    !coordinates[2].equals(Integer.toString(z)) ||
                    !rawSlot.equals(Integer.toString(slot)))
                throw new IllegalArgumentException("Noncanonical live block slot: " + physicalKey);
            if (Math.floorDiv(x, 16) != chunk.x() ||
                    Math.floorDiv(z, 16) != chunk.z())
                throw new IllegalArgumentException(
                        "LIVE monetary slot assigned to a foreign Chunk: " + physicalKey);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Malformed live block coordinates: " + physicalKey, invalid);
        }
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
            for (var world : report.worlds())
                expectedChunks.put(world.world(), world.scannedChunks());
            Map<String, Integer> countedChunks = new HashMap<>();
            // Materialize exact physical slot → monetary amount authority
            // from the original one-pass Paper scan. Equal sums alone cannot
            // prove the same physical holders were retained in source detail.
            Map<String, BigInteger> expectedValues = new HashMap<>();
            for (var world : report.worlds()) {
                for (var entry : world.entries()) {
                    if (expectedValues.putIfAbsent(entry.physicalKey(), entry.amount()) != null)
                        throw new IllegalArgumentException("Repeated physical slot in World aggregate");
                }
            }
            Set<UUID> expectedPlayers = new HashSet<>();
            for (var player : report.online().players()) {
                expectedPlayers.add(player.owner());
                for (var entry : player.entries()) {
                    if (expectedValues.putIfAbsent(entry.physicalKey(), entry.amount()) != null)
                        throw new IllegalArgumentException("Repeated physical slot in online Player aggregate");
                }
            }
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
                } else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                    if (!expectedPlayers.contains(player.player()) ||
                            !countedPlayers.add(player.player()))
                        throw new IllegalArgumentException("Foreign or repeated online Player");
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
                        requireBlockSlotInChunk(chunk.chunk(), entry.physicalKey());
                    } else if (source instanceof HybridLiveSourceStage.PlayerSource player) {
                        if (entry.world() != null || !entry.physicalKey().startsWith(
                                "player/" + player.player() + "/"))
                            throw new IllegalArgumentException("Online Player monetary source mismatch");
                    }
                    BigInteger originallyRead = expectedValues.remove(entry.physicalKey());
                    if (originallyRead == null || !originallyRead.equals(entry.amount()))
                        throw new IllegalArgumentException(
                                "Paper LIVE monetary source does not match the real scanned slot: "
                                + entry.physicalKey());
                    total = total.add(entry.amount());
                }
            }
            if (!expectedValues.isEmpty())
                throw new IllegalArgumentException(
                        "Paper LIVE monetary detail omitted physical slots: "
                        + expectedValues.keySet());
            for (var world : expectedChunks.entrySet()) {
                if (!world.getValue().equals(countedChunks.getOrDefault(world.getKey(), 0)))
                    throw new IllegalArgumentException("Paper LIVE Chunk count disagrees with source details");
            }
            if (!countedPlayers.equals(expectedPlayers) || !total.equals(report.total()))
                throw new IllegalArgumentException("Paper LIVE monetary detail disagrees with aggregate");
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
        List<WorldCapture> worldCaptures = new ArrayList<>();
        List<HybridLiveSourceStage.Source> sources = new ArrayList<>();
        int chunks = 0, tiles = 0, entities = 0, slots = 0;

        for (World world : selection) {
            Set<Long> observedChunkCoordinates = new HashSet<>();
            var report = PaperLoadedWorldAssets.scan(world,
                    Math.max(1, budget.maxChunks() - chunks),
                    Math.max(1, budget.maxTileEntities() - tiles),
                    Math.max(1, budget.maxEntities() - entities),
                    Math.max(1, budget.maxSlots() - slots),
                    budget.maxSerializedBytes(), budget.maxInflatedBytes(),
                    wallets, entitiesSeen, physicalSlots,
                    slice -> {
                        if (!observedChunkCoordinates.add(PaperLoadedWorldAssets.chunkCoordinate(
                                slice.chunkX(), slice.chunkZ())))
                            throw new IOException("Duplicate captured Paper Chunk source");
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
            if (observedChunkCoordinates.size() != report.scannedChunks())
                throw new IOException("Missing captured Paper Chunk source");
            captured.add(report);
            worldCaptures.add(new WorldCapture(world, observedChunkCoordinates));
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
        requireWorldSourceSetsStillLoaded(worldCaptures);
        return new Observed(new Report(captured, online,
                chunks, tiles, entities, slots), sources);
    }
}
