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
        extractEquipment(root, found, visitedSlots);
        return Map.copyOf(found);
    }


    /**
     * Minecraft 1.21.5+ saves armor and off-hand contents in equipment.
     * Old Inventory slot numbers must not be counted again if also present.
     */
    private static void extractEquipment(Map<String,Object> root,
                                         Map<String,BigInteger> found,
                                         Set<String> visitedSlots) throws IOException {
        Object raw = root.get("equipment");
        if (raw == null) return;
        if (!(raw instanceof Map<?,?> equipment))
            throw new IOException("Malformed saved player equipment");
        Map<String,Integer> legacySlots = Map.of(
                "head", 103, "chest", 102, "legs", 101, "feet", 100,
                "offhand", -106);
        Set<String> permitted = Set.of("head", "chest", "legs", "feet",
                "offhand", "mainhand", "body", "saddle");
        for (var entry : equipment.entrySet()) {
            if (!(entry.getKey() instanceof String slot) || !permitted.contains(slot))
                throw new IOException("Unknown saved equipment slot: " + entry.getKey());
            if (!(entry.getValue() instanceof Map<?,?> stack) ||
                    stack.keySet().stream().anyMatch(name -> !(name instanceof String)))
                throw new IOException("Invalid saved equipment item: " + slot);
            // The selected hotbar slot and equipment.mainhand can be two
            // representations of the same physical stack in saved playerdata.
            if ("mainhand".equals(slot) && root.containsKey("SelectedItemSlot")) {
                Object rawSelected = root.get("SelectedItemSlot");
                if (!(rawSelected instanceof Number selected) ||
                        selected instanceof Float || selected instanceof Double ||
                        selected.longValue() < 0 || selected.longValue() > 8)
                    throw new IOException("Invalid selected hotbar slot in playerdata");
                if (visitedSlots.contains("inventory:" + selected.intValue()))
                    throw new IOException("Duplicate selected inventory/equipment mainhand slot");
            }
            Integer legacy = legacySlots.get(slot);
            if (legacy != null && visitedSlots.contains("inventory:" + legacy))
                throw new IOException("Duplicate legacy inventory/equipment slot: " + slot);
            String physical = "equipment:" + slot;
            if (!visitedSlots.add(physical))
                throw new IOException("Duplicate equipment slot: " + slot);
            @SuppressWarnings("unchecked")
            Map<String,Object> item = (Map<String,Object>) stack;
            BigInteger value = StoredItemValue.value(item);
            if (value.signum() > 0) found.put(physical, value);
        }
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

            // Ender chests have exactly 27 slots (0..26). Unlike the
            // player inventory, negative legacy equipment slots are invalid.
            if ("ender".equals(prefix) && (slot.longValue() < 0 || slot.longValue() > 26))
                throw new IOException("Invalid Ender Chest slot: " + slot.longValue());
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
