package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Draft SC-CONTAINER-01..04: a missing source directory is not zero assets. */
class WorldStorageSourceContainerShapeTest {
    @TempDir Path temp;

    private WorldStorageDiscovery.Manifest scan(Path root, boolean included) throws IOException {
        return WorldStorageDiscovery.discover(Map.of("world", root),
                new WorldScope(included, Set.of(), included ? Set.of() : Set.of("world")), 100);
    }

    @Test void regionContainerIsFile() throws Exception {
        Path root = Files.createDirectory(temp.resolve("region-world"));
        Files.writeString(root.resolve("region"), "not a directory");
        assertThrows(IOException.class, () -> scan(root, true));
    }

    @Test void playerdataContainerIsFile() throws Exception {
        Path root = Files.createDirectory(temp.resolve("player-world"));
        Files.writeString(root.resolve("playerdata"), "not a directory");
        assertThrows(IOException.class, () -> scan(root, false),
                "Playerdata is authoritative even when world terrain is excluded");
    }

    @Test void emptyDirectoriesAreValid() throws Exception {
        Path root = Files.createDirectory(temp.resolve("empty-world"));
        Files.createDirectories(root.resolve("region"));
        Files.createDirectories(root.resolve("entities"));
        var manifest = scan(root, true);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }

    @Test void excludedAndBackupAreIgnored() throws Exception {
        Path root = Files.createDirectory(temp.resolve("excluded-world"));
        Files.writeString(root.resolve("region"), "excluded terrain");
        Files.createDirectories(root.resolve("backups"));
        Files.writeString(root.resolve("backups/region"), "unrelated backup");
        var manifest = scan(root, false);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
    }
}
