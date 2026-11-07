package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate regression coverage for fail-closed discovery of filesystem
 * indirections and bounded traversal. These tests do not authorize
 * publication or assert a completed saved-asset snapshot.
 */
class WorldStorageDiscoverySymlinkBudgetTest {
    @TempDir Path directory;

    @Test void excludedWorldSymlinkedPlayerdataCannotEscapeStorageRoot() throws Exception {
        Path excluded = directory.resolve("excluded");
        Path external = directory.resolve("external.dat");
        Files.createDirectories(excluded.resolve("playerdata"));
        Files.writeString(external, "outside snapshot");
        Files.createSymbolicLink(excluded.resolve("playerdata/00000000-0000-0000-0000-000000000101.dat"),
                external);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded),
                new WorldScope(false, Set.of(), Set.of("excluded")), 100));
    }

    @Test void unrelatedSymlinkedBackupCannotSilentlyBypassVisitSafety() throws Exception {
        Path root = directory.resolve("world");
        Path external = directory.resolve("external");
        Files.createDirectories(root.resolve("region"));
        Files.createDirectories(external);
        Files.writeString(root.resolve("region/r.0.0.mca"), "fixture");
        Files.createSymbolicLink(root.resolve("backup-link"), external);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void visitBudgetCountsDirectoriesAndFilesWithoutPublishingPartialManifest() throws Exception {
        Path root = directory.resolve("world");
        Path region = root.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.writeString(region, "fixture");
        // The root, region directory and file each consume one visit.
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 2));
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 3);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(region.toAbsolutePath(), manifest.worldFiles().get(0).path());
    }
}
