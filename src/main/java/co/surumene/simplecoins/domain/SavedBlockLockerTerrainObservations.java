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

    /**
     * Only the completed terrain traversal may create this immutable receipt.
     * A public record constructor allowed a caller to graft invented signage
     * onto a genuine ContentWitness and then obtain a fabricated Free candidate.
     * The separate source witness still does NOT exclude external writers.
     */
    public static final class Snapshot {
        private final List<ObservedChest> chests;
        private final WorldSnapshotTraversal.ContentWitness witness;
        private final WorldSnapshotTraversal.Report report;
        private final Set<String> configuredContainers;

        private Snapshot(List<ObservedChest> chests,
                         WorldSnapshotTraversal.ContentWitness witness,
                         WorldSnapshotTraversal.Report report,Set<String> configuredContainers) {
            this.chests = List.copyOf(chests);
            this.witness = Objects.requireNonNull(witness);
            this.report = Objects.requireNonNull(report);
            this.configuredContainers=Set.copyOf(configuredContainers);
        }

        public Set<String> configuredContainers() { return configuredContainers; }

        public List<ObservedChest> chests() {
            return chests;
        }

        public WorldSnapshotTraversal.ContentWitness witness() {
            return witness;
        }

        public WorldSnapshotTraversal.Report report() {
            return report;
        }

        @FunctionalInterface
        public interface IndependentProofs {
            SavedBlockLockerOwnerCandidate.ExternalProof forChest(ObservedChest chest)
                    throws IOException;
        }

        /**
         * Compose selected observations with the existing fail-closed local
         * physical owner index. The independent proof provider never gains
         * affirmative flags from SAVED NBT or a merely successful readback.
         * No unpublished index is returned on failed or changed source input.
         * This does not freeze Paper memory or establish G0 writer closure.
         */
        public SavedBlockLockerPhysicalOwnerIndex assembleOwnerIndex(
                WorldStorageDiscovery.Manifest selected,
                IndependentProofs independentProofs) throws IOException {
            if (independentProofs == null)
                throw new IOException("Independent BlockLocker proof provider is missing");
            witness.requireOriginalManifest(selected);
            witness.requireUnchanged();
            var validRegions = Set.copyOf(selected.worldFiles());
            var builder = new SavedBlockLockerPhysicalOwnerIndex.Builder();
            for (ObservedChest chest : chests) {
                if (chest == null || !selected.origin().scope().includes(chest.world()))
                    throw new IOException("Observed chest world is outside the selected source scope");
                var identities = new HashSet<WorldStorageDiscovery.SourceFile>();
                for (WorldStorageDiscovery.SourceFile source : chest.sources()) {
                    if (source == null || source.kind() != WorldStorageDiscovery.Kind.REGION
                            || !source.world().equals(chest.world())
                            || !validRegions.contains(source) || !identities.add(source))
                        throw new IOException("Unselected or duplicate SAVED chest provenance");
                }
                // Both actual signs and SIGN ABSENCE were read in the five
                // adjacent blocks for each physical chest half. A candidate's
                // local evidence must contain exactly those selected Regions.
                Set<String> requiredRegionNames = new HashSet<>();
                for (var member : chest.observation().storageMembers()) {
                    try {
                        requireRegionName(requiredRegionNames, member.x(), member.z());
                        requireRegionName(requiredRegionNames, member.x(),
                                Math.subtractExact(member.z(), 1));
                        requireRegionName(requiredRegionNames,
                                Math.addExact(member.x(), 1), member.z());
                        requireRegionName(requiredRegionNames, member.x(),
                                Math.addExact(member.z(), 1));
                        requireRegionName(requiredRegionNames,
                                Math.subtractExact(member.x(), 1), member.z());
                        Math.addExact(member.y(), 1); // up has the same Region
                    } catch (ArithmeticException overflow) {
                        throw new IOException("Invalid SAVED chest sign-neighborhood coordinates",
                                overflow);
                    }
                }
                Set<String> observedNames = new HashSet<>();
                for (var source : identities)
                    observedNames.add(source.path().getFileName().toString());
                if (!requiredRegionNames.equals(observedNames))
                    throw new IOException("Incomplete or foreign SAVED sign-neighborhood Region provenance");
                builder.accept(chest.world(), chest.observation(),
                        independentProofs.forChest(chest));
            }
            var index = builder.finish();
            witness.requireUnchanged();
            return index;
        }
    }

    private static void requireRegionName(Set<String> output, int x, int z) {
        output.add("r." + Math.floorDiv(x, 512) + "."
                + Math.floorDiv(z, 512) + ".mca");
    }

    private record CapturedChunk(WorldStorageDiscovery.SourceFile source,
                                 AnvilRegionReader.Chunk chunk) {}
    private record PhysicalChest(SavedBlockLockerChestSigns.Position position) {}

    /**
     * A chest ownership observation depends on EVERY selected Chunk inspected
     * by SavedBlockLockerChestSigns.observe: storage members plus north/east/
     * south/west/up. In particular, "no sign" is source-backed evidence about
     * an adjacent Chunk, not only about the chest's own Region.
     */
    private static List<WorldStorageDiscovery.SourceFile> observedNeighborhoodSources(
            SavedBlockLockerChestSigns.Observation observation,
            Map<SavedBlockLockerChestSigns.ChunkPosition,
                    WorldStorageDiscovery.SourceFile> sourceByChunk) throws IOException {
        if (observation == null || observation.storageMembers().isEmpty())
            throw new IOException("No physical chest members to provenance");
        Set<WorldStorageDiscovery.SourceFile> dependencies = new HashSet<>();
        for (var member : observation.storageMembers()) {
            int x = member.x(), z = member.z();
            try {
                addObservedSource(dependencies, sourceByChunk, x, z);
                addObservedSource(dependencies, sourceByChunk, x, Math.subtractExact(z, 1));
                addObservedSource(dependencies, sourceByChunk, Math.addExact(x, 1), z);
                addObservedSource(dependencies, sourceByChunk, x, Math.addExact(z, 1));
                addObservedSource(dependencies, sourceByChunk, Math.subtractExact(x, 1), z);
                Math.addExact(member.y(), 1); // up: same x/z Chunk
            } catch (ArithmeticException overflow) {
                throw new IOException("SAVED sign-neighborhood coordinate overflow", overflow);
            }
        }
        return dependencies.stream()
                .sorted(Comparator.comparing(s -> s.path().toString())).toList();
    }

    private static void addObservedSource(
            Set<WorldStorageDiscovery.SourceFile> dependencies,
            Map<SavedBlockLockerChestSigns.ChunkPosition,
                    WorldStorageDiscovery.SourceFile> sourceByChunk,
            int x, int z) throws IOException {
        var chunk = new SavedBlockLockerChestSigns.ChunkPosition(
                Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        var source = sourceByChunk.get(chunk);
        if (source == null || source.kind() != WorldStorageDiscovery.Kind.REGION)
            throw new IOException("Missing selected SAVED sign-neighborhood Region: " + chunk);
        dependencies.add(source);
    }

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
        return captureConfigured(manifest,scope,maximumCompressedBytes,maximumInflatedBytes,maximumSavedTerrainChunks,
                privateHeaders,moreHeaders,everyoneAliases,decoder,Set.of("minecraft:chest","minecraft:trapped_chest"));
    }

    /** Configured storage types; connectContainers=false is required by the caller. */
    public static Snapshot captureConfigured(WorldStorageDiscovery.Manifest manifest,WorldScope scope,
            int maximumCompressedBytes,int maximumInflatedBytes,int maximumSavedTerrainChunks,
            Set<String> privateHeaders,Set<String> moreHeaders,Set<String> everyoneAliases,
            BlockLockerSavedProfileResolution.DisplayDecoder decoder,Set<String> configuredContainers) throws IOException {
        Set<String> configured=Set.copyOf(configuredContainers);
        if (manifest == null || manifest.origin() == null || scope == null
                || !manifest.origin().scope().equals(scope))
            throw new IOException("A genuinely discovered, scope-matched SAVED Manifest is required");
        if (maximumSavedTerrainChunks < 1)
            throw new IOException("SAVED terrain observation requires a positive Chunk budget");
        if (privateHeaders == null || moreHeaders == null || everyoneAliases == null
                || decoder == null)
            throw new IOException("Missing actual BlockLocker sign configuration/decoder");

        // Pass 1 deliberately retains only the physical chest positions and
        // the identity of EVERY selected terrain Chunk. Inflated NBT for an
        // unrelated Chunk must not remain reachable just because this world
        // contains one BlockLocker-protected chest. Every input still goes
        // through the canonical full-file/content-verified SAVED traversal.
        Map<String, Map<SavedBlockLockerChestSigns.ChunkPosition,
                WorldStorageDiscovery.SourceFile>> selectedChunks = new TreeMap<>();
        Map<String, Map<SavedBlockLockerChestSigns.Position, PhysicalChest>> physicalByWorld =
                new TreeMap<>();
        int[] visited = {0};
        var first = WorldSnapshotTraversal.traverseVerified(
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
                        var chunks = selectedChunks.computeIfAbsent(
                                source.world(), ignored -> new HashMap<>());
                        var key = new SavedBlockLockerChestSigns.ChunkPosition(
                                chunk.x(), chunk.z());
                        if (chunks.putIfAbsent(key, source) != null)
                            throw new IOException("Duplicate selected SAVED terrain Chunk: "
                                    + source.world() + "/" + key);
                        Object rawEntities = chunk.nbt().get("block_entities");
                        if (rawEntities == null) return;
                        if (!(rawEntities instanceof List<?> entities))
                            throw new IOException("Invalid saved block_entities list");
                        for (Object raw : entities) {
                            if (!(raw instanceof Map<?, ?> block))
                                throw new IOException("Invalid saved BlockEntity entry");
                            Object rawId = block.get("id");
                            if (!(rawId instanceof String tileType))throw new IOException("Missing BlockEntity id");
                            if(!configured.contains(tileType)&&configured.stream().noneMatch(type ->
                                    SavedBlockLockerChestSigns.matchesStorage(tileType,type)))continue;
                            if (!(block.get("x") instanceof Integer x)
                                    || !(block.get("y") instanceof Integer y)
                                    || !(block.get("z") instanceof Integer z)
                                    || Math.floorDiv(x, 16) != chunk.x()
                                    || Math.floorDiv(z, 16) != chunk.z())
                                throw new IOException("Saved chest BlockEntity outside selected Chunk");
                            var state = SavedTerrainBlockPalette.read(chunk.nbt(), x, y, z);
                            if (!SavedBlockLockerChestSigns.matchesStorage(tileType,state.name()))
                                throw new IOException("Saved configured BlockEntity disagrees with terrain state");
                            if(!configured.contains(state.name()))continue;
                            var position = new SavedBlockLockerChestSigns.Position(x, y, z);
                            var physical = physicalByWorld.computeIfAbsent(
                                    source.world(), ignored -> new HashMap<>());
                            if (physical.putIfAbsent(position, new PhysicalChest(position)) != null)
                                throw new IOException("Duplicate physical SAVED chest BlockEntity: "
                                        + position);
                        }
                    }

                    @Override
                    public void player(java.util.UUID player, Map<String, Object> data) {
                        // All selected playerdata are still checked by the
                        // full-file visitor; they cannot assert chest ownership.
                    }
                }, null);

        // There is no BlockLocker neighborhood to resolve if the complete
        // selected SAVED terrain has no physical chests. No second NBT pass
        // is needed; this does not prove that Paper's live state is frozen.
        if (physicalByWorld.isEmpty()) {
            first.witness().requireUnchanged();
            return new Snapshot(List.of(), first.witness(), first.report(),configured);
        }

        // A chest's one-block N/E/S/W/up neighborhood and its possible
        // double-chest partner all lie within these 5 horizontal positions.
        // An ABSENT sign requires the neighboring Chunk itself as evidence.
        Map<String, Set<SavedBlockLockerChestSigns.ChunkPosition>> needed =
                new TreeMap<>();
        for (var worldEntry : physicalByWorld.entrySet()) {
            var world = worldEntry.getKey();
            var selected = selectedChunks.get(world);
            if (selected == null)
                throw new IOException("Missing discovered SAVED chest World");
            Set<SavedBlockLockerChestSigns.ChunkPosition> targets = new HashSet<>();
            for (var chest : worldEntry.getValue().keySet()) {
                int x = chest.x(), z = chest.z();
                try {
                    Math.addExact(chest.y(), 1);
                    for (var key : List.of(
                            new SavedBlockLockerChestSigns.ChunkPosition(
                                    Math.floorDiv(x, 16), Math.floorDiv(z, 16)),
                            new SavedBlockLockerChestSigns.ChunkPosition(
                                    Math.floorDiv(x, 16), Math.floorDiv(Math.subtractExact(z, 1), 16)),
                            new SavedBlockLockerChestSigns.ChunkPosition(
                                    Math.floorDiv(Math.addExact(x, 1), 16), Math.floorDiv(z, 16)),
                            new SavedBlockLockerChestSigns.ChunkPosition(
                                    Math.floorDiv(x, 16), Math.floorDiv(Math.addExact(z, 1), 16)),
                            new SavedBlockLockerChestSigns.ChunkPosition(
                                    Math.floorDiv(Math.subtractExact(x, 1), 16), Math.floorDiv(z, 16)))) {
                        if (!selected.containsKey(key))
                            throw new IOException("Missing selected SAVED sign-neighborhood Chunk: "
                                    + world + "/" + key);
                        targets.add(key);
                    }
                } catch (ArithmeticException overflow) {
                    throw new IOException("SAVED sign-neighborhood coordinate overflow", overflow);
                }
            }
            needed.put(world, Set.copyOf(targets));
        }

        // Pass 2 needs to decode only the Region files containing verified
        // chest/sign-neighborhood Chunks. Pass 1 already fully parsed EVERY
        // selected source, and its content witness rechecks the entire source
        // universe (including sidecars and playerdata) after these reads.
        // Repeating the complete full-world NBT decode would otherwise add a
        // costly server-tick pause to a memory-saving refactor.
        Map<String, Map<SavedBlockLockerChestSigns.ChunkPosition, CapturedChunk>> byWorld =
                new TreeMap<>();
        for (var worldEntry : needed.entrySet()) {
            String world = worldEntry.getKey();
            Map<SavedBlockLockerChestSigns.ChunkPosition,
                    WorldStorageDiscovery.SourceFile> selected = selectedChunks.get(world);
            Map<WorldStorageDiscovery.SourceFile,
                    Set<SavedBlockLockerChestSigns.ChunkPosition>> selectedRegions = new HashMap<>();
            for (var key : worldEntry.getValue()) {
                var source = selected.get(key);
                if (source == null || source.kind() != WorldStorageDiscovery.Kind.REGION)
                    throw new IOException("Missing selected SAVED sign-neighborhood Region: "
                            + world + "/" + key);
                selectedRegions.computeIfAbsent(source, ignored -> new HashSet<>()).add(key);
            }
            for (var sourceEntry : selectedRegions.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().path().toString())).toList()) {
                var source = sourceEntry.getKey();
                var required = sourceEntry.getValue();
                Set<SavedBlockLockerChestSigns.ChunkPosition> seen = new HashSet<>();
                // The canonical Anvil scanner validates the complete allocation
                // table and NBT in every SELECTED Region, not only positive
                // chest entries; a changed/foreign source cannot be accepted.
                AnvilRegionReader.scan(source.path(), maximumCompressedBytes,
                        maximumInflatedBytes, chunk -> {
                            var key = new SavedBlockLockerChestSigns.ChunkPosition(
                                    chunk.x(), chunk.z());
                            if (!source.equals(selected.get(key)) || !seen.add(key))
                                throw new IOException("SAVED selected Region/Chunk identity drift: "
                                        + world + "/" + key);
                            if (required.contains(key)) {
                                if (chunk.nbt() == null)
                                    throw new IOException("Missing saved sign-neighborhood terrain NBT");
                                var prior = byWorld.computeIfAbsent(
                                        world, ignored -> new HashMap<>())
                                        .putIfAbsent(key, new CapturedChunk(source, chunk));
                                if (prior != null)
                                    throw new IOException("Duplicate required SAVED terrain Chunk: "
                                            + world + "/" + key);
                            }
                        });
                Set<SavedBlockLockerChestSigns.ChunkPosition> expected = new HashSet<>();
                for (var candidate : selected.entrySet()) {
                    if (source.equals(candidate.getValue())) expected.add(candidate.getKey());
                }
                if (!seen.equals(expected))
                    throw new IOException("SAVED selected Region Chunk set changed: " + source.path());
            }
            if (byWorld.getOrDefault(world, Map.of()).size() != worldEntry.getValue().size())
                throw new IOException("Incomplete SAVED sign-neighborhood Chunk capture: " + world);
        }
        first.witness().requireUnchanged();

        List<ObservedChest> groups = new ArrayList<>();
        for (var worldEntry : byWorld.entrySet()) {
            String world = worldEntry.getKey();
            Map<SavedBlockLockerChestSigns.ChunkPosition, Map<String, Object>> terrain =
                    new HashMap<>();
            Map<SavedBlockLockerChestSigns.ChunkPosition,
                    WorldStorageDiscovery.SourceFile> sourceByChunk = new HashMap<>();
            Map<SavedBlockLockerChestSigns.Position, PhysicalChest> physical =
                    physicalByWorld.get(world);
            if (physical == null || physical.isEmpty())
                throw new IOException("SAVED terrain chest selection lost its physical source");
            for (var entry : worldEntry.getValue().entrySet()) {
                var captured = entry.getValue();
                var chunk = captured.chunk();
                terrain.put(entry.getKey(), chunk.nbt());
                sourceByChunk.put(entry.getKey(), captured.source());

            }
            Set<SavedBlockLockerChestSigns.Position> accounted = new HashSet<>();
            for (var position : physical.keySet().stream().sorted(POSITION_ORDER).toList()) {
                if (accounted.contains(position)) continue;
                var observation = SavedBlockLockerChestSigns.observeConfigured(terrain, position,
                        privateHeaders, moreHeaders, everyoneAliases, decoder,configured);
                for (var member : observation.storageMembers()) {
                    var located = physical.get(member);
                    if (located == null || !accounted.add(member))
                        throw new IOException("Unresolved or overlapping SAVED chest member: " + member);
                }
                // Include source evidence for negative sign observations too.
                var sources = observedNeighborhoodSources(observation, sourceByChunk);
                groups.add(new ObservedChest(world, sources, observation));
            }
            if (accounted.size() != physical.size())
                throw new IOException("Incomplete SAVED physical chest observation");
        }
        // A bounded final stamp cannot substitute for an exclusive writer epoch.
        first.witness().requireUnchanged();
        return new Snapshot(groups, first.witness(), first.report(),configured);
    }
}
