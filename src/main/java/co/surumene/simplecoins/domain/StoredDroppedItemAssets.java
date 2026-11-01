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
 * Read-only extraction of dropped-item Entities from a saved entity chunk.
 * UUIDs are physical identity keys; the caller must still reconcile loaded
 * entities, other inventory-bearing mobs, and the snapshot generation before
 * treating any settlement as complete.
 */
public final class StoredDroppedItemAssets {
    private StoredDroppedItemAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!id.equals("minecraft:item")) continue;

            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Dropped item has no valid UUID");
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate dropped item UUID: " + key);

            Object rawItem = entity.get("Item");
            if (!(rawItem instanceof Map<?, ?> item) ||
                    item.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed dropped item stack: " + key);
            @SuppressWarnings("unchecked")
            Map<String, Object> stack = (Map<String, Object>) item;
            BigInteger value = StoredItemValue.value(stack);
            if (value.signum() > 0) found.put(key, value);
        }
        return Map.copyOf(found);
    }
}
