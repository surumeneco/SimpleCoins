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
 * Candidate characterization of the existing WorldStorageDiscovery playerdata
 * authority rule; not a Confirmed CAT specification.
 */
class WorldStorageDualPlayerdataLayoutTest {
    @TempDir Path directory;

    @Test void sameUuidAcrossBothSupportedLayoutsFailsClosed() throws Exception {
        Path world = directory.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000051");
        Path vanilla = world.resolve("playerdata/" + player + ".dat");
        Path legacy = world.resolve("players/data/" + player + ".dat");
        Files.createDirectories(vanilla.getParent());
        Files.createDirectories(legacy.getParent());
        Files.writeString(vanilla, "vanilla");
        Files.writeString(legacy, "legacy");

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void distinctUuidsAcrossBothSupportedLayoutsArePreserved() throws Exception {
        Path world = directory.resolve("world");
        UUID vanillaPlayer = UUID.fromString("00000000-0000-0000-0000-000000000052");
        UUID legacyPlayer = UUID.fromString("00000000-0000-0000-0000-000000000053");
        Path vanilla = world.resolve("playerdata/" + vanillaPlayer + ".dat");
        Path legacy = world.resolve("players/data/" + legacyPlayer + ".dat");
        Files.createDirectories(vanilla.getParent());
        Files.createDirectories(legacy.getParent());
        Files.writeString(vanilla, "vanilla");
        Files.writeString(legacy, "legacy");

        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(Map.of(vanillaPlayer, vanilla.toAbsolutePath(),
                legacyPlayer, legacy.toAbsolutePath()), manifest.playerDataFiles());
        assertTrue(manifest.worldFiles().isEmpty());
    }
}
