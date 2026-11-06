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

}
