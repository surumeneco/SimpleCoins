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
 * Candidate regression: separate world names must not double-count one physical
 * authoritative Anvil source. This does not assert CAT Gate approval.
 */
class WorldStorageDiscoveryCrossWorldAliasTest {
    @TempDir Path temporaryDirectory;

    @Test
    void excludedWorldDoesNotMakeItsRegionAnAuthoritativeAlias() throws Exception {
        Path includedWorld = temporaryDirectory.resolve("included-world");
        Path excludedWorld = temporaryDirectory.resolve("excluded-world");
        Path included = includedWorld.resolve("region/r.0.0.mca");
        Path excluded = excludedWorld.resolve("region/r.0.0.mca");
        Files.createDirectories(included.getParent());
        Files.createDirectories(excluded.getParent());
        Files.writeString(included, "fixture");
        try {
            Files.createLink(excluded, included);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable on this filesystem: " + unsupported);
        }

        WorldStorageDiscovery.Manifest manifest = WorldStorageDiscovery.discover(
                Map.of("included", includedWorld, "excluded", excludedWorld),
                new WorldScope(true, Set.of(), Set.of("excluded")),
                100);
        assertTrue(manifest.worldFiles().stream().allMatch(source ->
                source.world().equals("included")));
        org.junit.jupiter.api.Assertions.assertEquals(1, manifest.worldFiles().size());
    }

    @Test
    void hardLinkedRegionAcrossDistinctWorldRootsFailsClosed() throws Exception {
        Path firstWorld = temporaryDirectory.resolve("first-world");
        Path secondWorld = temporaryDirectory.resolve("second-world");
        Path first = firstWorld.resolve("region/r.0.0.mca");
        Path second = secondWorld.resolve("region/r.0.0.mca");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, "fixture");
        try {
            Files.createLink(second, first);
        } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable on this filesystem: " + unsupported);
        }

        IOException failure = assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(
                        Map.of("first", firstWorld, "second", secondWorld),
                        new WorldScope(true, Set.of(), Set.of()),
                        100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                () -> "Expected cross-world physical-source rejection, got: " + failure);
    }
}
