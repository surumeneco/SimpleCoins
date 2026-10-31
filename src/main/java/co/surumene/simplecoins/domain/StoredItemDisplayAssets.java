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
 * Read-only extraction of the displayed item in saved Java item_display
 * Entities. Empty air displays are not currency; all other malformed item
 * data aborts extraction rather than silently losing physical assets.
 */
public final class StoredItemDisplayAssets {
    private StoredItemDisplayAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!id.equals("minecraft:item_display")) continue;

            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Item display has no valid UUID");
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate item display UUID: " + key);

            if (!entity.containsKey("item")) continue;
            Object rawItem = entity.get("item");
            if (!(rawItem instanceof Map<?, ?> stack) ||
                    stack.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed item display stack: " + key);
            if ("minecraft:air".equals(stack.get("id"))) {
                Object rawCount = stack.get("count");
                if (!(rawCount instanceof Number count) ||
                        count instanceof Float || count instanceof Double || count.longValue() != 0)
                    throw new IOException("Malformed empty item display: " + key);
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) stack;
            BigInteger value = StoredItemValue.value(item);
            if (value.signum() > 0) found.put(key, value);
        }
        return Map.copyOf(found);
    }
}
