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

/** SC-DUAL-01..04: source-observed dual-layout playerdata authority contract. */
class WorldStorageDualPlayerdataAuthorityTest {
    @TempDir Path directory;

    private static final UUID FIRST =
            UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID SECOND =
            UUID.fromString("00000000-0000-0000-0000-000000000052");

    private static Path writePlayer(Path world, String layout, UUID uuid) throws IOException {
        Path file = world.resolve(layout).resolve(uuid + ".dat");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "player " + uuid);
        return file;
    }

    private static WorldStorageDiscovery.Manifest scan(Path world, WorldScope scope)
            throws IOException {
        return WorldStorageDiscovery.discover(Map.of("world", world), scope, 100);
    }

    @Test void sameWorldLegacyAndModernCopiesForOneUuidFailClosed() throws Exception {
        Path world = directory.resolve("world");
        writePlayer(world, "playerdata", FIRST);
        writePlayer(world, "players/data", FIRST);

        assertThrows(IOException.class,
                () -> scan(world, new WorldScope(true, Set.of(), Set.of())));
    }

    @Test void independentUuidsAcrossBothLayoutsRemainDiscoverable() throws Exception {
        Path world = directory.resolve("world");
        Path modern = writePlayer(world, "playerdata", FIRST);
        Path legacy = writePlayer(world, "players/data", SECOND);

        var manifest = scan(world, new WorldScope(true, Set.of(), Set.of()));
        assertEquals(Map.of(FIRST, modern.toAbsolutePath(), SECOND, legacy.toAbsolutePath()),
                manifest.playerDataFiles());
        assertTrue(manifest.worldFiles().isEmpty());
        assertEquals(Set.of(modern.toAbsolutePath().toString(),
                        legacy.toAbsolutePath().toString()), manifest.requiredSourceIds());
    }

    @Test void excludedWorldDoesNotMaskDualLayoutConflict() throws Exception {
        Path world = directory.resolve("world");
        writePlayer(world, "playerdata", FIRST);
        writePlayer(world, "players/data", FIRST);

        assertThrows(IOException.class,
                () -> scan(world, new WorldScope(false, Set.of(), Set.of("world"))));
    }

    @Test void precedingRegionDoesNotPermitPartialManifestPublication() throws Exception {
        Path world = directory.resolve("world");
        Path region = world.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.write(region, new byte[8192]);
        writePlayer(world, "playerdata", FIRST);
        writePlayer(world, "players/data", FIRST);

        assertThrows(IOException.class,
                () -> scan(world, new WorldScope(true, Set.of(), Set.of())));
    }
}
