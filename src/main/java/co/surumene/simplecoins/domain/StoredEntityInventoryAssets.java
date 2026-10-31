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
 * Read-only saved Entity inventory extraction for container vehicles and
 * other Entities with an Items list. No ownership is inferred: authoritative
 * owner, loaded-Entity and world snapshot reconciliation remain mandatory.
 */
public final class StoredEntityInventoryAssets {
    private StoredEntityInventoryAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object rawEntity : entities) {
            if (!(rawEntity instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            boolean slotted = entity.containsKey("Items");
            boolean unslotted = entity.containsKey("Inventory");
            if (!slotted && !unslotted) continue;
            if (slotted && unslotted)
                throw new IOException("Ambiguous Entity inventory representations");

            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Inventory Entity has no valid UUID");
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate inventory Entity UUID: " + key);

            Object rawInventory = entity.get(slotted ? "Items" : "Inventory");
            if (!(rawInventory instanceof List<?> inventory))
                throw new IOException("Malformed Entity inventory list: " + key);
            Set<Integer> occupiedSlots = new HashSet<>();
            int position = 0;
            for (Object rawStack : inventory) {
                if (!(rawStack instanceof Map<?, ?> stack) ||
                        stack.keySet().stream().anyMatch(k -> !(k instanceof String)))
                    throw new IOException("Malformed Entity inventory stack: " + key);
                int index;
                if (slotted) {
                    Object rawSlot = stack.get("Slot");
                    if (!(rawSlot instanceof Number slot) ||
                            slot instanceof Float || slot instanceof Double ||
                            slot.longValue() < 0 || slot.longValue() > 255)
                        throw new IOException("Invalid Entity inventory slot: " + key);
                    index = slot.intValue();
                } else {
                    // Villager/Allay-style Inventory stores stacks in list order
                    // without Slot tags. This is a snapshot-local physical index.
                    if (stack.containsKey("Slot"))
                        throw new IOException("Unexpected Slot in unslotted Entity inventory: " + key);
                    index = position;
                }
                position++;
                if (!occupiedSlots.add(index))
                    throw new IOException("Duplicate Entity inventory slot: " + key + ":" + index);
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) stack;
                BigInteger value = StoredItemValue.value(item);
                if (value.signum() > 0) found.put(key + ":inventory:" + index, value);
            }
        }
        return Map.copyOf(found);
    }
}
