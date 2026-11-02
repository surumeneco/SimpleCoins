package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Independent verification of existing discovery behavior, CAT Draft SC-ORDER/BUDGET/ROOT. */
class WorldStorageOrderBudgetVerificationTest {
    @TempDir Path temp;

    private static void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[8192]);
    }

    private static LinkedHashMap<String, Path> ordered(
            String a, Path pa, String b, Path pb, String c, Path pc) {
        LinkedHashMap<String, Path> roots = new LinkedHashMap<>();
        roots.put(a, pa);
        roots.put(b, pb);
        roots.put(c, pc);
        return roots;
    }

    @Test void inputWorldIterationOrderDoesNotChangeManifestOrSourceClosure() throws Exception {
        Path alpha = temp.resolve("alpha");
        Path beta = temp.resolve("beta");
        Path excluded = temp.resolve("excluded");
        UUID playerA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID playerE = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
        write(alpha.resolve("region/r.0.0.mca"));
        write(alpha.resolve("playerdata/" + playerA + ".dat"));
        write(beta.resolve("entities/r.-1.2.mca"));
        write(excluded.resolve("region/r.0.0.mca"));
        write(excluded.resolve("playerdata/" + playerE + ".dat"));

        WorldScope scope = new WorldScope(true, Set.of(), Set.of("excluded"));
        var forward = WorldStorageDiscovery.discover(
                ordered("alpha", alpha, "beta", beta, "excluded", excluded), scope, 100);
        var reverse = WorldStorageDiscovery.discover(
                ordered("excluded", excluded, "beta", beta, "alpha", alpha), scope, 100);

        assertEquals(forward.worldFiles(), reverse.worldFiles());
        assertEquals(forward.playerDataFiles(), reverse.playerDataFiles());
        assertEquals(List.copyOf(forward.requiredSourceIds()),
                List.copyOf(reverse.requiredSourceIds()));
        assertEquals(List.of("alpha", "beta"),
                forward.worldFiles().stream().map(WorldStorageDiscovery.SourceFile::world).toList());
        assertEquals(Set.of(playerA, playerE), forward.playerDataFiles().keySet(),
                "Excluded worlds still contribute world-independent playerdata");
        assertEquals(4, forward.requiredSourceIds().size());
        assertEquals(new ArrayList<>(new java.util.TreeSet<>(forward.requiredSourceIds())),
                new ArrayList<>(forward.requiredSourceIds()));
    }

    @Test void sameRegionCoordinatesInDifferentWorldsAreDistinctSources() throws Exception {
        Path a = temp.resolve("world-a");
        Path b = temp.resolve("world-b");
        Path regionA = a.resolve("region/r.0.0.mca");
        Path regionB = b.resolve("region/r.0.0.mca");
        write(regionA);
        write(regionB);

        var manifest = WorldStorageDiscovery.discover(Map.of("a", a, "b", b),
                new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(Set.of(
                new WorldStorageDiscovery.SourceFile("a", regionA.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("b", regionB.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION)), Set.copyOf(manifest.worldFiles()));
        assertEquals(2, manifest.requiredSourceIds().size(),
                "World identity, not only region coordinates, distinguishes sources");
    }

    @Test void excludedBackupTraversalStillConsumesGlobalVisitBudget() throws Exception {
        Path included = temp.resolve("included");
        Path excluded = temp.resolve("excluded");
        write(included.resolve("region/r.0.0.mca"));
        for (int i = 0; i < 20; i++)
            write(excluded.resolve("backups/file-" + i + ".bak"));
        LinkedHashMap<String, Path> roots = new LinkedHashMap<>();
        roots.put("included", included);
        roots.put("excluded", excluded);
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("excluded"));

        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(roots, scope, 12));
        assertTrue(failure.getMessage().contains("visit budget exhausted"),
                "Budget exhaustion must fail closed, not return partial assets");
    }

    @Test void identicalWorldRootUnderDifferentNamesIsRejectedInEitherOrder() throws Exception {
        Path root = temp.resolve("same-root");
        write(root.resolve("region/r.0.0.mca"));
        WorldScope scope = new WorldScope(true, Set.of(), Set.of());
        for (Map<String, Path> roots : List.of(
                Map.of("a", root, "b", root),
                ordered("b", root, "a", root, "c", temp.resolve("unused")))) {
            IOException failure = assertThrows(IOException.class,
                    () -> WorldStorageDiscovery.discover(roots, scope, 100));
            assertTrue(failure.getMessage().contains("Overlapping world storage roots"));
        }
    }
}
