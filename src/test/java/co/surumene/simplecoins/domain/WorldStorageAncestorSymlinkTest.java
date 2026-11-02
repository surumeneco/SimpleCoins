package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-003 / SC-SOURCE-ANCESTOR-01..03 (CAT Draft).
 * Test preparation only. Normative Process/PI/TCE are not Confirmed and
 * formal CAT machine Gates have not run; do not merge this Red test.
 */
class WorldStorageAncestorSymlinkTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.Manifest discover(Path world) throws IOException {
        return WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
    }

    private Path regularWorld() throws IOException {
        Path world = directory.resolve("actual/world");
        Path region = world.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.write(region, new byte[8192]);
        return world;
    }

    @Test void ordinaryWorldRootStillDiscoversSavedRegion() throws Exception {
        Path world = regularWorld();
        var manifest = discover(world);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(world.resolve("region/r.0.0.mca").toAbsolutePath(),
                manifest.worldFiles().getFirst().path());
    }

    @Test void symlinkAsWorldRootItselfIsRejected() throws Exception {
        Path world = regularWorld();
        Path alias = Files.createSymbolicLink(directory.resolve("world-link"), world);
        assertThrows(IOException.class, () -> discover(alias));
    }

    @Test void symlinkInAncestorOfOtherwiseRegularWorldRootMustBeRejected() throws Exception {
        Path world = regularWorld();
        Path alias = Files.createSymbolicLink(directory.resolve("alias"), world.getParent());
        Path aliasedWorld = alias.resolve("world");
        // NOFOLLOW_LINKS on the final component does not inspect its ancestors.
        assertTrue(Files.isSymbolicLink(alias));
        assertFalse(Files.isSymbolicLink(aliasedWorld));
        assertTrue(Files.isDirectory(aliasedWorld, LinkOption.NOFOLLOW_LINKS));
        assertThrows(IOException.class, () -> discover(aliasedWorld),
                "A symlinked parent must not silently become an authoritative root");
    }

    @Test void excludedWorldPlayerdataStillRequiresTrustedAncestorPath() throws Exception {
        Path world = regularWorld();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000051");
        Path playerdata = world.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(playerdata.getParent());
        Files.writeString(playerdata, "fixture");
        Path alias = Files.createSymbolicLink(directory.resolve("alias"), world.getParent());
        Path aliasedWorld = alias.resolve("world");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", aliasedWorld),
                new WorldScope(false, Set.of(), Set.of("world")), 100));
    }
}
