package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: a stable source list is not proof of stable world roots. */
class WorldSnapshotRootIdentityClosureTest {
    @TempDir Path directory;
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    private Path player(Path world) throws IOException {
        Path file = world.resolve("playerdata/" + PLAYER + ".dat");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
            output.write(new byte[]{10, 0, 0, 0});
        }
        Files.write(file, bytes.toByteArray());
        return file;
    }

    @Test void replacingEmptyWorldRootWithSameSourceSetMustFail() throws Exception {
        Path first = directory.resolve("world-first");
        Path second = Files.createDirectory(directory.resolve("world-second"));
        player(first);
        var roots = Map.of("first", first, "second", second);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);

        // Both manifests have exactly the same single playerdata source.
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverseWithClosure(
                manifest, roots, scope, 100, 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID uuid, Map<String, Object> data)
                            throws IOException {
                        Files.move(second, directory.resolve("old-second"));
                        Files.createDirectory(second);
                    }
                }));
    }

    @Test void stableWorldRootsStillAllowSuccessfulReport() throws Exception {
        Path first = directory.resolve("world-first");
        Path second = Files.createDirectory(directory.resolve("world-second"));
        player(first);
        var roots = Map.of("first", first, "second", second);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);

        assertEquals(new WorldSnapshotTraversal.Report(0, 0, 1),
                WorldSnapshotTraversal.traverseWithClosure(
                        manifest, roots, scope, 100, 4096, 4096,
                        new WorldSnapshotTraversal.Visitor() {
                            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                        AnvilRegionReader.Chunk chunk) {}
                            @Override public void player(UUID uuid, Map<String, Object> data) {}
                        }));
    }
}
