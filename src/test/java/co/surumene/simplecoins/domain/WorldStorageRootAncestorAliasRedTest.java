package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/** Candidate Red vectors: symlinks in ancestor components of a supplied world root. */
@EnabledOnOs(OS.LINUX)
class WorldStorageRootAncestorAliasRedTest {
    @TempDir Path temp;

    private static WorldScope allWorlds() {
        return new WorldScope(true, Set.of(), Set.of());
    }

    private static Path realWorld(Path temp) throws IOException {
        Path world = temp.resolve("real/world");
        Files.createDirectories(world.resolve("region"));
        return world;
    }

    private static Path aliasParent(Path temp) throws IOException {
        Path link = temp.resolve("alias");
        Files.createSymbolicLink(link, temp.resolve("real"));
        return link;
    }

    @Test void symlinkedAncestorOfWorldRootMustFailClosed() throws Exception {
        Path real = realWorld(temp);
        Files.write(real.resolve("region/r.0.0.mca"), new byte[8192]);
        Path alias = aliasParent(temp).resolve("world");
        assertTrue(Files.isDirectory(alias), "Precondition: ancestor alias resolves to a real world");
        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(Map.of("world", alias), allWorlds(), 100),
                "A linked ancestor must not bypass NOFOLLOW_LINKS on the final world-root component");
    }

    @Test void emptyWorldAliasThroughAncestorMustNotBeIndependent() throws Exception {
        Path real = realWorld(temp);
        Path alias = aliasParent(temp).resolve("world");
        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(Map.of("actual", real, "alias", alias),
                        allWorlds(), 100),
                "The same empty physical root must not become two independent world identities");
    }

    @Test void ordinaryRootWithoutLinksRemainsDiscoverable() throws Exception {
        Path real = realWorld(temp);
        Path region = real.resolve("region/r.0.0.mca");
        Files.write(region, new byte[8192]);
        var manifest = WorldStorageDiscovery.discover(
                Map.of("actual", real), allWorlds(), 100);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(region.toAbsolutePath(), manifest.worldFiles().getFirst().path());
    }
}
