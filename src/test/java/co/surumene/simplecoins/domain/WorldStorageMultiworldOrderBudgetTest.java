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

/** Candidate regression for map-order-independent shared visit budget. */
class WorldStorageMultiworldOrderBudgetTest {
    @TempDir Path temp;

    @Test
    void bothRootOrdersExhaustOneSharedVisit() throws Exception {
        Path first = Files.createDirectory(temp.resolve("first"));
        Path second = Files.createDirectory(temp.resolve("second"));
        WorldScope scope = new WorldScope(true, Set.of(), Set.of());

        for (boolean reverse : new boolean[] {false, true}) {
            Map<String, Path> roots = roots(first, second, reverse);
            assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(roots, scope, 1));
        }
    }

    @Test
    void exclusionDoesNotAlterTwoVisitBudgetInEitherOrder() throws Exception {
        Path first = Files.createDirectory(temp.resolve("first"));
        Path second = Files.createDirectory(temp.resolve("second"));
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("second"));

        for (boolean reverse : new boolean[] {false, true}) {
            var manifest = WorldStorageDiscovery.discover(roots(first, second, reverse), scope, 2);
            assertTrue(manifest.worldFiles().isEmpty());
            assertTrue(manifest.playerDataFiles().isEmpty());
        }
    }

    private static Map<String, Path> roots(Path first, Path second, boolean reverse) {
        Map<String, Path> roots = new LinkedHashMap<>();
        if (reverse) {
            roots.put("second", second);
            roots.put("first", first);
        } else {
            roots.put("first", first);
            roots.put("second", second);
        }
        return roots;
    }
}
