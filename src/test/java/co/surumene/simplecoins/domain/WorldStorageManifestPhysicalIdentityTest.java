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
 * Code-to-spec Candidate verification of the public Manifest boundary.
 * These tests intentionally expose physical aliases that lexical path
 * normalization alone cannot detect; they do not authorize settlement.
 */
class WorldStorageManifestPhysicalIdentityTest {
    @TempDir Path directory;

    @Test
    void hardLinkedWorldSourcesCannotBecomeTwoRequiredIds() throws Exception {
        Path region = directory.resolve("r.0.0.mca");
        Path alias = directory.resolve("r.1.0.mca");
        Files.writeString(region, "one physical saved source");
        Files.createLink(alias, region);

        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region,
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", alias,
                        WorldStorageDiscovery.Kind.ENTITY)), Map.of());

        assertThrows(IOException.class, manifest::requiredSourceIds,
                "Two hard links must not establish two independent mandatory sources");
    }

    @Test
    void hardLinkedWorldAndPlayerdataCannotBecomeTwoRequiredIds() throws Exception {
        Path region = directory.resolve("r.0.0.mca");
        Path playerdata = directory.resolve("player.dat");
        Files.writeString(region, "one physical saved source");
        Files.createLink(playerdata, region);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000055");

        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region,
                        WorldStorageDiscovery.Kind.REGION)), Map.of(player, playerdata));

        assertThrows(IOException.class, manifest::requiredSourceIds,
                "Source categories must not bypass physical duplicate detection");
    }

    @Test
    void symlinkAliasCannotBecomeSecondRequiredId() throws Exception {
        Path region = directory.resolve("r.0.0.mca");
        Path alias = directory.resolve("r.1.0.mca");
        Files.writeString(region, "one physical saved source");
        Files.createSymbolicLink(alias, region);

        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region,
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", alias,
                        WorldStorageDiscovery.Kind.ENTITY)), Map.of());

        assertThrows(IOException.class, manifest::requiredSourceIds,
                "A symlink alias must not establish a second mandatory source");
    }

    @Test
    void distinctPhysicalSourcesRetainIndependentRequiredIds() throws Exception {
        Path region = directory.resolve("r.0.0.mca");
        Path playerdata = directory.resolve("player.dat");
        Files.writeString(region, "world source");
        Files.writeString(playerdata, "player source");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000056");

        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region,
                        WorldStorageDiscovery.Kind.REGION)), Map.of(player, playerdata));

        Set<String> ids = manifest.requiredSourceIds();
        assertEquals(Set.of(region.toAbsolutePath().normalize().toString(),
                playerdata.toAbsolutePath().normalize().toString()), ids);
        assertThrows(UnsupportedOperationException.class, () -> ids.add("unexpected"));
    }
}
