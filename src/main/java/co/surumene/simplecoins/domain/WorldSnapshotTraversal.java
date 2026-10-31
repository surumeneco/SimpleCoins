package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
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
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles()) {
            final int[] fileChunks = {0};
            AnvilRegionReader.scan(source.path(), maximumCompressedBytes, maximumInflatedBytes,
                    chunk -> {
                        visitor.chunk(source, chunk);
                        fileChunks[0] = Math.addExact(fileChunks[0], 1);
                    });
            files = Math.addExact(files, 1);
            chunks = Math.addExact(chunks, fileChunks[0]);
        }
        for (var entry : manifest.playerDataFiles().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
            var data = PlayerDataReader.read(entry.getValue(),
                    maximumCompressedBytes, maximumInflatedBytes);
            visitor.player(entry.getKey(), data);
            players = Math.addExact(players, 1);
        }
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
}
