package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only asset slot extraction from one complete saved playerdata NBT root.
 * Each physical slot is counted at most once. A missing inventory is a source
 * failure, not proof of an empty balance. World-independent data is always
 * eligible, regardless of the selected world accounting scope.
 */
public final class SavedPlayerAssets {
    private SavedPlayerAssets() {}

    public static Map<String, BigInteger> extract(Map<String, Object> root) throws IOException {
        Objects.requireNonNull(root);
        Map<String, BigInteger> found = new LinkedHashMap<>();
        Set<String> visitedSlots = new HashSet<>();
        extractInventory(root, "Inventory", "inventory", found, visitedSlots);
        extractInventory(root, "EnderItems", "ender", found, visitedSlots);
        return Map.copyOf(found);
    }

    private static void extractInventory(Map<String, Object> root, String key,
                                         String prefix, Map<String, BigInteger> found,
                                         Set<String> visitedSlots) throws IOException {
        if (!(root.get(key) instanceof List<?> stacks))
            throw new IOException("Missing or malformed playerdata inventory: " + key);

        for (Object rawStack : stacks) {
            if (!(rawStack instanceof Map<?, ?> item))
                throw new IOException("Malformed playerdata item in " + key);
            Object rawSlot = item.get("Slot");
            if (!(rawSlot instanceof Number slot) ||
                    slot instanceof Float || slot instanceof Double ||
                    slot.longValue() < -128 || slot.longValue() > 255)
                throw new IOException("Missing or invalid playerdata physical slot in " + key);

            String physical = prefix + ":" + slot.intValue();
            if (!visitedSlots.add(physical))
                throw new IOException("Duplicate playerdata physical slot: " + physical);
            if (item.keySet().stream().anyMatch(name -> !(name instanceof String)))
                throw new IOException("Malformed playerdata item keys");

            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) item;
            BigInteger amount = StoredItemValue.value(typed);
            if (amount.signum() > 0) found.put(physical, amount);
        }
    }
}
