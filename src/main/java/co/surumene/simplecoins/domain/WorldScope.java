package co.surumene.simplecoins.domain;

import java.util.Set;

/** A world in both explicit lists reverts to the configured default. */
public record WorldScope(boolean defaultIncluded, Set<String> included, Set<String> excluded) {
    public WorldScope {
        included = Set.copyOf(included);
        excluded = Set.copyOf(excluded);
    }

    /** Null world means world-independent player/ender-chest data: always count it. */
    public boolean includes(String world) {
        if (world == null) return true;
        boolean on = included.contains(world);
        boolean off = excluded.contains(world);
        if (on == off) return defaultIncluded;
        return on;
    }
}
