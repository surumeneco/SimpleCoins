package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Independent Linux filesystem verification of the existing non-symlink discovery boundary. */
@EnabledOnOs(OS.LINUX)
class WorldStorageSymlinkBoundaryVerificationTest {
    @TempDir Path temp;

    private static WorldScope allWorlds() {
        return new WorldScope(true, Set.of(), Set.of());
    }

    private static void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[]{1, 2, 3});
    }

    private static IOException reject(Path root, WorldScope scope) {
        return assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(Map.of("world", root), scope, 100));
    }

    private static void assertSymbolicFailure(IOException failure) {
        assertTrue(failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains("symbolic"),
                () -> "Expected symbolic-link rejection, got: " + failure.getMessage());
    }

    @Test void symbolicWorldRootIsRejected() throws Exception {
        Path actual = temp.resolve("actual-world");
        Files.createDirectories(actual);
        write(actual.resolve("region/r.0.0.mca"));
        Path alias = temp.resolve("linked-world");
        Files.createSymbolicLink(alias, actual);
        assertSymbolicFailure(reject(alias, allWorlds()));
    }

    @Test void symbolicAuthoritativeRegionIsRejected() throws Exception {
        Path root = temp.resolve("world");
        Path target = temp.resolve("external/r.0.0.mca");
        write(target);
        Path source = root.resolve("region/r.0.0.mca");
        Files.createDirectories(source.getParent());
        Files.createSymbolicLink(source, target);
        assertSymbolicFailure(reject(root, allWorlds()));
    }

    @Test void symbolicPlayerdataIsRejected() throws Exception {
        Path root = temp.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000061");
        Path target = temp.resolve("external/" + player + ".dat");
        write(target);
        Path source = root.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(source.getParent());
        Files.createSymbolicLink(source, target);
        assertSymbolicFailure(reject(root, allWorlds()));
    }

    @Test void symbolicRegionDirectoryIsRejected() throws Exception {
        Path root = temp.resolve("world");
        Path externalRegion = temp.resolve("external-region");
        write(externalRegion.resolve("r.0.0.mca"));
        Files.createDirectories(root);
        Files.createSymbolicLink(root.resolve("region"), externalRegion);
        assertSymbolicFailure(reject(root, allWorlds()));
    }

    @Test void symbolicBackupDirectoryIsRejected() throws Exception {
        Path root = temp.resolve("world");
        Path backup = temp.resolve("external-backup");
        Files.createDirectories(root);
        write(backup.resolve("region/r.0.0.mca"));
        Files.createSymbolicLink(root.resolve("backups"), backup);
        assertSymbolicFailure(reject(root, allWorlds()));
    }

    @Test void excludedWorldDoesNotBypassSymbolicLinkRejection() throws Exception {
        Path root = temp.resolve("excluded");
        Path target = temp.resolve("external/r.0.0.mca");
        write(target);
        Path source = root.resolve("region/r.0.0.mca");
        Files.createDirectories(source.getParent());
        Files.createSymbolicLink(source, target);
        assertSymbolicFailure(reject(root,
                new WorldScope(false, Set.of(), Set.of("world"))));
    }

    @Test void regularWorldSourcesRemainDiscoverable() throws Exception {
        Path root = temp.resolve("world");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000062");
        Path region = root.resolve("region/r.0.0.mca");
        Path data = root.resolve("playerdata/" + player + ".dat");
        write(region);
        write(data);
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", root), allWorlds(), 100);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(region.toAbsolutePath(), manifest.worldFiles().getFirst().path());
        assertEquals(Map.of(player, data.toAbsolutePath()), manifest.playerDataFiles());
    }
}
