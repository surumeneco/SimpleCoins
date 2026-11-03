package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression for the discovery-only contract: identical region
 * coordinates in different dimensions represent distinct saved sources.
 * Does not authorize publication or substitute for CAT semantic gates.
 */
class WorldStorageDiscoveryDimensionIdentityTest {
    @TempDir Path temporaryDirectory;

    @Test void sameCoordinatesAcrossDimensionsRemainDistinctAndDeterministic() throws Exception {
        Path world = temporaryDirectory.resolve("world");
        Path overworld = world.resolve("region/r.0.0.mca");
        Path nether = world.resolve("DIM-1/region/r.0.0.mca");
        Path end = world.resolve("DIM1/region/r.0.0.mca");
        Path custom = world.resolve("dimensions/custom/moon/region/r.0.0.mca");
        for (Path file : List.of(overworld, nether, end, custom)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }

        var expected = List.of(end, nether, custom, overworld).stream()
                .map(path -> new WorldStorageDiscovery.SourceFile(
                        "world", path.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION))
                .toList();
        var first = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
        var second = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);

        assertEquals(expected, first.worldFiles());
        assertEquals(first.worldFiles(), second.worldFiles());
        assertEquals(Set.of(
                overworld.toAbsolutePath().toString(),
                nether.toAbsolutePath().toString(),
                end.toAbsolutePath().toString(),
                custom.toAbsolutePath().toString()), first.requiredSourceIds());
    }
}
