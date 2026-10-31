package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerDataHardlinkTest {
    @TempDir Path directory;

    @Test void distinctPlayerUuidsMustNotCountOnePhysicalPlayerdataFileTwice() throws Exception {
        Path world = directory.resolve("world");
        Path players = world.resolve("players/data");
        Files.createDirectories(players);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Path original = players.resolve(first + ".dat");
        Path alias = players.resolve(second + ".dat");
        Files.write(original, new byte[]{1, 2, 3});
        Files.createLink(alias, original);

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100));
    }
}
