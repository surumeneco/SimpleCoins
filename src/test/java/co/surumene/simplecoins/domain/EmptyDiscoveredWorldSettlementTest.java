package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** An authentically discovered zero-input world is not a failed or missing scan. */
class EmptyDiscoveredWorldSettlementTest {
    @TempDir Path directory;

    private static WorldScope scope() {
        return new WorldScope(true, Set.of(), Set.of());
    }

    private static SettlementScanRunner.Staged stage(WorldStorageDiscovery.Manifest manifest)
            throws IOException {
        return SettlementScanRunner.stage(
                manifest, scope(), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(),
                65536, 65536);
    }

    @Test void validEmptyDiscoveredWorldIsACompletedZeroBalanceGeneration() throws Exception {
        Path world = Files.createDirectory(directory.resolve("new-world"));
        var manifest = WorldStorageDiscovery.discover(Map.of("new-world", world), scope(), 100);
        assertTrue(manifest.worldFiles().isEmpty());
        assertTrue(manifest.playerDataFiles().isEmpty());
        assertNotNull(manifest.origin());

        var staged = stage(manifest);
        assertEquals(0, staged.report().regionFiles());
        assertEquals(0, staged.report().playerFiles());
        assertNotNull(staged.savedContentWitness());
        assertEquals(BigInteger.ZERO, staged.pending().commit().statistics().worldTotal());
    }

    @Test void unprovenManualEmptyManifestIsNotSilentlyTreatedAsComplete() {
        var manual = new WorldStorageDiscovery.Manifest(List.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> stage(manual));
    }

    @Test void fileAppearingAfterEmptyDiscoveryInvalidatesZeroBalanceCandidate() throws Exception {
        Path world = Files.createDirectory(directory.resolve("new-world"));
        var manifest = WorldStorageDiscovery.discover(Map.of("new-world", world), scope(), 100);
        Path playerdata = world.resolve("playerdata");
        Files.createDirectories(playerdata);
        Files.write(playerdata.resolve(
                UUID.fromString("00000000-0000-0000-0000-000000009999") + ".dat"), new byte[0]);
        assertThrows(IOException.class, () -> stage(manifest));
    }
}
