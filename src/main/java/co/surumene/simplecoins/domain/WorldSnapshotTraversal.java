package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
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
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles()) {
            Path path = source.path().toAbsolutePath().normalize();
            if (!paths.add(path))
                throw new IOException("Duplicate world data source: " + path);
        }
        for (var entry : manifest.playerDataFiles().entrySet()) {
            Path raw = entry.getValue();
            if (!raw.getFileName().toString().equals(entry.getKey() + ".dat"))
                throw new IOException("Playerdata UUID and filename disagree: " + entry.getKey() + " vs " + raw);
            Path path = raw.toAbsolutePath().normalize();
            if (!paths.add(path))
                throw new IOException("Duplicate or overlapping playerdata source: " + path);
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
}
