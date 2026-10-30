package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only inventory extraction from modern terrain chunk block_entities.
 * Positions and slots are physical identity keys, independent of ownership.
 * The caller must resolve BlockLocker (including linked chests), world scope,
 * memory/disk duplicates and all other asset sources before settlement commit.
 */
public final class StoredBlockEntityAssets {
    private StoredBlockEntityAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("block_entities") instanceof List<?> blockEntities))
            throw new IOException("Missing modern chunk block_entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> positions = new HashSet<>();
        for (Object block : blockEntities) {
            if (!(block instanceof Map<?,?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed block entity");
            String pos = coordinate(entity.get("x")) + "," + coordinate(entity.get("y")) +
                    "," + coordinate(entity.get("z"));
            if (!positions.add(pos))
                throw new IOException("Duplicate block entity at " + pos);

            Object rawInventory = entity.get("Items");
            if (rawInventory == null) continue;
            if (!(rawInventory instanceof List<?> inventory))
                throw new IOException("Malformed block entity Items at " + pos);

            Set<Integer> slots = new HashSet<>();
            for (Object rawStack : inventory) {
                if (!(rawStack instanceof Map<?, ?> stack))
                    throw new IOException("Malformed item in block entity " + pos);
                Object rawSlot = stack.get("Slot");
                if (!(rawSlot instanceof Number n) || n instanceof Float ||
                        n instanceof Double || n.longValue() < 0 || n.longValue() > 255)
                    throw new IOException("Invalid block entity slot at " + pos);
                int slot = n.intValue();
                if (!slots.add(slot))
                    throw new IOException("Duplicate block entity slot: " + pos + ":" + slot);
                if (stack.keySet().stream().anyMatch(k -> !(k instanceof String)))
                    throw new IOException("Malformed block entity item keys");

                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) stack;
                BigInteger amount = StoredItemValue.value(item);
                if (amount.signum() > 0) found.put(pos + ":" + slot, amount);
            }
        }
        return Map.copyOf(found);
    }

    private static int coordinate(Object raw) throws IOException {
        if (!(raw instanceof Integer value))
            throw new IOException("Missing or invalid block entity coordinates");
        return value;
    }
}
