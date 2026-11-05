package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-003; CAT Draft simplecoins.phase5.discovery-visit-budget.
 * Existing-code behavior characterization, not a confirmed CAT or Paper Gate.
 */
class WorldStorageDiscoveryVisitBudgetTest {
    @TempDir Path directory;

    @Test void exactBudgetIncludesWorldRootRegionDirectoryAndFile() throws Exception {
        Path root = directory.resolve("single");
        Path region = root.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.writeString(region, "fixture");

        var scope = new WorldScope(true, Set.of(), Set.of());
        var sources = Map.of("single", root);
        var manifest = WorldStorageDiscovery.discover(sources, scope, 3);

        assertEquals(List.of(new WorldStorageDiscovery.SourceFile(
                "single", region.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                manifest.worldFiles());
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(sources, scope, 2),
                "World root and directory entries count against the visit budget");
    }

    @Test void excludedBackupTreesConsumeBudgetButNeverAddAccountingSources() throws Exception {
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        Path activeRegion = included.resolve("region/r.0.0.mca");
        Path backups = excluded.resolve("backups/export");
        Files.createDirectories(activeRegion.getParent());
        Files.createDirectories(backups);
        Files.writeString(activeRegion, "included asset source");
        for (int i = 0; i < 4; i++) {
            Files.writeString(backups.resolve("backup-" + i + ".txt"), "unrelated");
        }

        // Included root + region directory + region file = 3 visits.
        // Excluded root + backups + export + four unrelated files = 7 visits.
        var roots = Map.of("included", included, "excluded", excluded);
        var scope = new WorldScope(true, Set.of(), Set.of("excluded"));
        var manifest = WorldStorageDiscovery.discover(roots, scope, 10);
        assertEquals(List.of(new WorldStorageDiscovery.SourceFile(
                "included", activeRegion.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                manifest.worldFiles());
        assertTrue(manifest.playerDataFiles().isEmpty());

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(roots, scope, 9),
                "Excluding a world's assets cannot disable discovery resource bounds");
    }

    @Test void entirelyUnrelatedBackupTreeIsEmptyOnlyWhenFullyVisited() throws Exception {
        Path root = directory.resolve("backups-only");
        Path export = root.resolve("backups/export");
        Files.createDirectories(export);
        Files.writeString(export.resolve("old.txt"), "old");
        Files.writeString(export.resolve("manifest.json"), "{}");

        var roots = Map.of("backups-only", root);
        var scope = new WorldScope(true, Set.of(), Set.of());
        var manifest = WorldStorageDiscovery.discover(roots, scope, 5);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(roots, scope, 4),
                "Even non-authoritative files are charged before an empty manifest is returned");
    }
}
