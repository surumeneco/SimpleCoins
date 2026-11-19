package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, explicitly captured World-registry identity for one bounded
 * Paper saved-source discovery interval.
 *
 * Both maps must originate from full Bukkit.getWorlds() enumeration performed
 * by a caller on Paper's main thread. This class proves only equality of those
 * two observations and the saved Manifest provenance, not writer exclusion,
 * all offline Worlds, unchanged live values, or permission to publish/admit.
 */
public final class WorldSourceIdentitySnapshot {
    /** One observed Paper World, including its actual dimension folder. */
    public record Identity(UUID uuid, Path worldFolder) {
        public Identity {
            Objects.requireNonNull(uuid, "World UUID");
            Objects.requireNonNull(worldFolder, "Paper dimension folder");
            worldFolder = worldFolder.toAbsolutePath().normalize();
        }
    }

    private final Map<String, Identity> captured;

    private WorldSourceIdentitySnapshot(Map<String, Identity> validated) {
        captured = Map.copyOf(validated);
    }

    /** Require an exact non-aliased World name/UUID/directory universe. */
    public static WorldSourceIdentitySnapshot capture(
            Map<String, Identity> observed) throws IOException {
        if (observed == null || observed.isEmpty())
            throw new IOException("Paper registered World universe is empty");
        Map<String, Identity> checked = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        Set<Path> folders = new HashSet<>();
        for (var entry : observed.entrySet()) {
            String name = entry.getKey();
            Identity world = entry.getValue();
            if (name == null || name.isBlank() || world == null
                    || world.uuid() == null || world.worldFolder() == null
                    || world.uuid().equals(new UUID(0, 0)))
                throw new IOException("Invalid Paper World registered identity");
            Identity canonical = new Identity(world.uuid(), world.worldFolder());
            if (!ids.add(canonical.uuid()))
                throw new IOException("Duplicate Bukkit World UUID: " + canonical.uuid());
            if (!folders.add(canonical.worldFolder()))
                throw new IOException("Multiple Bukkit World identities share dimension directory: "
                        + canonical.worldFolder());
            checked.put(name, canonical);
        }
        return new WorldSourceIdentitySnapshot(checked);
    }

    /** Immutable source roots to pass to the SAME WorldStorageDiscovery run. */
    public Map<String, Path> worldFolders() {
        Map<String, Path> roots = new HashMap<>();
        captured.forEach((name, identity) -> roots.put(name, identity.worldFolder()));
        return Map.copyOf(roots);
    }

    /** Bukkit's observed UUIDs, which must be verified against real Paper metadata. */
    public Map<String, UUID> worldIds() {
        Map<String, UUID> ids = new HashMap<>();
        captured.forEach((name, identity) -> ids.put(name, identity.uuid()));
        return Map.copyOf(ids);
    }

    /** Saved discovery may neither omit, add nor remap one registered World. */
    public void requireDiscoveredManifest(
            WorldStorageDiscovery.Manifest manifest) throws IOException {
        if (manifest == null || manifest.origin() == null)
            throw new IOException("World identity requires actual saved discovery provenance");
        Map<String, Path> actual = new HashMap<>();
        for (var entry : manifest.origin().worldFolders().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                throw new IOException("Invalid discovered World root");
            actual.put(entry.getKey(), entry.getValue().toAbsolutePath().normalize());
        }
        if (!worldFolders().equals(actual))
            throw new IOException("Discovered saved World roots differ from registered Paper Worlds");
    }

    /**
     * Caller must obtain a FRESH complete main-thread Bukkit.getWorlds() set.
     * A changed name, UUID, physical folder or alias rejects the observation.
     */
    public void requireSameRegisteredWorlds(
            Map<String, Identity> current) throws IOException {
        WorldSourceIdentitySnapshot after = capture(current);
        if (!captured.equals(after.captured))
            throw new IOException("Paper registered World universe changed during discovery");
    }
}
