package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

    /**
     * Still NOT a coherent-world snapshot proof: a file may change outside
     * the scan window, and Paper memory/external plugins require reconciliation.
     * Compare content as well as metadata, to detect same-size/mtime rewrites.
     */
    private record SourceStamp(Object physicalKey, long size,
                               java.nio.file.attribute.FileTime modified, String sha256) {}

    private static SourceStamp stamp(Path path) throws IOException {
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile())
            throw new IOException("Snapshot source disappeared or ceased being regular: " + path);
        String fingerprint = contentFingerprint(path, before.size());
        // Guard the fingerprint read itself. A file swapping identities or
        // changing size/mtime during the digest is not a stable source.
        BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || !Objects.equals(before.fileKey(), after.fileKey())
                || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("Snapshot source changed while fingerprinting: " + path);
        return new SourceStamp(before.fileKey(), before.size(),
                before.lastModifiedTime(), fingerprint);
    }

    /** Stream with a fixed 8 KiB buffer; no entire MCA/playerdata in memory. */
    private static String contentFingerprint(Path path, long expectedBytes) throws IOException {
        MessageDigest sha256;
        try { sha256 = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        long readBytes = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(path,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            int n;
            while ((n = input.read(buffer)) != -1) {
                if (n > expectedBytes - readBytes)
                    throw new IOException("Snapshot source grew during fingerprinting: " + path);
                sha256.update(buffer, 0, n);
                readBytes += n;
            }
        }
        if (readBytes != expectedBytes)
            throw new IOException("Snapshot source shrank during fingerprinting: " + path);
        return HexFormat.of().formatHex(sha256.digest());
    }

    public static Report traverse(WorldStorageDiscovery.Manifest manifest,
                                  int maximumCompressedBytes,
                                  int maximumInflatedBytes,
                                  Visitor visitor) throws IOException {
        return traverse(manifest, maximumCompressedBytes, maximumInflatedBytes,
                visitor, null);
    }

    /**
     * Route saved NBT callbacks through an explicit saved/live partition,
     * without skipping any saved-file parsing or drift/integrity inspection.
     * Before calling this, the caller must stage and acknowledge all selected
     * LIVE sources (including their actual values) in the same validated
     * settlement generation. A successful report is still not authority
     * to publish or admit players: Paper coherence is not proven here.
     */
    public static Report traverse(WorldStorageDiscovery.Manifest manifest,
                                  int maximumCompressedBytes,
                                  int maximumInflatedBytes,
                                  Visitor visitor,
                                  SavedLiveSourcePartition partition) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(visitor, "visitor");
        if (maximumCompressedBytes < 1 || maximumInflatedBytes < 1)
            throw new IllegalArgumentException("Snapshot read budgets must be positive");

        // Reject an ambiguous manifest before invoking any callbacks.
        Set<Path> paths = new HashSet<>();
        // Store the exact preflight source set, including absent mandatory
        // playerdata. A late-appearing file is not part of the input snapshot.
        Map<Path, SourceStamp> before = new HashMap<>();
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
            before.put(path, stamp(path));
            // Oversized Anvil chunks store their real compressed NBT in
            // separate c.x.z.mcc files. These are authoritative sub-sources
            // even though the manifest's required source ID is the .mca.
            for (Path sidecar : AnvilRegionReader.externalSidecars(path)) {
                if (!paths.add(sidecar))
                    throw new IOException("Duplicate external saved source: " + sidecar);
                requireUniquePhysicalFile(sidecar, physicalKeys, unknownKeys);
                before.put(sidecar, stamp(sidecar));
            }
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
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireUniquePhysicalFile(path, physicalKeys, unknownKeys);
                before.put(path, stamp(path));
            } else {
                before.put(path, null);
            }
        }

        int files = 0, chunks = 0, players = 0;
        // Entity UUIDs are global identities, even across worlds and region files.
        // Per-chunk checks cannot detect copied entities in separate chunks.
        Set<UUID> savedEntityIds = new HashSet<>();
        for (WorldStorageDiscovery.SourceFile source : manifest.worldFiles()) {
            final int[] fileChunks = {0};
            AnvilRegionReader.scan(source.path(), maximumCompressedBytes, maximumInflatedBytes,
                    chunk -> {
                        if (source.kind() == WorldStorageDiscovery.Kind.ENTITY) {
                            for (UUID uuid : SavedEntityUuidIntegrity.identities(chunk.nbt())) {
                                if (!savedEntityIds.add(uuid))
                                    throw new IOException("Duplicate saved Entity UUID across chunks: " + uuid);
                            }
                        }
                        if (partition == null ||
                                partition.savedChunk(source.world(), chunk.x(), chunk.z(),
                                        source.kind()) == SavedLiveSourcePartition.Authority.SAVED)
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
            if (partition == null ||
                    partition.savedPlayer(entry.getKey()) == SavedLiveSourcePartition.Authority.SAVED)
                visitor.player(entry.getKey(), data);
            players = Math.addExact(players, 1);
        }
        // Existing input hashes alone cannot detect a new MCA/UUID.dat file
        // appearing in an otherwise untouched source directory.
        // Revisit the complete original source-root discovery before final
        // content hashes. A hand-constructed fixture cannot claim this proof.
        manifest.requireUnchangedDiscovery();
        // Check EVERY authoritative input again, including already visited
        // regions. The caller must discard all staged values on any mismatch.
        // This is a drift detector, not a substitute for freezing Paper worlds.
        for (var entry : before.entrySet()) {
            if (entry.getValue() == null)
                throw new IOException("Required playerdata appeared during snapshot traversal: "
                        + entry.getKey());
            SourceStamp observed = stamp(entry.getKey());
            if (!entry.getValue().equals(observed))
                throw new IOException("Authoritative saved source changed during traversal: "
                        + entry.getKey());
        }
        if (partition != null) partition.finish();
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
