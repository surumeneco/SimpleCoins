package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

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
 * TASK-20261101-003. Characterizes current source-shape behavior documented in
 * docs/cat/phase5-excluded-nonregular-source-identity.md (unconfirmed Draft).
 * No tests here claim a complete saved asset scan or authoritative CAT gate.
 */
class WorldStorageExcludedSourceShapeTest {
    @TempDir Path directory;

    private static WorldScope excludingWorld() {
        return new WorldScope(true, Set.of(), Set.of("excluded"));
    }

    @Test void excludedWorldMcaNamedDirectoriesAreNotTerrainSourcesButPlayerdataRemains() throws Exception {
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        Path includedRegion = included.resolve("region/r.0.0.mca");
        Path excludedRegionDirectory = excluded.resolve("region/r.1.0.mca");
        Path excludedEntityDirectory = excluded.resolve("entities/r.2.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000051");
        Path playerdata = excluded.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(includedRegion.getParent());
        Files.writeString(includedRegion, "fixture");
        Files.createDirectories(excludedRegionDirectory);
        Files.createDirectories(excludedEntityDirectory);
        Files.createDirectories(playerdata.getParent());
        Files.writeString(playerdata, "fixture");

        var manifest = WorldStorageDiscovery.discover(
                Map.of("included", included, "excluded", excluded), excludingWorld(), 100);
        assertEquals(List.of(new WorldStorageDiscovery.SourceFile(
                "included", includedRegion.toAbsolutePath(),
                WorldStorageDiscovery.Kind.REGION)), manifest.worldFiles());
        assertEquals(Map.of(player, playerdata.toAbsolutePath()), manifest.playerDataFiles());
    }

    @Test void includedWorldMcaNamedDirectoryAbortsEvenWithValidRegion() throws Exception {
        Path included = directory.resolve("included");
        Path validRegion = included.resolve("region/r.0.0.mca");
        Path invalidEntities = included.resolve("entities/r.1.0.mca");
        Files.createDirectories(validRegion.getParent());
        Files.writeString(validRegion, "fixture");
        Files.createDirectories(invalidEntities);

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("included", included), excludingWorld(), 100));
    }

    @Test void excludedWorldMalformedRegionNameDoesNotSuppressMalformedPlayerdata() throws Exception {
        Path excluded = directory.resolve("excluded");
        Path invalidRegion = excluded.resolve("region/r.+0.0.mca");
        Path invalidPlayerdata = excluded.resolve("playerdata/1-1-1-1-1.dat");
        Files.createDirectories(invalidRegion.getParent());
        Files.createDirectories(invalidPlayerdata.getParent());
        Files.writeString(invalidRegion, "ignored excluded-world region");
        Files.writeString(invalidPlayerdata, "invalid playerdata filename");

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded), excludingWorld(), 100));
    }

    @Test void excludedWorldSourceShapedSymlinkStillFailsClosed() throws Exception {
        Path excluded = directory.resolve("excluded");
        Path regionDirectory = excluded.resolve("region");
        Path target = excluded.resolve("backup.mca");
        Files.createDirectories(regionDirectory);
        Files.writeString(target, "fixture");
        Files.createSymbolicLink(regionDirectory.resolve("r.3.0.mca"), target);

        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded), excludingWorld(), 100));
    }
}
