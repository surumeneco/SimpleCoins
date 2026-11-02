package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec Candidate SC-NAME-01..06: observed source filename boundaries.
 * This test does not establish Paper snapshot completeness or CAT confirmation.
 */
class WorldStorageSourceFilenameBoundaryTest {
    @TempDir Path temp;

    private static final WorldScope ALL = new WorldScope(true, Set.of(), Set.of());
    private static final UUID PLAYER =
            UUID.fromString("abcdefab-cdef-abcd-efab-cdefabcdefab");

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[8192]);
    }

    private static WorldStorageDiscovery.Manifest discover(Path world) throws IOException {
        return WorldStorageDiscovery.discover(Map.of("world", world), ALL, 100);
    }

    @Test void extremeSignedIntRegionCoordinatesAreAccepted() throws Exception {
        Path world = temp.resolve("extremes");
        Path region = world.resolve("region/r.-2147483648.2147483647.mca");
        Path entity = world.resolve("entities/r.2147483647.-2147483648.mca");
        write(region);
        write(entity);
        var manifest = discover(world);
        assertEquals(Set.of(
                new WorldStorageDiscovery.SourceFile("world", region.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", entity.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.ENTITY)), Set.copyOf(manifest.worldFiles()));
        assertEquals(2, manifest.requiredSourceIds().size());
    }

    @Test void outOfRangeRegionCoordinatesFailClosed() throws Exception {
        int i = 0;
        for (String invalid : List.of(
                "r.2147483648.0.mca", "r.-2147483649.0.mca",
                "r.0.2147483648.mca", "r.0.-2147483649.mca")) {
            Path world = temp.resolve("overflow-" + i++);
            write(world.resolve("region/r.0.0.mca"));
            write(world.resolve("region/" + invalid));
            IOException error = assertThrows(IOException.class, () -> discover(world), invalid);
            assertTrue(error.getMessage().contains("Invalid Anvil region coordinates"), invalid);
        }
    }

    @Test void malformedRegionCoordinateShapeFailsClosed() throws Exception {
        int i = 0;
        for (String invalid : List.of(
                "r.1.2.3.mca", "r.1..mca", "r..2.mca", "r.1.2..mca")) {
            Path world = temp.resolve("malformed-" + i++);
            write(world.resolve("region/" + invalid));
            assertThrows(IOException.class, () -> discover(world), invalid);
        }
    }

    @Test void canonicalLowercaseAlphabeticUuidIsAccepted() throws Exception {
        Path world = temp.resolve("canonical-player");
        Path player = world.resolve("playerdata/" + PLAYER + ".dat");
        write(player);
        var manifest = discover(world);
        assertEquals(Map.of(PLAYER, player.toAbsolutePath()), manifest.playerDataFiles());
        assertTrue(manifest.worldFiles().isEmpty());
    }

    @Test void noncanonicalOrInvalidPlayerdataNamesFailClosed() throws Exception {
        int i = 0;
        for (String invalid : List.of(
                PLAYER.toString().toUpperCase(Locale.ROOT) + ".dat",
                "not-a-uuid.dat", ".dat")) {
            Path world = temp.resolve("bad-player-" + i++);
            write(world.resolve("playerdata/" + invalid));
            assertThrows(IOException.class, () -> discover(world), invalid);
        }
    }

    @Test void unrelatedBackupNamesDoNotBecomeAuthoritativeSources() throws Exception {
        Path world = temp.resolve("backups");
        Path region = world.resolve("region/r.0.0.mca");
        Path player = world.resolve("playerdata/" + PLAYER + ".dat");
        write(region);
        write(player);
        write(world.resolve("backups/region/r.2147483648.0.mca"));
        write(world.resolve("backups/playerdata/" + PLAYER + ".dat.bak"));
        var manifest = discover(world);
        assertEquals(List.of(new WorldStorageDiscovery.SourceFile("world",
                region.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                manifest.worldFiles());
        assertEquals(Map.of(PLAYER, player.toAbsolutePath()), manifest.playerDataFiles());
        assertEquals(2, manifest.requiredSourceIds().size());
    }
}
