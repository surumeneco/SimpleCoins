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

    @Test void customNamespaceDimensionSourcesAreDiscoveredWithoutBackupAliases() throws Exception {
        Path root = directory.resolve("modded");
        Path region = root.resolve("dimensions/custom/moon/region/r.-2.3.mca");
        Path entities = root.resolve("dimensions/custom/moon/entities/r.-2.3.mca");
        Path backup = root.resolve("backups/dimensions/custom/moon/region/r.0.0.mca");
        for (Path file : new Path[]{region, entities, backup}) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }
        var manifest = WorldStorageDiscovery.discover(
                Map.of("modded", root), new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(Set.of(
                new WorldStorageDiscovery.SourceFile("modded", region.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("modded", entities.toAbsolutePath(),
                        WorldStorageDiscovery.Kind.ENTITY)), Set.copyOf(manifest.worldFiles()));
        assertTrue(manifest.playerDataFiles().isEmpty());

        // SC-COVERAGE-02: a directory at an authoritative custom source path
        // must fail closed instead of being counted as an empty region file.
        Files.createDirectories(root.resolve("dimensions/custom/moon/entities/r.0.0.mca"));
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("modded", root), new WorldScope(true, Set.of(), Set.of()), 100));
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

    @Test void excludedCustomDimensionMustNotSuppressIndependentPlayerdata() throws Exception {
        // SC-COVERAGE-04: WorldScope excludes world-owned terrain and Entity
        // sources, never the world-independent playerdata candidate.
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        Path includedRegion = included.resolve("region/r.0.0.mca");
        Path excludedRegion = excluded.resolve("dimensions/custom/moon/region/r.0.0.mca");
        Path excludedEntities = excluded.resolve("dimensions/custom/moon/entities/r.0.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000046");
        Path excludedPlayer = excluded.resolve("playerdata/" + player + ".dat");
        for (Path file : new Path[]{includedRegion, excludedRegion, excludedEntities, excludedPlayer}) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }

        var manifest = WorldStorageDiscovery.discover(
                Map.of("included", included, "excluded", excluded),
                new WorldScope(true, Set.of(), Set.of("excluded")), 100);
        assertEquals(List.of(new WorldStorageDiscovery.SourceFile(
                "included", includedRegion.toAbsolutePath(), WorldStorageDiscovery.Kind.REGION)),
                manifest.worldFiles());
        assertEquals(Map.of(player, excludedPlayer.toAbsolutePath()), manifest.playerDataFiles());
    }

    @Test void excludedWorldPlayerdataConflictStillFailsClosed() throws Exception {
        // SC-COVERAGE-03/04: an excluded world is not an authority override.
        // Identical UUIDs across world roots cannot be arbitrarily selected.
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000047");
        Path included = directory.resolve("included");
        Path excluded = directory.resolve("excluded");
        for (Path root : new Path[]{included, excluded}) {
            Path file = root.resolve("playerdata/" + player + ".dat");
            Files.createDirectories(file.getParent());
            Files.writeString(file, "fixture");
        }
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("included", included, "excluded", excluded),
                new WorldScope(true, Set.of(), Set.of("excluded")), 100));
    }

    @Test void excludedWorldNonRegularPlayerdataStillFailsClosed() throws Exception {
        // An invalid mandatory playerdata source is not equivalent to zero
        // assets merely because its containing world is excluded.
        Path excluded = directory.resolve("excluded");
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000048");
        Files.createDirectories(excluded.resolve("playerdata/" + player + ".dat"));
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded),
                new WorldScope(false, Set.of(), Set.of("excluded")), 100));
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


    @Test void orphanPlayerdataBackupCannotDisguiseMissingOwnerAsNoAssets() throws Exception {
        // Paper 26.2 writes UUID.dat_old when replacing UUID.dat. A backup
        // cannot be counted as current assets, but must not be ignored when
        // it is the only remaining evidence of a player's saved inventory.
        for (String directoryName : List.of("playerdata", "players/data")) {
            Path root = directory.resolve(directoryName.replace('/', '_'));
            UUID player = UUID.randomUUID();
            Path backup = root.resolve(directoryName).resolve(player + ".dat_old");
            Files.createDirectories(backup.getParent());
            Files.writeString(backup, "older playerdata NBT");
            assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                    Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100),
                    "Orphan backup is not an empty saved-playerdata source: " + backup);
        }
    }

    @Test void excludedWorldStillRejectsOrphanPlayerdataBackup() throws Exception {
        Path excluded = directory.resolve("excluded-backup");
        UUID player = UUID.randomUUID();
        Path backup = excluded.resolve("players/data/" + player + ".dat_old");
        Files.createDirectories(backup.getParent());
        Files.writeString(backup, "older playerdata NBT");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("excluded", excluded),
                new WorldScope(false, Set.of(), Set.of("excluded")), 100));
    }

    @Test void validPrimaryAndBackupUsePrimaryOnlyWithoutDoubleAccounting() throws Exception {
        Path root = directory.resolve("paired-playerdata");
        UUID player = UUID.randomUUID();
        Path primary = root.resolve("players/data/" + player + ".dat");
        Path backup = root.resolve("players/data/" + player + ".dat_old");
        Files.createDirectories(primary.getParent());
        Files.writeString(primary, "current playerdata");
        Files.writeString(backup, "previous playerdata");
        var manifest = WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100);
        assertEquals(Map.of(player, primary.toAbsolutePath()), manifest.playerDataFiles());
        assertEquals(Set.of(primary.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
        // A mid-audit loss of the authoritative primary cannot be substituted
        // by a backup or accepted as an unchanged source generation.
        Files.delete(primary);
        assertThrows(IOException.class, manifest::requireUnchangedDiscovery);
    }

    @Test void nonCanonicalAndNonRegularPlayerdataBackupsAreNotInvisible() throws Exception {
        Path root = directory.resolve("invalid-backup");
        Path playerRoot = root.resolve("playerdata");
        Files.createDirectories(playerRoot);
        Path invalid = playerRoot.resolve("1-1-1-1-1.dat_old");
        Files.writeString(invalid, "ambiguous UUID alias");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100));
        Files.delete(invalid);
        invalid = playerRoot.resolve("not-a-uuid.dat_old");
        Files.writeString(invalid, "bad UUID");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100));
        Files.delete(invalid);
        Path nonregular = playerRoot.resolve(UUID.randomUUID() + ".dat_old");
        Files.createDirectories(nonregular);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void noncanonicalRegionNamesCannotAliasTheSameChunkCoordinates() throws Exception {
        Path regionDir = directory.resolve("world/region");
        Files.createDirectories(regionDir);
        Files.writeString(regionDir.resolve("r.0.0.mca"), "fixture");
        // Integer.parseInt accepts both of these aliases for coordinate zero.
        // Scanning either alongside r.0.0.mca would count the same chunk twice.
        for (String alias : new String[]{"r.00.0.mca", "r.+0.0.mca", "r.-0.0.mca"}) {
            Path extra = regionDir.resolve(alias);
            Files.writeString(extra, "fixture");
            assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                    Map.of("world", directory.resolve("world")),
                    new WorldScope(true, Set.of(), Set.of()), 100), alias);
            Files.delete(extra);
        }
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

    @Test void hardLinkedRegionFilesMustNotBeCountedAsIndependentSources() throws Exception {
        Path regions = directory.resolve("world/region");
        Files.createDirectories(regions);
        Path original = regions.resolve("r.0.0.mca");
        Path alias = regions.resolve("r.1.0.mca");
        Files.write(original, new byte[8192]);
        Files.createLink(alias, original);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", directory.resolve("world")),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }


    @Test void noncanonicalPlayerdataUuidFilenameMustNotBeAcceptedAsAuthoritative() throws Exception {
        Path world = directory.resolve("world");
        Path playerdata = world.resolve("playerdata/1-1-1-1-1.dat");
        Files.createDirectories(playerdata.getParent());
        Files.writeString(playerdata, "fixture");
        // UUID.fromString accepts abbreviated groups and silently pads them.
        // The server's canonical playerdata path does not use that spelling.
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world),
                new WorldScope(true, Set.of(), Set.of()), 100));
    }

    @Test void manifestRequiredSourcesUseCanonicalPathsAndRejectDuplicates() throws Exception {
        Path region = directory.resolve("world/region/r.0.0.mca");
        UUID player = UUID.randomUUID();
        Path playerdata = directory.resolve("world/playerdata/" + player + ".dat");
        var source = new WorldStorageDiscovery.SourceFile(
                "world", region, WorldStorageDiscovery.Kind.REGION);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(source), Map.of(player, playerdata));
        assertEquals(Set.of(region.toAbsolutePath().normalize().toString(),
                        playerdata.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
        var duplicate = new WorldStorageDiscovery.Manifest(
                List.of(source), Map.of(player, region));
        assertThrows(IOException.class, duplicate::requiredSourceIds);

        // The independent manifest contract ends at required-source IDs.
        // SettlementAccumulator closure is exercised in its own dependent Work.
    }

    @Test void manualManifestMustRejectBlankWorldIdentity() {
        Path region = directory.resolve("region/r.0.0.mca");
        assertThrows(IllegalArgumentException.class, () ->
                new WorldStorageDiscovery.SourceFile("", region, WorldStorageDiscovery.Kind.REGION));
        assertThrows(IllegalArgumentException.class, () ->
                new WorldStorageDiscovery.SourceFile("  ", region, WorldStorageDiscovery.Kind.REGION));
        assertEquals("world", new WorldStorageDiscovery.SourceFile(
                "world", region, WorldStorageDiscovery.Kind.REGION).world());
    }
    @Test void crossCategoryHardlinkCannotBeTwoAuthoritativeSources() throws Exception {
        Path world = directory.resolve("cross-kind-world");
        Path region = world.resolve("region/r.0.0.mca");
        Path playerdata = world.resolve("playerdata/" +
                UUID.fromString("00000000-0000-0000-0000-000000000045") + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(playerdata.getParent());
        Files.writeString(region, "shared physical source");
        Files.createLink(playerdata, region);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", world), new WorldScope(true, Set.of(), Set.of()), 100));
    }


    @Test void paper26DimensionFoldersMustIncludeSharedPlayerdataOutsideWorldPaths() throws Exception {
        // Paper 26.2 exposes dimensions/minecraft/<dimension> as World#getWorldPath().
        // The authoritative UUID.dat is in the sibling level-wide players/data folder.
        Path levelRoot = directory.resolve("paper26");
        Path overworld = levelRoot.resolve("dimensions/minecraft/overworld");
        Path nether = levelRoot.resolve("dimensions/minecraft/the_nether");
        Path region = overworld.resolve("region/r.0.0.mca");
        UUID player = UUID.fromString("00000000-0000-0000-0000-00000000beef");
        Path playerData = levelRoot.resolve("players/data/" + player + ".dat");
        Files.createDirectories(region.getParent());
        Files.createDirectories(nether);
        Files.createDirectories(playerData.getParent());
        Files.writeString(region, "region");
        Files.writeString(playerData, "playerdata");

        var result = WorldStorageDiscovery.discover(
                Map.of("world", overworld, "world_nether", nether),
                new WorldScope(true, Set.of(), Set.of("world")), 200,
                Set.of(levelRoot));
        assertTrue(result.worldFiles().isEmpty(), "Excluded WorldScope applies to regions");
        assertEquals(Map.of(player, playerData.toAbsolutePath()), result.playerDataFiles(),
                "Excluded worlds cannot omit globally scoped playerdata");
        assertTrue(result.requiredSourceIds().contains(playerData.toAbsolutePath().toString()));
        result.requireUnchangedDiscovery();

        Files.delete(playerData);
        assertThrows(IOException.class, result::requireUnchangedDiscovery,
                "Saved-source closure must detect loss of the shared primary");
    }

    @Test void paper26SharedOrphanBackupMustFailClosed() throws Exception {
        Path levelRoot = directory.resolve("paper26-orphan");
        Path dimension = levelRoot.resolve("dimensions/minecraft/overworld");
        Path backup = levelRoot.resolve(
                "players/data/00000000-0000-0000-0000-00000000beef.dat_old");
        Files.createDirectories(dimension);
        Files.createDirectories(backup.getParent());
        Files.writeString(backup, "orphan");

        var error = assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", dimension),
                new WorldScope(true, Set.of(), Set.of()), 100, Set.of(levelRoot)));
        assertTrue(error.getMessage().contains("Orphan Paper playerdata backup"),
                "A missing shared playerdata primary cannot count as zero");
    }

    @Test void paper26SharedPlayerdataMustRejectAliasesAndSymlinks() throws Exception {
        Path levelRoot = directory.resolve("paper26-alias");
        Path dimension = levelRoot.resolve("dimensions/minecraft/overworld");
        Path players = levelRoot.resolve("players/data");
        Files.createDirectories(dimension);
        Files.createDirectories(players);
        UUID player = UUID.fromString("00000000-0000-0000-0000-00000000feed");
        Path primary = players.resolve(player + ".dat");
        Files.writeString(primary, "primary");
        Path alias = levelRoot.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(alias.getParent());
        Files.writeString(alias, "duplicate");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", dimension),
                new WorldScope(true, Set.of(), Set.of()), 100, Set.of(levelRoot)));
        Files.delete(alias);
        Files.createSymbolicLink(alias, primary);
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", dimension),
                new WorldScope(true, Set.of(), Set.of()), 100, Set.of(levelRoot)));
    }

}
