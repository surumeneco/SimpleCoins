package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Code-to-spec characterization of WorldStorageDiscovery.Manifest metadata only.
 *
 * These tests do not prove physical file identity, disk snapshot consistency,
 * complete saved-asset inventory, or settlement publication authorization.
 * Normative CAT promotion and the remaining Phase 5 Gates are separate.
 */
class WorldStorageManifestClosureSnapshotCandidateTest {
    @TempDir Path directory;

    @Test void normalizationRejectsTwoLexicalRegionAliases() {
        Path canonical = directory.resolve("world/region/r.0.0.mca");
        Path alias = directory.resolve("world/region/../region/r.0.0.mca");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile(
                        "world", canonical, WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile(
                        "world", alias, WorldStorageDiscovery.Kind.REGION)), Map.of());

        assertEquals(canonical.toAbsolutePath().normalize(),
                alias.toAbsolutePath().normalize());
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void normalizationRejectsCrossCategoryLexicalAlias() {
        Path canonical = directory.resolve("world/region/r.1.-2.mca");
        Path alias = directory.resolve("world/playerdata/../region/r.1.-2.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000071");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile(
                        "world", canonical, WorldStorageDiscovery.Kind.REGION)),
                Map.of(player, alias));

        assertEquals(canonical.toAbsolutePath().normalize(),
                alias.toAbsolutePath().normalize());
        assertThrows(IOException.class, manifest::requiredSourceIds);
    }

    @Test void callerMutationCannotRewriteManifestOrClosure() throws Exception {
        Path region = directory.resolve("world/region/r.0.0.mca");
        Path playerdata = directory.resolve(
                "world/playerdata/00000000-0000-0000-0000-000000000072.dat");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000072");
        var original = new WorldStorageDiscovery.SourceFile(
                "world", region, WorldStorageDiscovery.Kind.REGION);
        List<WorldStorageDiscovery.SourceFile> callerSources = new ArrayList<>();
        callerSources.add(original);
        Map<UUID, Path> callerPlayers = new HashMap<>();
        callerPlayers.put(player, playerdata);
        var manifest = new WorldStorageDiscovery.Manifest(callerSources, callerPlayers);

        callerSources.clear();
        callerPlayers.clear();
        assertEquals(List.of(original), manifest.worldFiles());
        assertEquals(Map.of(player, playerdata), manifest.playerDataFiles());
        assertEquals(Set.of(region.toAbsolutePath().normalize().toString(),
                playerdata.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());

        assertThrows(UnsupportedOperationException.class,
                () -> manifest.worldFiles().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> manifest.playerDataFiles().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> manifest.requiredSourceIds().clear());
    }

    @Test void closureKeysAreSortedAcrossSourceKinds() throws Exception {
        Path last = directory.resolve("world/region/r.9.0.mca");
        Path first = directory.resolve("world/entities/r.-9.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000073");
        Path playerdata = directory.resolve("world/playerdata/" + player + ".dat");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile(
                        "world", last, WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile(
                        "world", first, WorldStorageDiscovery.Kind.ENTITY)),
                Map.of(player, playerdata));
        List<String> actual = new ArrayList<>(manifest.requiredSourceIds());
        List<String> expected = List.of(
                last.toAbsolutePath().normalize().toString(),
                first.toAbsolutePath().normalize().toString(),
                playerdata.toAbsolutePath().normalize().toString()
        ).stream().sorted().toList();

        assertEquals(expected, actual);
    }
}
