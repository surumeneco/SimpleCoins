package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * SC-BUDGET-01..03: draft characterization of the inclusive visit budget.
 * An unrelated filesystem entry must still consume budget.
 */
class WorldStorageVisitBudgetBoundaryTest {
    @TempDir Path temp;

    @Test void exactOneEntryBudgetAcceptsEmptyWorldRoot() throws Exception {
        Path world = Files.createDirectory(temp.resolve("world"));
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 1);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }

    @Test void unrelatedFileStillConsumesVisitBudget() throws Exception {
        Path world = Files.createDirectory(temp.resolve("world"));
        Files.writeString(world.resolve("notes.txt"), "not a settlement source");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 1));
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 2);
        assertEquals(0, manifest.worldFiles().size());
        assertEquals(0, manifest.playerDataFiles().size());
    }
}
