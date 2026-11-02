package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate CAT Draft SC-PRESENCE-01..04.
 * A manually constructed manifest must not claim source closure for
 * missing, non-regular, or symbolic authoritative files.
 */
class WorldStorageManifestSourcePresenceTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.SourceFile region(Path path) {
        return new WorldStorageDiscovery.SourceFile(
                "world", path, WorldStorageDiscovery.Kind.REGION);
    }

    @Test void missingRegionCannotCloseManifest() {
        Path absent = directory.resolve("world/region/r.0.0.mca");
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(absent)), Map.of());
        assertFalse(Files.exists(absent));
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void directoryPlayerdataCannotCloseManifest() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000091");
        Path invalid = directory.resolve("world/playerdata/" + player + ".dat");
        Files.createDirectories(invalid);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(player, invalid));
        assertTrue(Files.isDirectory(invalid));
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void symlinkRegionCannotCloseManifest() throws Exception {
        Path actual = directory.resolve("source.mca");
        Files.write(actual, new byte[]{1, 2, 3});
        Path alias = directory.resolve("world/region/r.0.0.mca");
        Files.createDirectories(alias.getParent());
        Files.createSymbolicLink(alias, actual);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(alias)), Map.of());
        assertTrue(Files.isSymbolicLink(alias));
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void existingRegularSourcesRemainEligible() throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000092");
        Path region = directory.resolve("world/region/r.0.0.mca");
        Path playerdata = directory.resolve("world/playerdata/" + player + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(playerdata.getParent());
        Files.write(region, new byte[]{1, 2, 3});
        Files.write(playerdata, new byte[]{4, 5, 6});
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(region)), Map.of(player, playerdata));
        assertEquals(Set.of(
                region.toAbsolutePath().normalize().toString(),
                playerdata.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
    }
}
