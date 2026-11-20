package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Bounded, read-only producer of raw SAVED chest/sign observations from the
 * selected terrain sources. This is not an ownership index or a verified
 * accounting epoch. It NEVER grants an ExternalProof, P/F/D or admission.
 *
 * The full terrain map is retained only during this call. If the supplied
 * bounded memory budget cannot cover every chosen SAVED terrain Chunk, the
 * entire candidate fails. A streaming/windowed producer is still needed for
 * production-scale G0; this class must never silently truncate that scope.
 */
public final class SavedBlockLockerTerrainObservations {
    private SavedBlockLockerTerrainObservations() {}

    public record ObservedChest(
            String world,
            List<WorldStorageDiscovery.SourceFile> sources,
            SavedBlockLockerChestSigns.Observation observation) {
        public ObservedChest {
            if (world == null || world.isBlank() || observation == null
                    || sources == null || sources.isEmpty())
                throw new IllegalArgumentException("Missing SAVED chest observation provenance");
            sources = List.copyOf(sources);
        }
    }

    public record Snapshot(
            List<ObservedChest> chests,
            WorldSnapshotTraversal.ContentWitness witness,
            WorldSnapshotTraversal.Report report) {
        public Snapshot {
            chests = List.copyOf(chests);
            Objects.requireNonNull(witness);
            Objects.requireNonNull(report);
        }
    }

    private record CapturedChunk(WorldStorageDiscovery.SourceFile source,
                                 AnvilRegionReader.Chunk chunk) {}
    private record PhysicalChest(SavedBlockLockerChestSigns.Position position,
                                 WorldStorageDiscovery.SourceFile source) {}

    private static final Comparator<SavedBlockLockerChestSigns.Position> POSITION_ORDER =
            Comparator.comparingInt(SavedBlockLockerChestSigns.Position::x)
                    .thenComparingInt(SavedBlockLockerChestSigns.Position::y)
                    .thenComparingInt(SavedBlockLockerChestSigns.Position::z);

