package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSourceIdentitySnapshotTest {
    @TempDir Path root;
    private static final UUID A = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-4000-8000-0000000000b2");

    private Path world(String name) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir);
        return dir;
    }

    private static WorldSourceIdentitySnapshot.Identity identity(UUID uuid, Path dir) {
        return new WorldSourceIdentitySnapshot.Identity(uuid, dir);
    }

    @Test void exactCapturedBukkitUniverseMustEqualDiscoveredsavedWorldRoots()
            throws Exception {
        Path first = world("first"), second = world("second");
        var initial = WorldSourceIdentitySnapshot.capture(Map.of(
                "first", identity(A, first), "second", identity(B, second)));
        assertEquals(Map.of("first", A, "second", B), initial.worldIds());
        assertEquals(Map.of("first", first, "second", second), initial.worldFolders());
        var real = WorldStorageDiscovery.discover(initial.worldFolders(),
                new WorldScope(true, Set.of(), Set.of()), 1024);
        initial.requireDiscoveredManifest(real);
        initial.requireSameRegisteredWorlds(Map.of(
                "first", identity(A, first), "second", identity(B, second)));
        // The original names/IDs must remain independent of caller mutation.
        var claimed = new java.util.HashMap<>(initial.worldIds());
        claimed.remove("second");
        assertEquals(2, initial.worldIds().size());
    }

    @Test void rejectedAddedRemovedRenamedWorldAndUuidOrPathSwap() throws Exception {
        Path first = world("first"), second = world("second"), third = world("third");
        var initial = WorldSourceIdentitySnapshot.capture(Map.of(
                "first", identity(A, first), "second", identity(B, second)));
        assertThrows(IOException.class, () -> initial.requireSameRegisteredWorlds(
                Map.of("first", identity(A, first))));
        assertThrows(IOException.class, () -> initial.requireSameRegisteredWorlds(
                Map.of("first", identity(A, first), "second", identity(B, second),
                        "extra", identity(UUID.randomUUID(), third))));
        assertThrows(IOException.class, () -> initial.requireSameRegisteredWorlds(
                Map.of("renamed", identity(A, first), "second", identity(B, second))));
        assertThrows(IOException.class, () -> initial.requireSameRegisteredWorlds(
                Map.of("first", identity(B, first), "second", identity(A, second))));
        assertThrows(IOException.class, () -> initial.requireSameRegisteredWorlds(
                Map.of("first", identity(A, third), "second", identity(B, second))));
    }

    @Test void noOriginAndManifestMissingOrExtraRootCannotBeAccepted() throws Exception {
        Path first = world("first"), second = world("second");
        var captured = WorldSourceIdentitySnapshot.capture(
                Map.of("first", identity(A, first), "second", identity(B, second)));
        assertThrows(IOException.class, () -> captured.requireDiscoveredManifest(
                new WorldStorageDiscovery.Manifest(java.util.List.of(), Map.of())));
        var onlyFirst = WorldStorageDiscovery.discover(Map.of("first", first),
                new WorldScope(true, Set.of(), Set.of()), 1024);
        assertThrows(IOException.class, () -> captured.requireDiscoveredManifest(onlyFirst));
        var shiftedPath = WorldStorageDiscovery.discover(
                Map.of("first", first, "second", world("different")),
                new WorldScope(true, Set.of(), Set.of()), 1024);
        assertThrows(IOException.class, () -> captured.requireDiscoveredManifest(shiftedPath));
    }

    @Test void duplicateBukkitUuidOrPathAndEmptyWorldSetAreRejected() throws Exception {
        Path first = world("first"), second = world("second");
        assertThrows(IOException.class, () -> WorldSourceIdentitySnapshot.capture(Map.of()));
        assertThrows(IOException.class, () -> WorldSourceIdentitySnapshot.capture(Map.of(
                "first", identity(A, first), "second", identity(A, second))));
        assertThrows(IOException.class, () -> WorldSourceIdentitySnapshot.capture(Map.of(
                "first", identity(A, first), "second", identity(B, first))));
        assertThrows(IOException.class, () -> WorldSourceIdentitySnapshot.capture(Map.of(
                " ", identity(A, first))));
    }
}
