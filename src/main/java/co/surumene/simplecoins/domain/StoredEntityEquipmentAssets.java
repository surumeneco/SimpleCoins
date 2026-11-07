package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only 1.21.5+ saved Entity equipment extraction. Does not assign an
 * owner: WWC/WWW commander, tame owner, BAN and playerdata state must be
 * resolved by an authoritative adapter before staging any amounts.
 */
public final class StoredEntityEquipmentAssets {
    private static final Set<String> SLOTS = Set.of(
            "mainhand", "offhand", "head", "chest", "legs", "feet", "body", "saddle");

    private StoredEntityEquipmentAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!entity.containsKey("equipment")) continue;

            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Equipped Entity has no valid UUID");
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate equipped Entity UUID: " + key);

            if (!(entity.get("equipment") instanceof Map<?, ?> equipment))
                throw new IOException("Malformed Entity equipment: " + key);
            for (var entry : equipment.entrySet()) {
                if (!(entry.getKey() instanceof String slot) || !SLOTS.contains(slot))
                    throw new IOException("Unknown Entity equipment slot: " + entry.getKey());
                if (!(entry.getValue() instanceof Map<?, ?> stack) ||
                        stack.keySet().stream().anyMatch(k -> !(k instanceof String)))
                    throw new IOException("Malformed Entity equipment stack: " + key + ":" + slot);
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) stack;
                BigInteger value = StoredItemValue.value(item);
                if (value.signum() > 0) found.put(key + ":" + slot, value);
            }
        }
        return Map.copyOf(found);
    }
}
