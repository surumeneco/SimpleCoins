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
 * Read-only extraction of the single displayed stack in saved Java item
 * frames and glow item frames. The caller must reconcile live entities,
 * other inventory-bearing entity types and snapshot generations separately.
 */
public final class StoredItemFrameAssets {
    private StoredItemFrameAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!id.equals("minecraft:item_frame") && !id.equals("minecraft:glow_item_frame"))
                continue;

            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Item frame has no valid UUID");
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate item frame UUID: " + key);

            if (!entity.containsKey("Item")) continue; // Empty frame.
            Object rawItem = entity.get("Item");
            if (!(rawItem instanceof Map<?, ?> item) ||
                    item.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed item frame stack: " + key);
            @SuppressWarnings("unchecked")
            Map<String, Object> stack = (Map<String, Object>) item;
            BigInteger value = StoredItemValue.value(stack);
            if (value.signum() > 0) found.put(key, value);
        }
        return Map.copyOf(found);
    }
}
