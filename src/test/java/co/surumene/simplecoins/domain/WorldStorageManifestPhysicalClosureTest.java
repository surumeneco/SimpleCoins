package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-003 / SC-MANIFEST-01..04.
 * CAT Draft-only tests for manually constructed manifest closure.
 * These do not authorize a production implementation before formal Gates.
 */
class WorldStorageManifestPhysicalClosureTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.SourceFile region(Path file) {
        return new WorldStorageDiscovery.SourceFile(
                "world", file, WorldStorageDiscovery.Kind.REGION);
    }

    @Test void hardlinkedRegionPathsInManualManifestCannotClose() throws Exception {
        Path first = directory.resolve("region/r.0.0.mca");
        Path alias = directory.resolve("region/r.1.0.mca");
        Files.createDirectories(first.getParent());
        Files.write(first, new byte[8192]);
        Files.createLink(alias, first);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(first), region(alias)), Map.of());
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void hardlinkedRegionAndPlayerdataInManualManifestCannotClose() throws Exception {
        Path first = directory.resolve("region/r.0.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000073");
        Path alias = directory.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(first.getParent());
        Files.createDirectories(alias.getParent());
        Files.write(first, new byte[8192]);
        Files.createLink(alias, first);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(first)), Map.of(player, alias));
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void identicalContentIndependentFilesRemainDistinct() throws Exception {
        Path first = directory.resolve("region/r.0.0.mca");
        Path second = directory.resolve("region/r.1.0.mca");
        Files.createDirectories(first.getParent());
        byte[] bytes = new byte[8192];
        Files.write(first, bytes);
        Files.write(second, bytes);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(first), region(second)), Map.of());
        assertEquals(Set.of(first.toAbsolutePath().toString(),
                        second.toAbsolutePath().toString()),
                manifest.requiredSourceIds());
    }

    @Test void identicalLexicalPathStillRejected() throws Exception {
        Path first = directory.resolve("region/r.0.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000074");
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(region(first)), Map.of(player, first));
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }
}
