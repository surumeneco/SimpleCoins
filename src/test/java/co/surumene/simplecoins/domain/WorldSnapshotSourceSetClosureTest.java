package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: a new saved source must invalidate a staged manifest. */
class WorldSnapshotSourceSetClosureTest {
    @TempDir Path directory;

    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private Path player(UUID uuid) throws IOException {
        Path file = directory.resolve("world/playerdata/" + uuid + ".dat");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
            output.write(new byte[]{10, 0, 0, 0});
        }
        Files.write(file, bytes.toByteArray());
        return file;
    }

    private static WorldSnapshotTraversal.Visitor visitor() {
        return new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                        AnvilRegionReader.Chunk chunk) {}
            @Override public void player(UUID uuid, Map<String, Object> data) {}
        };
    }

    @Test void sourceAddedBeforeTraversalMustFailBeforeCallbacks() throws Exception {
        player(FIRST);
        Path world = directory.resolve("world");
        var roots = Map.of("world", world);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);
        player(SECOND);
        AtomicBoolean called = new AtomicBoolean();
        var callback = new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                        AnvilRegionReader.Chunk chunk) {}
            @Override public void player(UUID uuid, Map<String, Object> data) {
                called.set(true);
            }
        };
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverseWithClosure(
                manifest, roots, scope, 100, 4096, 4096, callback));
        assertFalse(called.get(), "Preflight closure must reject before staging assets");
    }

    @Test void sourceAddedDuringFinalCallbackMustFailAfterTraversal() throws Exception {
        player(FIRST);
        Path world = directory.resolve("world");
        var roots = Map.of("world", world);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverseWithClosure(
                manifest, roots, scope, 100, 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID uuid, Map<String, Object> data)
                            throws IOException {
                        WorldSnapshotSourceSetClosureTest.this.player(SECOND);
                    }
                }));
    }

    @Test void unchangedInventoryReturnsSuccessfulReport() throws Exception {
        player(FIRST);
        var roots = Map.of("world", directory.resolve("world"));
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);
        assertEquals(new WorldSnapshotTraversal.Report(0, 0, 1),
                WorldSnapshotTraversal.traverseWithClosure(
                        manifest, roots, scope, 100, 4096, 4096, visitor()));
    }

    @Test void excludedWorldRegionRemainsOutsideRequiredSourceSet() throws Exception {
        player(FIRST);
        var roots = Map.of("world", directory.resolve("world"));
        var scope = new WorldScope(false, Set.of(), Set.of("world"));
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);
        Path excluded = directory.resolve("world/region/r.0.0.mca");
        Files.createDirectories(excluded.getParent());
        Files.write(excluded, new byte[8192]);
        assertEquals(new WorldSnapshotTraversal.Report(0, 0, 1),
                WorldSnapshotTraversal.traverseWithClosure(
                        manifest, roots, scope, 100, 4096, 4096, visitor()));
    }

    @Test void recheckRespectsDiscoveryBudget() throws Exception {
        player(FIRST);
        var roots = Map.of("world", directory.resolve("world"));
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverseWithClosure(
                manifest, roots, scope, 1, 4096, 4096, visitor()));
    }
}
