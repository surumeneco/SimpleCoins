package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SC-COPY-01..04: verify existing Manifest copy and read-only behavior (CAT Draft). */
class WorldStorageManifestCopyVerificationTest {
    @TempDir Path temp;
    private WorldStorageDiscovery.SourceFile source() {
        return new WorldStorageDiscovery.SourceFile("world",
                temp.resolve("region/r.0.0.mca"), WorldStorageDiscovery.Kind.REGION);
    }

    @Test void callerListMutationCannotChangeManifest() throws Exception {
        var src = source();
        var list = new ArrayList<>(List.of(src));
        var manifest = new WorldStorageDiscovery.Manifest(list, Map.of());
        list.clear();
        assertEquals(List.of(src), manifest.worldFiles());
        assertEquals(Set.of(src.path().toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
    }

    @Test void callerMapMutationCannotChangeManifest() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000081");
        Path file = temp.resolve("playerdata/" + id + ".dat");
        var map = new HashMap<>(Map.of(id, file));
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), map);
        map.clear();
        assertEquals(Map.of(id, file), manifest.playerDataFiles());
        assertEquals(Set.of(file.toAbsolutePath().normalize().toString()),
                manifest.requiredSourceIds());
    }

    @Test void manifestCollectionsRejectExternalMutation() {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(source()), Map.of());
        assertThrows(UnsupportedOperationException.class, () -> manifest.worldFiles().clear());
        assertThrows(UnsupportedOperationException.class, () -> manifest.playerDataFiles().put(
                UUID.randomUUID(), temp.resolve("unexpected.dat")));
    }

    @Test void requiredSourceClosureIsReadOnly() throws Exception {
        var manifest = new WorldStorageDiscovery.Manifest(List.of(source()), Map.of());
        var closure = manifest.requiredSourceIds();
        assertThrows(UnsupportedOperationException.class, closure::clear);
        assertEquals(closure, manifest.requiredSourceIds());
    }
}
