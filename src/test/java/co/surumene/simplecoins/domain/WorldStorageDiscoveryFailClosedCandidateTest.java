package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Candidate characterization of saved-source discovery fail-closed boundaries.
 * Does not authorize publication or establish a complete saved-asset snapshot.
 */
class WorldStorageDiscoveryFailClosedCandidateTest {
    @TempDir Path directory;

    private static WorldScope allWorlds() {
        return new WorldScope(true, Set.of(), Set.of());
    }

    @Test void missingWorldRootIsNotAnEmptyInventory() {
        Path absent = directory.resolve("missing-world");
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", absent), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("root missing"));
    }

    @Test void budgetExhaustionDoesNotReturnPartialManifest() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 2));
        assertTrue(failure.getMessage().contains("budget exhausted"));
    }

    @Test void malformedPlayerdataIsRejectedEvenInExcludedWorld() throws Exception {
        Path world = Files.createDirectory(directory.resolve("excluded-world"));
        Path players = Files.createDirectory(world.resolve("playerdata"));
        Files.write(players.resolve("not-a-uuid.dat"), new byte[] {1});
        WorldScope excluded = new WorldScope(false, Set.of(), Set.of("excluded"));
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("excluded", world), excluded, 100));
        assertTrue(failure.getMessage().contains("Invalid playerdata UUID"));
    }

    @Test void unrelatedFilesDoNotBecomeAuthoritativeSources() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path backups = Files.createDirectory(world.resolve("backups"));
        Path backupRegion = Files.createDirectory(backups.resolve("region"));
        Files.write(backupRegion.resolve("r.0.0.mca"), new byte[] {1});
        var manifest = WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100);
        assertEquals(0, manifest.worldFiles().size());
        assertEquals(0, manifest.playerDataFiles().size());
    }
    @Test void symbolicWorldRootIsRejectedBeforeTraversal() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real-world"));
        Path alias = Files.createSymbolicLink(directory.resolve("world-alias"), real);
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", alias), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("root missing or symbolic"));
    }

    @Test void crossCategoryHardlinkCannotBeCountedAsTwoSavedSources() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Path players = Files.createDirectory(world.resolve("playerdata"));
        Path source = region.resolve("r.0.0.mca");
        Files.write(source, new byte[] {1});
        Files.createLink(players.resolve("00000000-0000-0000-0000-000000000001.dat"), source);
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"));
    }

    @Test void excludedWorldPlayerdataSymlinkStillFailsClosed() throws Exception {
        Path world = Files.createDirectory(directory.resolve("excluded-world"));
        Path players = Files.createDirectory(world.resolve("playerdata"));
        Path external = Files.write(directory.resolve("outside.dat"), new byte[] {1});
        Files.createSymbolicLink(players.resolve("00000000-0000-0000-0000-000000000001.dat"), external);
        WorldScope excluded = new WorldScope(false, Set.of(), Set.of("excluded"));
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("excluded", world), excluded, 100));
        assertTrue(failure.getMessage().contains("Symbolic storage file"));
    }

    @Test void crossWorldHardlinkCannotBeCountedTwice() throws Exception {
        Path first = Files.createDirectory(directory.resolve("first"));
        Path second = Files.createDirectory(directory.resolve("second"));
        Path firstRegion = Files.createDirectory(first.resolve("region"));
        Path secondRegion = Files.createDirectory(second.resolve("region"));
        Path source = Files.write(firstRegion.resolve("r.0.0.mca"), new byte[] {1});
        Files.createLink(secondRegion.resolve("r.1.0.mca"), source);
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(
                        Map.of("first", first, "second", second), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"));
    }

    @Test void overlappingWorldRootsCannotBeCountedAsIndependentInventories() throws Exception {
        Path outer = Files.createDirectory(directory.resolve("outer-world"));
        Path nested = Files.createDirectory(outer.resolve("nested-world"));
        Path region = Files.createDirectory(nested.resolve("region"));
        Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(
                        Map.of("outer", outer, "nested", nested), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Overlapping world storage roots"));
    }

    @Test void directoryAtAuthoritativeRegionFilePathIsNotSilentlyOmitted() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Files.createDirectory(region.resolve("r.0.0.mca"));
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Non-regular authoritative storage source"));
    }


    @Test void noncanonicalUppercasePlayerUuidCannotAliasCanonicalPlayer() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path players = Files.createDirectory(world.resolve("playerdata"));
        Files.write(players.resolve("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA.dat"), new byte[] {1});
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Noncanonical playerdata UUID filename"));
    }

    @Test void noncanonicalRegionCoordinateCannotAliasCanonicalRegion() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Files.write(region.resolve("r.00.0.mca"), new byte[] {1});
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Noncanonical Anvil region filename"));
    }

    @Test void excludedWorldStillContributesPlayerdataButNotRegionAssets() throws Exception {
        Path world = Files.createDirectory(directory.resolve("excluded-world"));
        Path region = Files.createDirectory(world.resolve("region"));
        Path players = Files.createDirectory(world.resolve("playerdata"));
        Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        Path player = Files.write(players.resolve("00000000-0000-0000-0000-000000000001.dat"), new byte[] {2});
        WorldScope excluded = new WorldScope(false, Set.of(), Set.of("excluded"));
        var manifest = WorldStorageDiscovery.discover(Map.of("excluded", world), excluded, 100);
        assertEquals(0, manifest.worldFiles().size());
        assertEquals(Map.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), player),
                manifest.playerDataFiles());
    }

    @Test void dimensionEntityAndRegionSourcesRemainDistinctAndDiscoverable() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path dimension = Files.createDirectories(world.resolve("dimensions").resolve("example").resolve("moon"));
        Path region = Files.createDirectory(dimension.resolve("region"));
        Path entities = Files.createDirectory(dimension.resolve("entities"));
        Files.write(region.resolve("r.-1.2.mca"), new byte[] {1});
        Files.write(entities.resolve("r.-1.2.mca"), new byte[] {2});
        var manifest = WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100);
        assertEquals(2, manifest.worldFiles().size());
        assertEquals(Set.of(WorldStorageDiscovery.Kind.REGION, WorldStorageDiscovery.Kind.ENTITY),
                manifest.worldFiles().stream().map(WorldStorageDiscovery.SourceFile::kind)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, manifest.requiredSourceIds().size());
    }

    @Test void legacyPlayersDataIsDiscoveredAsAnAuthoritativePlayerSource() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path players = Files.createDirectories(world.resolve("players").resolve("data"));
        Path saved = Files.write(players.resolve("00000000-0000-0000-0000-000000000002.dat"),
                new byte[] {1});
        var manifest = WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100);
        assertEquals(Map.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), saved),
                manifest.playerDataFiles());
    }

    @Test void legacyAndModernPlayerdataForSameUuidMustFailClosed() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        Path modern = Files.createDirectory(world.resolve("playerdata"));
        Path legacy = Files.createDirectories(world.resolve("players").resolve("data"));
        String filename = "00000000-0000-0000-0000-000000000002.dat";
        Files.write(modern.resolve(filename), new byte[] {1});
        Files.write(legacy.resolve(filename), new byte[] {2});
        IOException failure = assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(Map.of("world", world), allWorlds(), 100));
        assertTrue(failure.getMessage().contains("Ambiguous playerdata authority"));
    }
}
