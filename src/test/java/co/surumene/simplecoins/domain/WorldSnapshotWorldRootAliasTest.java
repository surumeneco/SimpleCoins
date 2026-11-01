package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: world labels must not alias the same physical root. */
class WorldSnapshotWorldRootAliasTest {
    @TempDir Path directory;

    @Test void distinctWorldLabelsCannotPointToSamePhysicalDirectory() throws Exception {
        Path actual = Files.createDirectory(directory.resolve("world"));
        Path alias = Files.createSymbolicLink(directory.resolve("alias"), directory)
                .resolve("world");
        var roots = Map.of("primary", actual, "duplicate", alias);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 100);

        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverseWithClosure(
                manifest, roots, scope, 100, 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {
                        fail("Aliased roots must be rejected before callbacks");
                    }
                    @Override public void player(UUID player, Map<String, Object> data) {
                        fail("Aliased roots must be rejected before callbacks");
                    }
                }));
    }
}
