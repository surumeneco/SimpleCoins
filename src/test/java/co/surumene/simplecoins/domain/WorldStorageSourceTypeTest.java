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

/** TASK-20261101-003: non-regular authoritative saved-source paths fail closed. */
class WorldStorageSourceTypeTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.Manifest discover(Path world) throws IOException {
        return WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
    }

    @Test void directoryNamedLikeAnvilRegionCannotMasqueradeAsZeroAssets() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve("region/r.0.0.mca"));
        assertThrows(IOException.class, () -> discover(world));
    }

    @Test void directoryNamedLikePlayerdataCannotMasqueradeAsZeroAssets() throws Exception {
        Path world = directory.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Files.createDirectories(world.resolve("playerdata/" + player + ".dat"));
        assertThrows(IOException.class, () -> discover(world));
    }

    @Test void unrelatedBackupDirectoriesDoNotBecomeAuthoritativeSources() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve("backups/r.0.0.mca"));
        var manifest = discover(world);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }

    @Test void regularAuthoritativeFilesStillAppearInTheManifest() throws Exception {
        Path world = directory.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Path region = world.resolve("region/r.0.0.mca");
        Path playerdata = world.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(playerdata.getParent());
        Files.write(region, new byte[8192]);
        Files.write(playerdata, new byte[]{1, 2, 3});
        var manifest = discover(world);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(region.toAbsolutePath(), manifest.worldFiles().getFirst().path());
        assertEquals(playerdata.toAbsolutePath(), manifest.playerDataFiles().get(player));
    }

    @Test void modernEntityRegionDirectoryCannotMasqueradeAsZeroEntities() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve(
                "dimensions/minecraft/the_nether/entities/r.0.0.mca"));
        assertThrows(IOException.class, () -> discover(world));
    }

    @Test void legacyPlayerdataDirectoryCannotMasqueradeAsZeroPlayers() throws Exception {
        Path world = directory.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Files.createDirectories(world.resolve("players/data/" + player + ".dat"));
        assertThrows(IOException.class, () -> discover(world));
    }

    @Test void excludedWorldRegionDirectoryIsNotAnAccountingSource() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve("region/r.0.0.mca"));
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(false, Set.of(), Set.of("world")), 100);
        assertTrue(manifest.worldFiles().isEmpty());
    }
}
