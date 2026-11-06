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
 * TASK-20261101-003: coordinate parsing must not silently discard a saved
 * source or accept two spellings for one authoritative Anvil region.
 */
class WorldStorageRegionCoordinateBoundaryTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.Manifest discover(String filename) throws IOException {
        Path world = directory.resolve("world");
        Path region = world.resolve("region");
        Files.createDirectories(region);
        Files.writeString(region.resolve(filename), "fixture");
        return WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
    }

    @Test void coordinateOverflowFailsClosedRatherThanOmittingSavedSource() {
        assertThrows(IOException.class, () -> discover("r.2147483648.0.mca"));
    }

    @Test void negativeCoordinateUnderflowFailsClosed() {
        assertThrows(IOException.class, () -> discover("r.-2147483649.0.mca"));
    }

    @Test void negativeZeroAliasFailsClosed() {
        assertThrows(IOException.class, () -> discover("r.-0.0.mca"));
    }

    @Test void signedIntBoundaryCoordinatesRemainValidSources() throws IOException {
        var manifest = discover("r.-2147483648.2147483647.mca");
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(WorldStorageDiscovery.Kind.REGION, manifest.worldFiles().get(0).kind());
    }
}
