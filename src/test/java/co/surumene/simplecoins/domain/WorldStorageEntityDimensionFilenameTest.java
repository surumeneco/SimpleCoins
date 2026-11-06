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
 * Characterization of authoritative entity-region paths across dimension layouts.
 * A malformed saved entity source must abort the entire manifest, not disappear.
 */
class WorldStorageEntityDimensionFilenameTest {
    @TempDir Path temp;

    private WorldStorageDiscovery.Manifest discover(String relative) throws IOException {
        Path world = temp.resolve("world");
        Path source = world.resolve(relative);
        Files.createDirectories(source.getParent());
        Files.writeString(source, "fixture");
        return WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
    }

    @Test void malformedNetherEntityCoordinateAbortsDiscovery() {
        assertThrows(IOException.class, () -> discover("DIM-1/entities/r.2147483648.0.mca"));
    }

    @Test void malformedCustomDimensionEntityAliasAbortsDiscovery() {
        assertThrows(IOException.class, () -> discover("dimensions/example/moon/entities/r.00.0.mca"));
    }

    @Test void canonicalCustomDimensionEntityRegionIsIncluded() throws IOException {
        var manifest = discover("dimensions/example/moon/entities/r.-1.2.mca");
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(WorldStorageDiscovery.Kind.ENTITY, manifest.worldFiles().get(0).kind());
    }
}
