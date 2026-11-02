package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-003 / SC-PHYSICAL-01..06 (CAT Draft).
 * Isolated regression for the already implemented physical source identity rule.
 * CAT semantic Gates and Paper acceptance are not yet complete.
 */
class WorldStoragePhysicalIdentityMatrixTest {
    @TempDir Path directory;

    private WorldStorageDiscovery.Manifest discover(Map<String, Path> roots, WorldScope scope)
            throws IOException {
        return WorldStorageDiscovery.discover(roots, scope, 100);
    }

    @Test void distinctPlayerUuidsHardLinkedToOneFileFailClosed() throws Exception {
        Path root = directory.resolve("world");
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000061");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000062");
        Path first = root.resolve("playerdata/" + a + ".dat");
        Path second = root.resolve("playerdata/" + b + ".dat");
        Files.createDirectories(first.getParent());
        Files.writeString(first, "same physical player data");
        Files.createLink(second, first);

        IOException failure = assertThrows(IOException.class, () -> discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of())));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                "Reject physical alias specifically, not an unrelated filesystem error");
    }

    @Test void regionAndEntityHardlinkFailClosed() throws Exception {
        Path root = directory.resolve("world");
        Path region = root.resolve("region/r.0.0.mca");
        Path entity = root.resolve("entities/r.1.0.mca");
        Files.createDirectories(region.getParent());
        Files.createDirectories(entity.getParent());
        Files.write(region, new byte[8192]);
        Files.createLink(entity, region);

        IOException failure = assertThrows(IOException.class, () -> discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of())));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                "Reject physical alias specifically, not an unrelated filesystem error");
    }

    @Test void identicalBytesInIndependentFilesRemainSeparateSources() throws Exception {
        Path root = directory.resolve("world");
        Path first = root.resolve("region/r.0.0.mca");
        Path second = root.resolve("region/r.1.0.mca");
        Files.createDirectories(first.getParent());
        byte[] bytes = new byte[8192];
        Files.write(first, bytes);
        Files.write(second, bytes);

        var manifest = discover(Map.of("world", root),
                new WorldScope(true, Set.of(), Set.of()));
        assertEquals(Set.of(
                new WorldStorageDiscovery.SourceFile("world", first.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", second.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION)),
                Set.copyOf(manifest.worldFiles()));
        assertEquals(2, manifest.requiredSourceIds().size());
    }

    @Test void excludedWorldRegionDoesNotBecomeMandatoryPhysicalSource() throws Exception {
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        Path primary = included.resolve("region/r.0.0.mca");
        Path alias = excluded.resolve("region/r.0.0.mca");
        Files.createDirectories(primary.getParent());
        Files.createDirectories(alias.getParent());
        Files.write(primary, new byte[8192]);
        Files.createLink(alias, primary);

        var manifest = discover(Map.of("included", included, "excluded", excluded),
                new WorldScope(true, Set.of(), Set.of("excluded")));
        assertEquals(Set.of(new WorldStorageDiscovery.SourceFile(
                "included", primary.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                Set.copyOf(manifest.worldFiles()));
        assertEquals(1, manifest.requiredSourceIds().size());
    }

    @Test void crossWorldPlayerdataHardlinkFailsClosed() throws Exception {
        Path a = directory.resolve("world-a");
        Path b = directory.resolve("world-b");
        UUID firstUuid = UUID.fromString("00000000-0000-0000-0000-000000000063");
        UUID secondUuid = UUID.fromString("00000000-0000-0000-0000-000000000064");
        Path first = a.resolve("playerdata/" + firstUuid + ".dat");
        Path alias = b.resolve("playerdata/" + secondUuid + ".dat");
        Files.createDirectories(first.getParent());
        Files.createDirectories(alias.getParent());
        Files.writeString(first, "physical source");
        Files.createLink(alias, first);

        IOException failure = assertThrows(IOException.class, () -> discover(
                Map.of("world-a", a, "world-b", b),
                new WorldScope(true, Set.of(), Set.of())));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                "Reject physical alias specifically, not an unrelated filesystem error");
    }

    @Test void excludedWorldPlayerdataAliasToIncludedRegionFailsClosed() throws Exception {
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000065");
        Path region = included.resolve("region/r.0.0.mca");
        Path alias = excluded.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(alias.getParent());
        Files.write(region, new byte[8192]);
        Files.createLink(alias, region);

        IOException failure = assertThrows(IOException.class, () -> discover(
                Map.of("included", included, "excluded", excluded),
                new WorldScope(true, Set.of(), Set.of("excluded"))));
        assertTrue(failure.getMessage().contains("Aliased physical authoritative source"),
                "Reject physical alias specifically, not an unrelated filesystem error");
    }
}
