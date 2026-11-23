package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A caller may bypass discovery and supply a Manifest directly. The traversal
 * preflight must not follow symbolic aliases into another saved asset source.
 * This characterizes existing fail-closed behavior; it does not authorize
 * settlement publication or establish a coherent saved/live snapshot.
 */
class WorldSnapshotTraversalSymlinkBoundaryTest {
    @TempDir Path directory;

    private WorldSnapshotTraversal.Visitor countingVisitor(AtomicInteger callbacks) {
        return new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                        AnvilRegionReader.Chunk chunk) {
                callbacks.incrementAndGet();
            }
            @Override public void player(UUID player, Map<String, Object> data) {
                callbacks.incrementAndGet();
            }
        };
    }

    @Test void manuallySuppliedSymbolicRegionCannotReachReaderOrVisitor() throws Exception {
        Path real = directory.resolve("outside.mca");
        Files.write(real, new byte[] {1, 2, 3});
        Path alias = directory.resolve("world/region/r.0.0.mca");
        Files.createDirectories(alias.getParent());
        Files.createSymbolicLink(alias, real);

        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(new WorldStorageDiscovery.SourceFile(
                        "world", alias, WorldStorageDiscovery.Kind.REGION)), Map.of());
        AtomicInteger callbacks = new AtomicInteger();

        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, countingVisitor(callbacks)));
        assertEquals(0, callbacks.get(), "A symbolic region must fail during preflight");
    }

    @Test void manuallySuppliedSymbolicPlayerdataCannotRegisterAnOwner() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000711");
        Path real = directory.resolve("outside.dat");
        Files.write(real, new byte[] {1, 2, 3});
        Path alias = directory.resolve("players/data/" + player + ".dat");
        Files.createDirectories(alias.getParent());
        Files.createSymbolicLink(alias, real);

        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(player, alias));
        AtomicInteger callbacks = new AtomicInteger();

        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, countingVisitor(callbacks)));
        assertEquals(0, callbacks.get(), "A symbolic playerdata file must not register a UUID");
    }
}
