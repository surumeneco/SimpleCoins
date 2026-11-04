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

/** Candidate tests: excluded world filtering does not exclude playerdata authority. */
class WorldStorageExcludedWorldPlayerdataAliasTest {
    @TempDir Path temp;
    private static final String PLAYER = "00000000-0000-0000-0000-000000000001";

    private void linkOrSkip(Path alias, Path source) {
        try {
            Files.createLink(alias, source);
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable: " + unsupported);
        }
    }

    private WorldScope onlyIncludedWorld() {
        return new WorldScope(false, Set.of("included"), Set.of("excluded"));
    }

    @Test
    void excludedWorldPlayerdataCannotAliasIncludedRegion() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path region = Files.createDirectory(included.resolve("region"));
        Path players = Files.createDirectory(excluded.resolve("playerdata"));
        Path source = Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(players.resolve(PLAYER + ".dat"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("included", included);
        roots.put("excluded", excluded);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(roots, onlyIncludedWorld(), 20));
    }

    @Test
    void excludedWorldPlayerdataAliasIsRejectedWhenExcludedWorldIsVisitedFirst() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path region = Files.createDirectory(included.resolve("region"));
        Path players = Files.createDirectory(excluded.resolve("playerdata"));
        Path source = Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(players.resolve(PLAYER + ".dat"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("excluded", excluded);
        roots.put("included", included);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(roots, onlyIncludedWorld(), 20));
    }

    @Test
    void excludedWorldPlayerdataCannotAliasSameUuidInIncludedWorld() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path includedPlayers = Files.createDirectory(included.resolve("playerdata"));
        Path excludedPlayers = Files.createDirectory(excluded.resolve("playerdata"));
        Path source = Files.write(includedPlayers.resolve(PLAYER + ".dat"), new byte[] {1});
        linkOrSkip(excludedPlayers.resolve(PLAYER + ".dat"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("included", included);
        roots.put("excluded", excluded);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(roots, onlyIncludedWorld(), 20));
    }

    @Test
    void excludedWorldPlayerdataCannotAliasIncludedEntityRegion() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path entities = Files.createDirectory(included.resolve("entities"));
        Path players = Files.createDirectory(excluded.resolve("playerdata"));
        Path source = Files.write(entities.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(players.resolve(PLAYER + ".dat"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("included", included);
        roots.put("excluded", excluded);

        assertThrows(IOException.class,
                () -> WorldStorageDiscovery.discover(roots, onlyIncludedWorld(), 20));
    }

    @Test
    void excludedWorldRegionAliasIsNotAuthoritative() throws Exception {
        Path included = Files.createDirectory(temp.resolve("included"));
        Path excluded = Files.createDirectory(temp.resolve("excluded"));
        Path includedRegion = Files.createDirectory(included.resolve("region"));
        Path excludedRegion = Files.createDirectory(excluded.resolve("region"));
        Path source = Files.write(includedRegion.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(excludedRegion.resolve("r.1.0.mca"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("included", included);
        roots.put("excluded", excluded);

        var manifest = WorldStorageDiscovery.discover(roots, onlyIncludedWorld(), 20);
        assertEquals(1, manifest.worldFiles().size());
        assertEquals("included", manifest.worldFiles().get(0).world());
        assertEquals(Map.of(), manifest.playerDataFiles());
    }
}
