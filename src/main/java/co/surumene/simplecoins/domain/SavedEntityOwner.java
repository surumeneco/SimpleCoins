package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saved vanilla tame-owner hints only. An Entity Owner tag is not generally
 * economic ownership: dropped items use it for delivery targets, projectiles
 * for their shooter. Caller must resolve registration, bans, WWC/WWW commander
 * precedence and loaded-Entity state before any asset classification.
 */
public final class SavedEntityOwner {
    private static final Set<String> TAMEABLES = Set.of(
            "minecraft:wolf", "minecraft:cat", "minecraft:parrot",
            "minecraft:horse", "minecraft:donkey", "minecraft:mule",
            "minecraft:llama", "minecraft:trader_llama",
            "minecraft:skeleton_horse", "minecraft:zombie_horse");

    private SavedEntityOwner() {}

    public static Map<String, UUID> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");
        Map<String, UUID> owners = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!TAMEABLES.contains(id)) continue;
            UUID uuid = uuid(entity.get("UUID"), "tameable Entity UUID");
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate tameable Entity UUID: " + key);
            if (!entity.containsKey("Owner")) continue;
            owners.put(key, uuid(entity.get("Owner"), "tameable Entity Owner"));
        }
        return Map.copyOf(owners);
    }

    private static UUID uuid(Object raw, String label) throws IOException {
        if (!(raw instanceof int[] parts) || parts.length != 4)
            throw new IOException("Invalid " + label);
        return new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
    }
}