    /**
     * Capture all discovered SAVED REGION chunks in this selected world scope.
     * Every selected source is scanned and stamped by the canonical traversal;
     * even successful readback cannot establish writer exclusion or LIVE parity.
     * Missing neighbor Chunks cause refusal, not a guessed unprotected chest.
     */
    public static Snapshot capture(
            WorldStorageDiscovery.Manifest manifest,
            WorldScope scope,
            int maximumCompressedBytes,
            int maximumInflatedBytes,
            int maximumSavedTerrainChunks,
            Set<String> privateHeaders,
            Set<String> moreHeaders,
            Set<String> everyoneAliases,
            BlockLockerSavedProfileResolution.DisplayDecoder decoder) throws IOException {
        if (manifest == null || manifest.origin() == null || scope == null
                || !manifest.origin().scope().equals(scope))
            throw new IOException("A genuinely discovered, scope-matched SAVED Manifest is required");
        if (maximumSavedTerrainChunks < 1)
            throw new IOException("SAVED terrain observation requires a positive Chunk budget");
        if (privateHeaders == null || moreHeaders == null || everyoneAliases == null
                || decoder == null)
            throw new IOException("Missing actual BlockLocker sign configuration/decoder");

        Map<String, Map<SavedBlockLockerChestSigns.ChunkPosition, CapturedChunk>> byWorld =
                new TreeMap<>();
        int[] visited = {0};
        var verified = WorldSnapshotTraversal.traverseVerified(
                manifest, maximumCompressedBytes, maximumInflatedBytes,
                new WorldSnapshotTraversal.Visitor() {
                    @Override
                    public void chunk(WorldStorageDiscovery.SourceFile source,
                                      AnvilRegionReader.Chunk chunk) throws IOException {
                        if (source.kind() != WorldStorageDiscovery.Kind.REGION
                                || !scope.includes(source.world())) return;
                        if (++visited[0] > maximumSavedTerrainChunks)
                            throw new IOException("SAVED terrain observation Chunk budget exhausted");
                        if (chunk.nbt() == null)
                            throw new IOException("Missing selected SAVED terrain NBT");
                        var chunks = byWorld.computeIfAbsent(source.world(), ignored -> new HashMap<>());
                        var key = new SavedBlockLockerChestSigns.ChunkPosition(chunk.x(), chunk.z());
                        if (chunks.putIfAbsent(key, new CapturedChunk(source, chunk)) != null)
                            throw new IOException("Duplicate selected SAVED terrain Chunk: "
                                    + source.world() + "/" + key);
                    }

                    @Override
                    public void player(java.util.UUID player, Map<String, Object> data) {
                        // Source traversal verifies and content-stamps this input;
                        // it is not BlockLocker terrain and cannot assign ownership.
                    }
                }, null);

        List<ObservedChest> groups = new ArrayList<>();
        for (var worldEntry : byWorld.entrySet()) {
            String world = worldEntry.getKey();
            Map<SavedBlockLockerChestSigns.ChunkPosition, Map<String, Object>> terrain =
                    new HashMap<>();
            Map<SavedBlockLockerChestSigns.Position, PhysicalChest> physical = new HashMap<>();
            for (var entry : worldEntry.getValue().entrySet()) {
                var captured = entry.getValue();
                var chunk = captured.chunk();
                terrain.put(entry.getKey(), chunk.nbt());
                Object rawEntities = chunk.nbt().get("block_entities");
                if (rawEntities == null) continue;
                if (!(rawEntities instanceof List<?> entities))
                    throw new IOException("Invalid saved block_entities list");
                for (Object raw : entities) {
                    if (!(raw instanceof Map<?, ?> block))
                        throw new IOException("Invalid saved BlockEntity entry");
                    Object rawId = block.get("id");
                    if (!"minecraft:chest".equals(rawId)
                            && !"minecraft:trapped_chest".equals(rawId)) continue;
                    if (!(block.get("x") instanceof Integer x)
                            || !(block.get("y") instanceof Integer y)
                            || !(block.get("z") instanceof Integer z)
                            || Math.floorDiv(x, 16) != chunk.x()
                            || Math.floorDiv(z, 16) != chunk.z())
                        throw new IOException("Saved chest BlockEntity outside selected Chunk");
                    var state = SavedTerrainBlockPalette.read(chunk.nbt(), x, y, z);
                    if (!rawId.equals(state.name()))
                        throw new IOException("Saved chest BlockEntity disagrees with terrain state");
                    var position = new SavedBlockLockerChestSigns.Position(x, y, z);
                    if (physical.putIfAbsent(position,
                            new PhysicalChest(position, captured.source())) != null)
                        throw new IOException("Duplicate physical SAVED chest BlockEntity: " + position);
                }
            }
            Set<SavedBlockLockerChestSigns.Position> accounted = new HashSet<>();
            for (var position : physical.keySet().stream().sorted(POSITION_ORDER).toList()) {
                if (accounted.contains(position)) continue;
                var observation = SavedBlockLockerChestSigns.observe(terrain, position,
                        privateHeaders, moreHeaders, everyoneAliases, decoder);
                List<WorldStorageDiscovery.SourceFile> sources = new ArrayList<>();
                for (var member : observation.storageMembers()) {
                    var located = physical.get(member);
                    if (located == null || !accounted.add(member))
                        throw new IOException("Unresolved or overlapping SAVED chest member: " + member);
                    if (!sources.contains(located.source())) sources.add(located.source());
                }
                sources.sort(Comparator.comparing(s -> s.path().toString()));
                groups.add(new ObservedChest(world, sources, observation));
            }
            if (accounted.size() != physical.size())
                throw new IOException("Incomplete SAVED physical chest observation");
        }
        // A bounded final stamp cannot substitute for an exclusive writer epoch.
        verified.witness().requireUnchanged();
        return new Snapshot(groups, verified.witness(), verified.report());
    }
}
