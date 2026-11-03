package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression: distinct saved-source names must not allow one
 * physical Anvil file to enter the accounting manifest twice.
 * This test is not CAT semantic Gate approval or publication authority.
 */
class WorldStorageDiscoveryPhysicalAliasTest {
    @TempDir Path temporaryDirectory;

    @Test
    void hardLinkedRegionFilesAcrossDimensionsFailClosed() throws Exception {
        Path world = temporaryDirectory.resolve("world");
        Path overworld = world.resolve("region/r.0.0.mca");
        Path nether = world.resolve("DIM-1/region/r.1.0.mca");
        Files.createDirectories(overworld.getParent());
        Files.createDirectories(nether.getParent());
        Files.writeString(overworld, "fixture");
        try {
            Files.createLink(nether, overworld);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable on this filesystem: " + unsupported);
        }

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(
                        Map.of("world", world),
                        new WorldScope(true, Set.of(), Set.of()),
                        100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                () -> "Expected physical-source identity rejection, got: " + failure);
    }
}
