package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Fail-closed manifest discovery for the settlement technical spike.
 *
 * Accept the actual world folders supplied by the caller (including Multiverse
 * worlds), rather than constructing fixed world/DIM paths. Discovers inputs
 * only: it does not parse MCA chunks, establish a consistent snapshot, or
 * grant permission to publish the resulting statistics.
 *
 * Playerdata is independent of the per-world accounting filter. Finding two
 * candidates for one player is an unresolved authority conflict, not permission
 * to select an arbitrary copy.
 */
public final class WorldStorageDiscovery {
    private WorldStorageDiscovery() {}

    public enum Kind { REGION, ENTITY }

    public record SourceFile(String world, Path path, Kind kind) {
        public SourceFile {
            Objects.requireNonNull(world);
            Objects.requireNonNull(path);
            Objects.requireNonNull(kind);
        }
    }

    public record Manifest(List<SourceFile> worldFiles, Map<UUID, Path> playerDataFiles) {
        public Manifest {
            worldFiles = List.copyOf(worldFiles);
            playerDataFiles = Map.copyOf(playerDataFiles);
        }
    }

    /**
     * Discover file candidates using bounded, non-symlink traversal. Every
     * error aborts discovery without publishing any partial manifest.
     *
     * maximumEntries limits ALL visited files/directories, including unrelated
     * paths; callers must choose a measured operational budget.
     */
    public static Manifest discover(Map<String, Path> worldFolders,
                                    WorldScope scope, int maximumEntries) throws IOException {
        Objects.requireNonNull(worldFolders);
        Objects.requireNonNull(scope);
        if (worldFolders.isEmpty() || maximumEntries < 1)
            throw new IllegalArgumentException("World folders and a positive visit budget are required");

        List<SourceFile> worldFiles = new ArrayList<>();
        Map<UUID, Path> players = new HashMap<>();
        Set<Path> worldRoots = new HashSet<>();
        int[] visited = {0};

        for (var entry : worldFolders.entrySet()) {
            String worldName = entry.getKey();
            Path rawRoot = entry.getValue();
            if (worldName == null || worldName.isBlank() || rawRoot == null)
                throw new IllegalArgumentException("Missing world name or folder");
            Path root = rawRoot.toAbsolutePath().normalize();
            for (Path existingRoot : worldRoots) {
                if (root.startsWith(existingRoot) || existingRoot.startsWith(root))
                    throw new IOException("Overlapping world storage roots: " + existingRoot + " and " + root);
            }
            worldRoots.add(root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("World storage root missing or symbolic: " + root);

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                private void charge(Path path) throws IOException {
                    if (++visited[0] > maximumEntries)
                        throw new IOException("Storage discovery visit budget exhausted at " + path);
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    charge(dir);
                    if (attrs.isSymbolicLink())
                        throw new IOException("World storage links are not valid snapshot roots: " + dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    charge(file);
                    if (attrs.isSymbolicLink())
                        throw new IOException("Symbolic storage file cannot be safely resolved: " + file);
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;

                    String filename = file.getFileName().toString();
                    String parent = file.getParent().getFileName().toString();
                    if (filename.endsWith(".dat") && isPlayerData(file, root)) {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(filename.substring(0, filename.length() - 4));
                        } catch (IllegalArgumentException badName) {
                            throw new IOException("Invalid playerdata UUID: " + file, badName);
                        }
                        Path previous = players.putIfAbsent(uuid, file);
                        if (previous != null)
                            throw new IOException("Ambiguous playerdata authority for " + uuid + ": " +
                                    previous + " and " + file);
                    } else if (scope.includes(worldName)
                            && filename.endsWith(".mca")) {
                        Kind kind = switch (parent) {
                            case "region" -> Kind.REGION;
                            case "entities" -> Kind.ENTITY;
                            default -> null;
                        };
                        if (kind != null && isExpectedWorldStorage(file, root))
                            worldFiles.add(new SourceFile(worldName, file, kind));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException failure) throws IOException {
                    throw failure;
                }
            });
        }

        worldFiles.sort(Comparator.comparing(SourceFile::world)
                .thenComparing(a -> a.path().toString())
                .thenComparing(SourceFile::kind));
        return new Manifest(worldFiles, players);
    }

    /**
     * A world root may contain unrelated backups, exports or caches. Only the
     * actual vanilla/Paper dimension layout is an authoritative data source.
     */
    private static boolean isExpectedWorldStorage(Path file, Path root) {
        Path relative = root.relativize(file.getParent());
        int size = relative.getNameCount();
        if (size == 1) return true; // root/region or root/entities
        if (size == 2) {
            String dimension = relative.getName(0).toString();
            return "DIM-1".equals(dimension) || "DIM1".equals(dimension);
        }
        return size == 4 && "dimensions".equals(relative.getName(0).toString())
                && !relative.getName(1).toString().isBlank()
                && !relative.getName(2).toString().isBlank();
    }

    private static boolean isPlayerData(Path file, Path root) {
        Path directory = file.getParent();
        return directory.equals(root.resolve("playerdata")) ||
                directory.equals(root.resolve("players").resolve("data"));
    }
}
