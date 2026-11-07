package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate characterization of source authority boundaries. An excluded world
 * does not authorize MCA accounting, but its playerdata is still authoritative.
 * These cases are not a substitute for saved/live snapshot acceptance.
 */
class WorldStorageDiscoveryExcludedWorldAuthorityTest {
    @TempDir Path directory;

    @Test void excludedWorldIgnoresMalformedRegionNameButRetainsPlayerdata() throws Exception {
        Path world = directory.resolve("excluded");
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("playerdata"));
        Files.writeString(world.resolve("region/r.00.0.mca"), "not authoritative");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000123");
        Path playerFile = world.resolve("playerdata/" + player + ".dat");
        Files.writeString(playerFile, "player");
        var manifest = WorldStorageDiscovery.discover(
                Map.of("excluded", world),
                new WorldScope(false, Set.of(), Set.of("excluded")), 100);
        assertTrue(manifest.worldFiles().isEmpty());
        assertEquals(Map.of(player, playerFile), manifest.playerDataFiles());
        assertEquals(Set.of(playerFile.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
    }

    @Test void sameMalformedRegionNameFailsWhenWorldIsIncluded() throws Exception {
        Path world = directory.resolve("included");
        Files.createDirectories(world.resolve("region"));
        Files.writeString(world.resolve("region/r.00.0.mca"), "invalid alias");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("included", world),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }
}
