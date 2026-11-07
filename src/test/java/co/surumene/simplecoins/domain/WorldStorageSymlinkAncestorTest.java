package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression for a symlink in an ancestor of an otherwise regular
 * world root. A NOFOLLOW_LINKS check on the final root alone is insufficient.
 * This test intentionally records the desired fail-closed behavior; it is not
 * evidence that the current implementation passes.
 */
class WorldStorageSymlinkAncestorTest {
    @TempDir Path directory;

    @Test
    void worldRootReachedThroughSymbolicAncestorMustFailClosed() throws Exception {
        Path actualParent = Files.createDirectory(directory.resolve("actual"));
        Path world = Files.createDirectory(actualParent.resolve("world"));
        Path region = world.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.writeString(region, "fixture");

        Path alias = Files.createSymbolicLink(directory.resolve("alias"), actualParent);
        Path rootThroughAlias = alias.resolve("world");
        // The final component is a real directory; only an ancestor is a link.
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", rootThroughAlias),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }
}
