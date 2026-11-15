package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Read-only content witness for the discovered playerdata file set. It closes
 * the specific same-path rollback window in the saved issuance receipt audit;
 * it is NOT a filesystem freeze or proof of a Paper saveData transaction.
 */
public final class SavedPlayerdataContentWitness {
    private record Stamp(Object fileKey, long size, FileTime modified, String sha256) {}

    private final WorldStorageDiscovery.Manifest manifest;
    private final int maximumBytes;
    private final Map<Path, Stamp> observed;

    private SavedPlayerdataContentWitness(WorldStorageDiscovery.Manifest manifest,
                                          int maximumBytes, Map<Path, Stamp> observed) {
        this.manifest = manifest;
        this.maximumBytes = maximumBytes;
        this.observed = Map.copyOf(observed);
    }

    public static SavedPlayerdataContentWitness capture(
            WorldStorageDiscovery.Manifest manifest, int maximumCompressedBytes)
            throws IOException {
        if (manifest == null || manifest.origin() == null)
            throw new IOException("Unproven saved playerdata discovery cannot create a witness");
        if (maximumCompressedBytes < 1)
            throw new IllegalArgumentException("Invalid playerdata witness budget");
        manifest.requireUnchangedDiscovery();
        Map<Path, Stamp> observed = new HashMap<>();
        for (Path path : manifest.playerDataFiles().values()) {
            Path canonical = path.toAbsolutePath().normalize();
            if (observed.putIfAbsent(canonical, stamp(canonical, maximumCompressedBytes)) != null)
                throw new IOException("Duplicate playerdata witness path: " + canonical);
        }
        return new SavedPlayerdataContentWitness(manifest, maximumCompressedBytes, observed);
    }

    /**
     * Capture only one authoritative UUID file, not every other player's
     * data. The discovery provenance is still required and revalidated on
     * both sides so an omitted or newly inserted file cannot be passed off
     * as a complete current saved-file universe.
     */
    public static SavedPlayerdataContentWitness capturePlayer(
            WorldStorageDiscovery.Manifest manifest,
            java.util.UUID player, int maximumCompressedBytes) throws IOException {
        if (manifest == null || manifest.origin() == null || player == null)
            throw new IOException("Unproven playerdata source or owner");
        if (maximumCompressedBytes < 1)
            throw new IllegalArgumentException("Invalid playerdata witness byte budget");
        manifest.requireUnchangedDiscovery();
        Path path = manifest.playerDataFiles().get(player);
        if (path == null || !path.getFileName().toString().equals(player + ".dat"))
            throw new IOException("Missing authoritative playerdata file for " + player);
        Path canonical = path.toAbsolutePath().normalize();
        return new SavedPlayerdataContentWitness(manifest,
                maximumCompressedBytes,
                Map.of(canonical, stamp(canonical, maximumCompressedBytes)));
    }

    public void requireUnchanged() throws IOException {
        manifest.requireUnchangedDiscovery();
        for (var entry : observed.entrySet()) {
            Stamp now = stamp(entry.getKey(), maximumBytes);
            if (!entry.getValue().equals(now))
                throw new IOException("Authoritative saved playerdata changed during grant audit: "
                        + entry.getKey());
        }
    }

    private static Stamp stamp(Path file, int maximumBytes) throws IOException {
        // Caller-supplied paths cannot acquire a symbolic ancestor even if
        // the final file itself is regular and readable.
        for (Path parent = file; parent != null; parent = parent.getParent()) {
            if (Files.isSymbolicLink(parent))
                throw new IOException("Symbolic playerdata source or ancestor: " + parent);
        }
        BasicFileAttributes before = Files.readAttributes(
                file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() <= 0 || before.size() > maximumBytes)
            throw new IOException("Missing or out-of-budget saved playerdata: " + file);

        final MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        long count = 0;
        try (InputStream stream = Files.newInputStream(
                file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            byte[] block = new byte[8192];
            int n;
            while ((n = stream.read(block)) != -1) {
                if (n > maximumBytes - count)
                    throw new IOException("Saved playerdata grew while hashing: " + file);
                sha.update(block, 0, n);
                count += n;
            }
        }
        BasicFileAttributes after = Files.readAttributes(
                file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || count != before.size()
                || count != after.size()
                || !Objects.equals(before.fileKey(), after.fileKey())
                || !before.lastModifiedTime().equals(after.lastModifiedTime()))
            throw new IOException("Saved playerdata changed while hashing: " + file);
        return new Stamp(before.fileKey(), count, before.lastModifiedTime(),
                HexFormat.of().formatHex(sha.digest()));
    }
}
