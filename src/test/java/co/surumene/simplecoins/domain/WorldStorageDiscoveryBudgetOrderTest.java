package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec characterization of the existing bounded discovery contract.
 * This does not promote a CAT Candidate to a normative Confirmed specification.
 */
class WorldStorageDiscoveryBudgetOrderTest {
    @TempDir Path temporary;

    private static WorldScope allWorlds() {
        return new WorldScope(true, Set.of(), Set.of());
    }

    private static Path region(Path root, String fileName) throws IOException {
        Path file = root.resolve("region").resolve(fileName);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fixture");
        return file;
    }

    @Test void exactlyThreeVisitsForRootRegionDirectoryAndRegionFileSucceed() throws Exception {
        Path root = temporary.resolve("world");
        Path source = region(root, "r.0.0.mca");

        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", root), allWorlds(), 3);

        assertEquals(List.of(new WorldStorageDiscovery.SourceFile(
                "world", source.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                manifest.worldFiles());
    }

    @Test void oneVisitBelowRequiredBudgetFailsWithoutReturningPartialManifest() throws Exception {
        Path root = temporary.resolve("world");
        region(root, "r.0.0.mca");

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(Map.of("world", root), allWorlds(), 2));
        assertTrue(failure.getMessage().contains("visit budget exhausted"), failure.getMessage());
    }

    @Test void unrelatedBackupEntriesConsumeTheSharedVisitBudget() throws Exception {
        Path root = temporary.resolve("world");
        Path live = region(root, "r.0.0.mca");
        Path backup = root.resolve("backups/archive");
        Files.createDirectories(backup);
        for (int i = 0; i < 8; i++) {
            Files.writeString(backup.resolve("unrelated-" + i + ".txt"), "not an asset");
        }

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(Map.of("world", root), allWorlds(), 5));
        assertTrue(failure.getMessage().contains("visit budget exhausted"), failure.getMessage());

        var sufficientBudget = WorldStorageDiscovery.discover(
                Map.of("world", root), allWorlds(), 100);
        assertEquals(1, sufficientBudget.worldFiles().size());
        assertEquals(live.toAbsolutePath(), sufficientBudget.worldFiles().getFirst().path());
        assertTrue(sufficientBudget.playerDataFiles().isEmpty());
    }

    @Test void sourceOrderingDoesNotDependOnWorldMapInsertionOrder() throws Exception {
        Path alpha = temporary.resolve("alpha-root");
        Path zeta = temporary.resolve("zeta-root");
        region(zeta, "r.3.0.mca");
        region(alpha, "r.2.0.mca");
        region(alpha, "r.1.0.mca");

        Map<String, Path> reversed = new LinkedHashMap<>();
        reversed.put("zeta", zeta);
        reversed.put("alpha", alpha);
        Map<String, Path> forward = new LinkedHashMap<>();
        forward.put("alpha", alpha);
        forward.put("zeta", zeta);

        var fromReverse = WorldStorageDiscovery.discover(reversed, allWorlds(), 100);
        var fromForward = WorldStorageDiscovery.discover(forward, allWorlds(), 100);

        assertEquals(fromForward.worldFiles(), fromReverse.worldFiles());
        assertEquals(fromForward.requiredSourceIds(), fromReverse.requiredSourceIds());
        assertEquals(List.of("alpha", "alpha", "zeta"),
                fromReverse.worldFiles().stream()
                        .map(WorldStorageDiscovery.SourceFile::world).toList());

        List<String> sourceIds = new ArrayList<>(fromReverse.requiredSourceIds());
        List<String> sorted = new ArrayList<>(sourceIds);
        sorted.sort(Comparator.naturalOrder());
        assertEquals(sorted, sourceIds);
    }
}
