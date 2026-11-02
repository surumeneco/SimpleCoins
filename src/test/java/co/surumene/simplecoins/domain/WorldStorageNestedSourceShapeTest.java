package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Draft SC-NESTED-01..04: malformed nested authoritative containers. */
class WorldStorageNestedSourceShapeTest {
    @TempDir Path temp;

    private void mustReject(String container) throws Exception {
        Path root = Files.createDirectory(temp.resolve("world"));
        Path path = root.resolve(container);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "file in place of directory");
        assertThrows(IOException.class, () -> WorldStorageDiscovery.discover(
                Map.of("world", root), new WorldScope(true, Set.of(), Set.of()), 100),
                container);
    }

    @Test void entityContainerIsFile() throws Exception {
        mustReject("entities");
    }

    @Test void legacyRegionContainerIsFile() throws Exception {
        mustReject("DIM-1/region");
    }

    @Test void customEntityContainerIsFile() throws Exception {
        mustReject("dimensions/custom/moon/entities");
    }

    @Test void legacyPlayerdataContainerIsFile() throws Exception {
        mustReject("players/data");
    }
}
