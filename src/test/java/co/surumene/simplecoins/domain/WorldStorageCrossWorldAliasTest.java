package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression coverage for the existing physical-source identity guard.
 * These cases assert the behavior of WorldStorageDiscovery without authorizing
 * settlement publication or changing any Confirmed CAT specification.
 */
class WorldStorageCrossWorldAliasTest {
    @TempDir Path directory;

    @Test void regionHardlinkAcrossDistinctWorldRootsMustFailClosed() throws Exception {
        Path first = directory.resolve("first/region/r.0.0.mca");
        Path second = directory.resolve("second/entities/r.9.-4.mca");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.write(first, new byte[8192]);
        Files.createLink(second, first);

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("first", first.getParent().getParent(),
                       "second", second.getParent().getParent()),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void crossWorldRegionPlayerdataHardlinkMustFailClosed() throws Exception {
        Path region = directory.resolve("included/region/r.0.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000049");
        Path data = directory.resolve("excluded/playerdata/" + player + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(data.getParent());
        Files.write(region, new byte[8192]);
        Files.createLink(data, region);

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("included", region.getParent().getParent(),
                       "excluded", data.getParent().getParent()),
                new WorldScope(true, Set.of(), Set.of("excluded")), 100));
    }

    @Test void distinctPhysicalFilesAcrossWorldsRemainIndependent() throws Exception {
        Path first = directory.resolve("first/region/r.0.0.mca");
        Path second = directory.resolve("second/entities/r.9.-4.mca");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.write(first, new byte[8192]);
        Files.write(second, new byte[8192]);

        var manifest = WorldStorageDiscovery.discover(
                Map.of("first", first.getParent().getParent(),
                       "second", second.getParent().getParent()),
                new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(2, manifest.worldFiles().size());
    }
}
