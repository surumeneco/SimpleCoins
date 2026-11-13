package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A saved source manifest cannot be restaged under a different world filter. */
class SettlementDiscoveryScopeBindingTest {
    @TempDir Path directory;

    private Path emptyRegionWorld(String name) throws IOException {
        Path root = directory.resolve(name);
        Path region = root.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.write(region, new byte[8192]);
        return root;
    }

    @Test void discoveryExclusionsCannotBeReversedWhenStaging() throws Exception {
        Map<String, Path> worlds = Map.of(
                "anchor", emptyRegionWorld("anchor"),
                "world", emptyRegionWorld("world"));
        WorldScope discoveredScope = new WorldScope(false, Set.of("anchor"), Set.of());
        var manifest = WorldStorageDiscovery.discover(worlds, discoveredScope, 100);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals("anchor", manifest.worldFiles().getFirst().world());

        // The second scope would include the world omitted from discovery,
        // while excluding the world whose saved region was discovered.
        WorldScope incompatibleScope = new WorldScope(true, Set.of(), Set.of("anchor"));
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(
                manifest, incompatibleScope, Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                65536, 65536));
    }

    @Test void sameDiscoveredScopeStillPermitsReadOnlyStaging() throws Exception {
        Map<String, Path> worlds = Map.of(
                "anchor", emptyRegionWorld("anchor"),
                "world", emptyRegionWorld("world"));
        WorldScope scope = new WorldScope(false, Set.of("anchor"), Set.of());
        var manifest = WorldStorageDiscovery.discover(worlds, scope, 100);
        var staged = SettlementScanRunner.stage(
                manifest, scope, Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                65536, 65536);
        assertEquals(1, staged.report().regionFiles());
        assertEquals(java.math.BigInteger.ZERO, staged.pending().commit().statistics().worldTotal());
    }
}
