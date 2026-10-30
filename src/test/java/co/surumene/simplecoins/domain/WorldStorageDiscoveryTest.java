package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldStorageDiscoveryTest {
    @TempDir Path directory;

    @Test void discoversModernAndLegacyWorldSourcesWithoutAssumingWorldNames() throws Exception {
        Path modern = directory.resolve("creative");
        Path legacy = directory.resolve("resource");
        Path modernRegions = modern.resolve("dimensions/minecraft/overworld/region/r.0.0.mca");
        Path modernEntities = modern.resolve("dimensions/minecraft/the_nether/entities/r.-1.1.mca");
        Path oldRegions = legacy.resolve("DIM-1/region/r.0.0.mca");
        for (Path file : new Path[]{modernRegions, modernEntities, oldRegions}) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }
        var files = WorldStorageDiscovery.discover(
                Map.of("creative", modern, "resource", legacy),
                new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(3, files.worldFiles().size());
        assertTrue(files.worldFiles().contains(new WorldStorageDiscovery.SourceFile(
                "creative", modernRegions.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)));
        assertTrue(files.worldFiles().contains(new WorldStorageDiscovery.SourceFile(
                "creative", modernEntities.toAbsolutePath(), WorldStorageDiscovery.Kind.ENTITY)));
        assertTrue(files.worldFiles().contains(new WorldStorageDiscovery.SourceFile(
                "resource", oldRegions.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)));
    }

    @Test void disabledWorldStillProvidesWorldIndependentPlayerdataCandidate() throws Exception {
        Path excluded = directory.resolve("off");
        UUID player = UUID.randomUUID();
        Path playerData = excluded.resolve("players/data/" + player + ".dat");
        Path region = excluded.resolve("region/r.0.0.mca");
        Files.createDirectories(playerData.getParent());
        Files.createDirectories(region.getParent());
        Files.writeString(playerData, "fixture");
        Files.writeString(region, "fixture");

        var files = WorldStorageDiscovery.discover(Map.of("off", excluded),
                new WorldScope(false, Set.of(), Set.of("off")), 100);
        assertTrue(files.worldFiles().isEmpty());
        assertEquals(playerData.toAbsolutePath(), files.playerDataFiles().get(player));
    }

    @Test void duplicatedPlayerdataMustBeResolvedBeforeAccounting() throws Exception {
        UUID player = UUID.randomUUID();
        Path a = directory.resolve("a"), b = directory.resolve("b");
        for (Path world : new Path[]{a, b}) {
            Path file = world.resolve("playerdata/" + player + ".dat");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("a", a, "b", b), new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void nestedWorldRootsCannotDoubleCountTheSameRegion() throws Exception {
        Path outer = directory.resolve("overworld");
        Path inner = outer.resolve("DIM1");
        Path region = inner.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.writeString(region, "fixture");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("outer", outer, "inner", inner),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void externalChunkSidecarsAreNotStandaloneRegionFiles() throws Exception {
        Path regionDirectory = directory.resolve("world/region");
        Files.createDirectories(regionDirectory);
        Path mca = regionDirectory.resolve("r.0.0.mca");
        Path sidecar = regionDirectory.resolve("c.0.0.mcc");
        Files.write(mca, new byte[8192]);
        Files.writeString(sidecar, "external chunk payload");
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", directory.resolve("world")),
                new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(1, manifest.worldFiles().size(),
                "An .mcc payload must only be read through its parent .mca header");
        assertEquals(mca.toAbsolutePath(), manifest.worldFiles().getFirst().path());
    }

    @Test void missingRootsAndBudgetExceededFailInsteadOfPublishingPartialCoverage() throws Exception {
        Path world = directory.resolve("world");
        Files.createDirectories(world.resolve("region"));
        for (int i = 0; i < 12; i++) Files.writeString(
                world.resolve("region/r." + i + ".0.mca"), "fixture");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 3));
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("missing", directory.resolve("missing")),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }
    @Test void ignoresBackupFoldersUnderAWorldInsteadOfCountingArchivedAssets() throws Exception {
        Path world = directory.resolve("world");
        UUID actualPlayer = UUID.randomUUID();
        UUID backupPlayer = UUID.randomUUID();
        Path liveRegion = world.resolve("region/r.0.0.mca");
        Path backupRegion = world.resolve("backups/region/r.0.0.mca");
        Path liveData = world.resolve("players/data/" + actualPlayer + ".dat");
        Path backupData = world.resolve("backups/playerdata/" + backupPlayer + ".dat");
        for (Path path : new Path[]{liveRegion, backupRegion, liveData, backupData}) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "fixture");
        }
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals(liveRegion.toAbsolutePath(), manifest.worldFiles().getFirst().path());
        assertEquals(Map.of(actualPlayer, liveData.toAbsolutePath()), manifest.playerDataFiles());
    }

}
