package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Draft characterization of included entity files and excluded-world playerdata. */
class WorldStorageExcludedWorldEntityAliasTest {
    @TempDir Path temp;
    private static final String PLAYER = "00000000-0000-0000-0000-000000000001";

    private static void linkOrSkip(Path alias, Path original) {
        try {
            Files.createLink(alias, original);
        } catch (IOException | UnsupportedOperationException | SecurityException failure) {
            assumeTrue(false, "Hard links unavailable: " + failure);
        }
    }

    private static WorldScope includedOnly() {
        return new WorldScope(false, Set.of("included"), Set.of("excluded"));
    }

    private Map<String, Path> roots(boolean excludedFirst, Path included, Path excluded) {
        Map<String, Path> roots = new LinkedHashMap<>();
        if (excludedFirst) roots.put("excluded", excluded);
        roots.put("included", included);
        if (!excludedFirst) roots.put("excluded", excluded);
        return roots;
    }

    private void assertEntityPlayerdataCollision(boolean excludedFirst) throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path entities = Files.createDirectory(included.resolve("entities"));
        Path players = Files.createDirectory(excluded.resolve("playerdata"));
        Path source = Files.write(entities.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(players.resolve(PLAYER + ".dat"), source);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(
                        roots(excludedFirst, included, excluded), includedOnly(), 20));
    }

    @Test
    void includedEntityAndExcludedPlayerdataAliasFailsWhenIncludedFirst() throws Exception {
        assertEntityPlayerdataCollision(false);
    }

    @Test
    void includedEntityAndExcludedPlayerdataAliasFailsWhenExcludedFirst() throws Exception {
        assertEntityPlayerdataCollision(true);
    }

    @Test
    void excludedEntityAliasDoesNotInvalidateIncludedPlayerdata() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path players = Files.createDirectory(included.resolve("playerdata"));
        Path entities = Files.createDirectory(excluded.resolve("entities"));
        Path source = Files.write(players.resolve(PLAYER + ".dat"), new byte[] {1});
        linkOrSkip(entities.resolve("r.0.0.mca"), source);

        var manifest = WorldStorageDiscovery.discover(
                roots(false, included, excluded), includedOnly(), 20);
        assertEquals(0, manifest.worldFiles().size());
        assertEquals(Map.of(UUID.fromString(PLAYER), source), manifest.playerDataFiles());
    }

    @Test
    void includedOverworldRegionAndNetherEntityPhysicalAliasFailsClosed() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path overworldRegion = Files.createDirectory(included.resolve("region"));
        Path netherEntities = Files.createDirectories(included.resolve("DIM-1/entities"));
        Path source = Files.write(overworldRegion.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(netherEntities.resolve("r.1.0.mca"), source);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(
                        Map.of("included", included),
                        new WorldScope(true, Set.of(), Set.of()), 20));
    }

}
