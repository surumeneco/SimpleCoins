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

/** Candidate CAT Draft vectors SC-REGION-BOUND-01..05; not normative. */
class WorldStorageRegionCoordinateBoundaryTest {
    @TempDir Path temp;

    @Test
    void signedIntExtremesAreCanonicalRegionNames() throws Exception {
        for (String name : new String[] {
                "r.-2147483648.2147483647.mca",
                "r.2147483647.-2147483648.mca"
        }) {
            Path root = Files.createDirectory(temp.resolve("world-" + Math.abs(name.hashCode())));
            Path region = Files.createDirectory(root.resolve("region"));
            Path file = Files.writeString(region.resolve(name), "fixture");
            var manifest = WorldStorageDiscovery.discover(
                    Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 16);
            assertEquals(1, manifest.worldFiles().size(), name);
            assertEquals(file.toAbsolutePath(), manifest.worldFiles().getFirst().path(), name);
        }
    }

    @Test
    void outOfRangeAndNoncanonicalCoordinateAliasesFailClosed() throws Exception {
        for (String name : new String[] {
                "r.2147483648.0.mca",
                "r.-2147483649.0.mca",
                "r.+2147483647.0.mca"
        }) {
            Path root = Files.createDirectory(temp.resolve("world-" + Math.abs(name.hashCode())));
            Path region = Files.createDirectory(root.resolve("region"));
            Files.writeString(region.resolve(name), "fixture");
            assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                    Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 16), name);
        }
    }
}
