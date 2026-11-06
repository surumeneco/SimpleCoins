package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Characterization candidates: excluded-world filtering must never suppress
 * world-independent playerdata validation or invent saved region assets.
 * These tests do not authorize publishing an asset inventory.
 */
class WorldStorageExcludedWorldPlayerdataValidationTest {
    @TempDir Path temp;

    @Test
    void invalidPlayerdataUuidInExcludedWorldFailsClosed() throws Exception {
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path playerdata = Files.createDirectory(excluded.resolve("playerdata"));
        Files.write(playerdata.resolve("not-a-uuid.dat"), new byte[] {1});
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("excluded"));

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("excluded", excluded), scope, 32));
    }

    @Test
    void noncanonicalPlayerdataUuidInExcludedWorldFailsClosed() throws Exception {
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path playerdata = Files.createDirectory(excluded.resolve("playerdata"));
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Files.write(playerdata.resolve(id.toString().toUpperCase() + ".dat"), new byte[] {1});
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("excluded"));

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("excluded", excluded), scope, 32));
    }

    @Test
    void excludedRegionDoesNotContributeEvenWhenFilenameIsMalformed() throws Exception {
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path region = Files.createDirectory(excluded.resolve("region"));
        Files.write(region.resolve("r.00.0.mca"), new byte[] {1});
        WorldScope scope = new WorldScope(true, Set.of(), Set.of("excluded"));

        WorldStorageDiscovery.Manifest manifest =
                WorldStorageDiscovery.discover(Map.of("excluded", excluded), scope, 32);
        assertEquals(0, manifest.worldFiles().size());
        assertEquals(0, manifest.playerDataFiles().size());
    }
}
