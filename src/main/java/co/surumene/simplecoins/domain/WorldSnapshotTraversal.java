package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded traversal of a previously discovered, stable on-disk snapshot.
 * Callbacks may STAGE values only. A successful traversal is not authorization
 * to publish settlement totals: world-memory reconciliation, ownership and
 * all required plugin-specific data sources must still be verified.
 */
public final class WorldSnapshotTraversal {
    private WorldSnapshotTraversal() {}

    public interface Visitor {
        void chunk(WorldStorageDiscovery.SourceFile source,
                   AnvilRegionReader.Chunk chunk) throws IOException;
        void player(UUID player, Map<String, Object> data) throws IOException;
    }

    /** A report exists only if every discovered input was read successfully. */
    public record Report(int regionFiles, int chunks, int playerFiles) {}

    /**
     * Re-discover a caller-defined authoritative saved-source set on both
     * sides of traversal. A new or missing file invalidates the staged report.
     *
     * This is a bounded best-effort check, not an atomic world snapshot:
     * callers must still freeze loaded sources and gate settlement publication.
     */
    public static Report traverseWithClosure(WorldStorageDiscovery.Manifest manifest,
                                             Map<String, Path> worldFolders,
                                             WorldScope scope, int maximumEntries,
                                             int maximumCompressedBytes,
                                             int maximumInflatedBytes,
                                             Visitor visitor) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(worldFolders, "worldFolders");
        Objects.requireNonNull(scope, "scope");
        // Freeze the caller's root inventory for this pair of checks.
        Map<String, Path> roots = Map.copyOf(worldFolders);
        // A stable list of saved files does not prove that an empty world root
        // has not been replaced while another world's sources are traversed.
        Map<String, RootIdentity> rootIdentities = new HashMap<>();
        Map<RootIdentity, String> physicalRootOwners = new HashMap<>();
        for (var entry : roots.entrySet()) {
            RootIdentity identity = rootIdentity(entry.getValue());
            String previous = physicalRootOwners.putIfAbsent(identity, entry.getKey());
            if (previous != null)
                throw new IOException("World roots alias one physical directory: " +
                        previous + " and " + entry.getKey());
            rootIdentities.put(entry.getKey(), identity);
        }
        requireSameSources(manifest,
                WorldStorageDiscovery.discover(roots, scope, maximumEntries));
        requireSameRoots(roots, rootIdentities);
        Report staged = traverse(manifest, maximumCompressedBytes,
                maximumInflatedBytes, visitor);
        requireSameSources(manifest,
                WorldStorageDiscovery.discover(roots, scope, maximumEntries));
        requireSameRoots(roots, rootIdentities);
        return staged;
    }

    /** Physical root identity is required even when a root has no saved files. */
    private record RootIdentity(Object fileKey, FileTime created) {}

    private static RootIdentity rootIdentity(Path root) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(root, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isDirectory() || attrs.fileKey() == null)
            throw new IOException("World root identity unavailable or symbolic: " + root);
        return new RootIdentity(attrs.fileKey(), attrs.creationTime());
    }

    private static void requireSameRoots(Map<String, Path> roots,
                                         Map<String, RootIdentity> expected) throws IOException {
        for (var entry : roots.entrySet()) {
            if (!expected.get(entry.getKey()).equals(rootIdentity(entry.getValue())))
                throw new IOException("World root changed during traversal: " + entry.getKey());
        }
    }

    private static void requireSameSources(WorldStorageDiscovery.Manifest expected,
                                           WorldStorageDiscovery.Manifest observed)
            throws IOException {
        if (!expected.worldFiles().equals(observed.worldFiles()) ||
                !expected.playerDataFiles().equals(observed.playerDataFiles()))
            throw new IOException("Authoritative saved-source set changed during traversal");
    }

    /**
     * An unhandled legacy Entity list is an incomplete asset source, not a
     * successful empty scan. Empty lists do not contribute assets.
     */
    private static void requireNoUnaccountedTerrainEntities(Map<String, Object> chunk)
            throws IOException {
        requireEmptyTerrainEntities(chunk.get("Entities"), "root");
        Object level = chunk.get("Level");
        if (level instanceof Map<?, ?> legacy)
            requireEmptyTerrainEntities(legacy.get("Entities"), "Level");
    }

    private static void requireEmptyTerrainEntities(Object value, String location)
            throws IOException {
        if (value == null) return;
        if (!(value instanceof List<?> entities) || !entities.isEmpty())
            throw new IOException("Unaccounted legacy terrain Entities at " + location);
    }

    public static Report traverse(WorldStorageDiscovery.Manifest manifest,
                                  int maximumCompressedBytes,
                                  int maximumInflatedBytes,
                                  Visitor visitor) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(visitor, "visitor");
        if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
            throw new IllegalArgumentException("Snapshot read budgets must be positive");

        // Reject an ambiguous manifest before invoking any callbacks.
        Set<Path> paths = new HashSet<>();
        Map<Object, Path> physicalKeys = new HashMap<>();
        List<Path> unknownKeys = new ArrayList<>();
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles()) {
            Path path = source.path().toAbsolutePath().normalize();
            String expectedDirectory = source.kind() == WorldStorageDiscovery.Kind.REGION
                    ? "region" : "entities";
            if (path.getParent() == null ||
                    !expectedDirectory.equals(path.getParent().getFileName().toString()))
                throw new IOException("World source kind disagrees with storage directory: " + path);
            requireCanonicalRegionName(path.getFileName().toString());
            if (!paths.add(path))
                throw new IOException("Duplicate world data source: " + path);
            requireUniquePhysicalFile(path, physicalKeys, unknownKeys);
        }
        for (var entry : manifest.playerDataFiles().entrySet()) {
            Path raw = entry.getValue();
            if (!raw.getFileName().toString().equals(entry.getKey() + ".dat"))
                throw new IOException("Playerdata UUID and filename disagree: " + entry.getKey() + " vs " + raw);
            Path path = raw.toAbsolutePath().normalize();
            if (!paths.add(path))
                throw new IOException("Duplicate or overlapping playerdata source: " + path);
            // Keep the existing later read failure for missing playerdata, but
            // reject physical aliases of present files before any callbacks.
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS))
                requireUniquePhysicalFile(path, physicalKeys, unknownKeys);
        }

        int files = 0, chunks = 0, players = 0;
        // Entity UUIDs are global identities, even across worlds and region files.
        // Per-chunk checks cannot detect copied entities in separate chunks.
        Set<UUID> savedEntityIds = new HashSet<>();
        // Revalidate all completed inputs after the last callback. A later
        // visitor may mutate a source that already passed its local check.
        Map<Path, SourceStamp> completedSources = new java.util.LinkedHashMap<>();
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles()) {
            SourceStamp before = stamp(source.path());
            final int[] fileChunks = {0};
            AnvilRegionReader.scan(source.path(), maximumCompressedBytes, maximumInflatedBytes,
                    chunk -> {
                        if (source.kind() == WorldStorageDiscovery.Kind.REGION) {
                            // Legacy terrain chunks may embed Entities rather than
                            // using the modern dedicated entity-region source.
                            // Until they have an authoritative accounting adapter,
                            // refuse to silently treat them as zero-valued.
                            requireNoUnaccountedTerrainEntities(chunk.nbt());
                        } else if (source.kind() == WorldStorageDiscovery.Kind.ENTITY) {
                            for (UUID uuid : SavedEntityUuidIntegrity.identities(chunk.nbt())) {
                                if (!savedEntityIds.add(uuid))
                                    throw new IOException("Duplicate saved Entity UUID across chunks: " + uuid);
                            }
                        }
                        visitor.chunk(source, chunk);
                        fileChunks[0] = Math.addExact(fileChunks[0], 1);
                    }, sidecar -> {
                        // The reader has validated this referenced sidecar.
                        // Physical aliases across all saved-source categories
                        // must not masquerade as independent inputs.
                        requireUniquePhysicalFile(sidecar, physicalKeys, unknownKeys);
                        completedSources.put(sidecar, stamp(sidecar));
                    });
            requireUnchanged(source.path(), before);
            completedSources.put(source.path(), before);
            files = Math.addExact(files, 1);
            chunks = Math.addExact(chunks, fileChunks[0]);
        }
        for (var entry : manifest.playerDataFiles().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
            SourceStamp before = stamp(entry.getValue());
            var data = PlayerDataReader.read(entry.getValue(),
                    maximumCompressedBytes, maximumInflatedBytes);
            visitor.player(entry.getKey(), data);
            requireUnchanged(entry.getValue(), before);
            completedSources.put(entry.getValue(), before);
            players = Math.addExact(players, 1);
        }
        // This detects observable changes to earlier sources during later
        // callbacks; it does not replace a frozen world/snapshot barrier.
        for (var entry : completedSources.entrySet())
            requireUnchanged(entry.getKey(), entry.getValue());
        return new Report(files, chunks, players);
    }

    /**
     * File paths are not physical identities: hard links can expose one saved
     * economic source under multiple otherwise valid names. Use file keys when
     * available and isSameFile for providers without them.
     */
    private static void requireUniquePhysicalFile(Path path,
                                                  Map<Object, Path> keyed,
                                                  List<Path> unkeyed) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile())
            throw new IOException("Snapshot source missing or symbolic: " + path);
        Object key = attrs.fileKey();
        if (key != null) {
            Path previous = keyed.putIfAbsent(key, path);
            if (previous != null)
                throw new IOException("Aliased physical snapshot source: " + previous + " and " + path);
            for (Path other : unkeyed)
                if (Files.isSameFile(other, path))
                    throw new IOException("Aliased physical snapshot source: " + other + " and " + path);
        } else {
            for (Path other : keyed.values())
                if (Files.isSameFile(other, path))
                    throw new IOException("Aliased physical snapshot source: " + other + " and " + path);
            for (Path other : unkeyed)
                if (Files.isSameFile(other, path))
                    throw new IOException("Aliased physical snapshot source: " + other + " and " + path);
            unkeyed.add(path);
        }
    }

    /**
     * Best-effort detection of concurrent saved-file changes during traversal.
     * Metadata stamps are not a substitute for a frozen Paper/world snapshot.
     */
    private record SourceStamp(Object key, long size, FileTime created, FileTime modified) {}

    private static SourceStamp stamp(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile())
            throw new IOException("Snapshot source is no longer a regular file: " + path);
        return new SourceStamp(attrs.fileKey(), attrs.size(),
                attrs.creationTime(), attrs.lastModifiedTime());
    }

    private static void requireUnchanged(Path path, SourceStamp before) throws IOException {
        if (!before.equals(stamp(path)))
            throw new IOException("Snapshot source changed during traversal: " + path);
    }

    /** Reject coordinate aliases even when a caller bypasses discovery. */
    private static void requireCanonicalRegionName(String filename) throws IOException {
        String[] parts = filename.split("\\.", -1);
        if (parts.length != 4 || !"r".equals(parts[0]) || !"mca".equals(parts[3]))
            throw new IOException("Invalid snapshot Anvil filename: " + filename);
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            if (!filename.equals("r." + x + "." + z + ".mca"))
                throw new IOException("Noncanonical snapshot Anvil filename: " + filename);
        } catch (NumberFormatException failure) {
            throw new IOException("Invalid snapshot Anvil coordinates: " + filename, failure);
        }
    }
}
