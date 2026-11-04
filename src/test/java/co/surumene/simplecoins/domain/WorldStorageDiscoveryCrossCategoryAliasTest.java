package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression for the shared physical-source identity invariant.
 * No CAT semantic Gate or publication authority is implied.
 */
class WorldStorageDiscoveryCrossCategoryAliasTest {
    @TempDir Path temporaryDirectory;

    @Test
    void regionAndPlayerdataHardLinkCannotEnterOneManifestTwice() throws Exception {
        Path world = temporaryDirectory.resolve("world");
        Path region = world.resolve("region/r.0.0.mca");
        Path player = world.resolve("playerdata/")
                .resolve(UUID.fromString("00000000-0000-0000-0000-000000000001") + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(player.getParent());
        Files.writeString(region, "same physical source");
        try {
            Files.createLink(player, region);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable on this filesystem: " + unsupported);
        }

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(
                        Map.of("world", world),
                        new WorldScope(true, Set.of(), Set.of()),
                        100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                () -> "Expected shared-source identity rejection, got: " + failure);
    }

    @Test
    void twoCoordinateNamesInOneRegionDirectoryCannotAliasSameFile() throws Exception {
        Path world = temporaryDirectory.resolve("world");
        Path first = world.resolve("region/r.0.0.mca");
        Path second = world.resolve("region/r.1.0.mca");
        Files.createDirectories(first.getParent());
        Files.writeString(first, "same physical region");
        try {
            Files.createLink(second, first);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable on this filesystem: " + unsupported);
        }

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(
                        Map.of("world", world),
                        new WorldScope(true, Set.of(), Set.of()),
                        100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                () -> "Expected same-directory physical alias rejection, got: " + failure);
    }
}
