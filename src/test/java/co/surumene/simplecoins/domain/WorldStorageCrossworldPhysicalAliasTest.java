package co.surumene.simplecoins.domain;

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

/**
 * Candidate cross-world physical-identity characterization.
 * CAT normative Gates remain not-run; this is a regression test only.
 */
class WorldStorageCrossworldPhysicalAliasTest {
    @TempDir Path temp;

    private void linkOrSkip(Path alias, Path original) {
        try {
            Files.createLink(alias, original);
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            assumeTrue(false, "Hard links unavailable in test filesystem: " + unsupported);
        }
    }

    @Test
    void differentWorldsCannotCountSameRegionInodeTwice() throws Exception {
        Path a = Files.createDirectory(temp.resolve("world_a"));
        Path b = Files.createDirectory(temp.resolve("world_b"));
        Path regionA = Files.createDirectory(a.resolve("region"));
        Path regionB = Files.createDirectory(b.resolve("region"));
        Path source = Files.write(regionA.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(regionB.resolve("r.1.0.mca"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("a", a);
        roots.put("b", b);

        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(roots, new WorldScope(true, Set.of(), Set.of()), 20));
    }

    @Test
    void crossWorldRegionAndPlayerdataHardlinkCannotBothBeSources() throws Exception {
        Path a = Files.createDirectory(temp.resolve("world_a"));
        Path b = Files.createDirectory(temp.resolve("world_b"));
        Path region = Files.createDirectory(a.resolve("region"));
        Path playerdata = Files.createDirectory(b.resolve("playerdata"));
        Path source = Files.write(region.resolve("r.0.0.mca"), new byte[] {1});
        linkOrSkip(playerdata.resolve(UUID.fromString("00000000-0000-0000-0000-000000000001") + ".dat"), source);
        Map<String, Path> roots = new LinkedHashMap<>();
        roots.put("b", b);
        roots.put("a", a);

        assertThrows(IOException.class, () ->
                WorldStorageDiscovery.discover(roots, new WorldScope(true, Set.of(), Set.of()), 20));
    }
}
