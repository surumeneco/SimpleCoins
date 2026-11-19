package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Source-bound, read-only World UUID evidence for Paper 26.2 per-dimension
 * `data/paper/metadata.dat` (PaperWorldMetadata.TYPE / UUIDUtil.CODEC).
 *
 * Legacy uid.dat is NOT a Paper 26.2 authority. A verified WorldFolder
 * manifest and a stable metadata witness are required. Neither this class nor
 * a byte-fingerprint recheck proves that all worlds were discovered or that
 * other writers were frozen; it never authorizes settlement publication.
 */
public final class PaperSavedWorldMetadataWitness {
    private static final int MAX_COMPRESSED_BYTES = 256 * 1024;
    private static final int MAX_INFLATED_BYTES = 1024 * 1024;

    private record Metadata(Path path, UUID uuid, String sha256,
                            Object fileKey, long size) {
        Metadata {
            Objects.requireNonNull(path);
            Objects.requireNonNull(uuid);
            Objects.requireNonNull(sha256);
        }
    }

    /** Immutable observed world names, UUIDs, and disk fingerprints. */
    public static final class Witness {
        private final Map<String, Metadata> metadata;
        private final Map<String, UUID> worldIds;

        private Witness(Map<String, Metadata> metadata) {
            this.metadata = Map.copyOf(metadata);
            Map<String, UUID> ids = new HashMap<>();
            metadata.forEach((name, source) -> ids.put(name, source.uuid()));
            this.worldIds = Map.copyOf(ids);
        }

        public Map<String, UUID> worldIds() {
            return worldIds;
        }

        /** File rewrites, type/owner/mount changes or identity swaps fail. */
        public void requireUnchanged() throws IOException {
            for (var entry : metadata.entrySet()) {
                Metadata before = entry.getValue();
                Metadata after = read(entry.getKey(), before.path());
                if (!before.equals(after))
                    throw new IOException("Paper World metadata identity/source changed: "
                            + entry.getKey());
            }
        }
    }

    private PaperSavedWorldMetadataWitness() {}

    /**
     * Verify EVERY world root in the same discovered Manifest, including an
     * empty Entity region directory. A caller's expected UUIDs are claims
     * checked against disk, never accepted as direct source authority.
     *
     * No-origin/manual Manifests cannot establish persisted World identity.
     */
    public static Witness capture(WorldStorageDiscovery.Manifest manifest,
                                  Map<String, UUID> expectedWorldIds)
            throws IOException {
        Objects.requireNonNull(manifest, "discovered saved source Manifest");
        Objects.requireNonNull(expectedWorldIds, "caller World UUID claims");
        if (manifest.origin() == null)
            throw new IOException("Cannot infer World UUIDs from a manual saved Manifest");
        Map<String, Path> roots = manifest.origin().worldFolders();
        if (roots.isEmpty() || !expectedWorldIds.keySet().equals(roots.keySet()))
            throw new IOException("Paper saved World identity claims omit or invent a world");
        manifest.requireUnchangedDiscovery();
        Set<UUID> seen = new HashSet<>();
        Map<String, Metadata> captured = new HashMap<>();
        for (var entry : roots.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isBlank() || entry.getValue() == null)
                throw new IOException("Invalid Paper world name or root");
            Path root = entry.getValue().toAbsolutePath().normalize();
            Path metadataFile = root.resolve("data/paper/metadata.dat");
            Metadata fromDisk = read(name, metadataFile);
            if (!fromDisk.uuid().equals(expectedWorldIds.get(name)))
                throw new IOException("Expected World UUID disagrees with Paper 26.2 metadata: "
                        + name);
            if (!seen.add(fromDisk.uuid()))
                throw new IOException("Duplicate Paper World UUID across discovered worlds: "
                        + fromDisk.uuid());
            captured.put(name, fromDisk);
        }
        Witness witness = new Witness(captured);
        witness.requireUnchanged(); // detect changes during discovery/decoding
        return witness;
    }

    private static Metadata read(String world, Path source) throws IOException {
        Path path = source.toAbsolutePath().normalize();
        for (Path component = path; component != null; component = component.getParent())
            if (Files.isSymbolicLink(component))
                throw new IOException("Symbolic ancestor in Paper World metadata source: " + component);
        BasicFileAttributes before = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() < 1
                || before.size() > MAX_COMPRESSED_BYTES)
            throw new IOException("Missing/invalid/oversized Paper World metadata: " + path);

        ByteArrayOutputStream out = new ByteArrayOutputStream((int)Math.min(before.size(), 8192));
        try (var input = Files.newInputStream(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count > MAX_COMPRESSED_BYTES - out.size())
                    throw new IOException("Paper metadata exceeded read budget: " + path);
                out.write(buffer, 0, count);
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || out.size() != before.size()
                || before.size() != after.size()
                || !Objects.equals(before.fileKey(), after.fileKey())
                || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("Paper metadata changed while reading: " + path);

        byte[] bytes = out.toByteArray();
        Map<String, Object> decoded;
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            decoded = NbtReader.read(gzip, MAX_INFLATED_BYTES);
        } catch (RuntimeException bad) {
            throw new IOException("Malformed Paper 26.2 World metadata: " + path, bad);
        }
        if (!(decoded.get("data") instanceof Map<?, ?> data)
                || !(data.get("uuid") instanceof int[] parts)
                || parts.length != 4)
            throw new IOException("Missing PaperWorldMetadata.data.uuid int[4] at " + path);
        UUID id = new UUID(
                ((long)parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                ((long)parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
        if (id.equals(new UUID(0, 0)))
            throw new IOException("Nil Paper World UUID for " + world);
        try {
            return new Metadata(path, id, HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)),
                    after.fileKey(), after.size());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("JVM missing SHA-256", impossible);
        }
    }
}
