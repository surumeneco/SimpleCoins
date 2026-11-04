package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Candidate characterization of the global traversal budget, including excluded worlds. */
class WorldStorageExcludedBudgetBoundaryTest {
    @TempDir Path directory;

    @Test void excludedWorldUnrelatedFilesStillExhaustGlobalVisitBudget() throws Exception {
        Path excluded = directory.resolve("excluded");
        Path backup = excluded.resolve("backups");
        Files.createDirectories(backup);
        for (int i = 0; i < 4; i++) {
            Files.writeString(backup.resolve("cache-" + i + ".bin"), "not authoritative");
        }
        var scope = new WorldScope(false, Set.of(), Set.of("excluded"));
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded), scope, 3),
                "Excluded worlds and unrelated files must still consume the bounded scan budget");
        var manifest = WorldStorageDiscovery.discover(
                Map.of("excluded", excluded), scope, 6);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }

    @Test void visitBudgetCountsDirectoriesAndFilesAtExactBoundary() throws Exception {
        Path world = directory.resolve("world");
        Path region = world.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.writeString(region, "fixture");
        var scope = new WorldScope(true, Set.of(), Set.of());
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), scope, 2));
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), scope, 3);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(region.toAbsolutePath(), manifest.worldFiles().getFirst().path());
    }
}
