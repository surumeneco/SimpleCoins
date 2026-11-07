package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate characterization of the existing fail-closed Anvil filename parser.
 * This is not an approved specification or a saved-NBT reader acceptance test.
 */
class WorldStorageRegionCoordinateBoundaryCandidateTest {
    @TempDir Path directory;

    @Test void outOfRangeRegionCoordinatesMustNotBecomeUnaccountedSources() throws Exception {
        for (String sourceFolder : new String[]{"region", "entities"}) {
            for (String coordinate : new String[]{"2147483648", "-2147483649"}) {
                Path world = directory.resolve(sourceFolder + coordinate);
                Path file = world.resolve(sourceFolder + "/r." + coordinate + ".0.mca");
                Files.createDirectories(file.getParent());
                Files.writeString(file, "candidate fixture");
                assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                        Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100),
                        sourceFolder + " " + coordinate);
            }
        }
    }

    @Test void boundaryIntCoordinatesRemainDistinctCanonicalSources() throws Exception {
        Path world = directory.resolve("boundary");
        Path lower = world.resolve("region/r.-2147483648.0.mca");
        Path upper = world.resolve("entities/r.2147483647.0.mca");
        for (Path file : new Path[]{lower, upper}) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "candidate fixture");
        }
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(Set.of(
                new WorldStorageDiscovery.SourceFile("world", lower.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", upper.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.ENTITY)), Set.copyOf(manifest.worldFiles()));
    }
}
