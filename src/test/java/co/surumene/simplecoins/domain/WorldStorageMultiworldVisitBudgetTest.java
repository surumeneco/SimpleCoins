package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate characterization of one shared traversal budget across all world roots.
 * CAT normative Gates remain unexecuted.
 */
class WorldStorageMultiworldVisitBudgetTest {
    @TempDir Path temp;

    @Test
    void twoEmptyWorldRootsShareOneBudget() throws Exception {
        Path first = Files.createDirectory(temp.resolve("world_one"));
        Path second = Files.createDirectory(temp.resolve("world_two"));
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("one", first);
        roots.put("two", second);
        WorldScope scope = new WorldScope(true, Set.of(), Set.of());

        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(roots, scope, 1));
        var manifest = WorldStorageDiscovery.discover(roots, scope, 2);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }

    @Test
    void excludedWorldStillConsumesTraversalBudget() throws Exception {
        Path first = Files.createDirectory(temp.resolve("world_one"));
        Path second = Files.createDirectory(temp.resolve("world_two"));
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("one", first);
        roots.put("two", second);
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("two"));

        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(roots, scope, 1));
        var manifest = WorldStorageDiscovery.discover(roots, scope, 2);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }
}
