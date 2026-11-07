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
            // A block entity must be located inside its declaring terrain chunk.
            // Both coordinates must be present together when a chunk declares them.
            Object chunkX = chunk.get("xPos");
            Object chunkZ = chunk.get("zPos");
            if (chunkX != null || chunkZ != null) {
                if (!(chunkX instanceof Integer cx) || !(chunkZ instanceof Integer cz) ||
                        Math.floorDiv(coordinate(entity.get("x")), 16) != cx ||
                        Math.floorDiv(coordinate(entity.get("z")), 16) != cz)
                    throw new IOException("Block entity outside declaring terrain chunk: " + pos);
            }

            // Decorated pots store one stack in the lowercase "item" tag,
            // not the conventional "Items" list. Ignoring it would undercount
            // physical currency and incorrectly publish a partial settlement.
            if (entity.containsKey("item")) {
                if (!id.equals("minecraft:decorated_pot") || entity.containsKey("Items"))
                    throw new IOException("Unsupported or ambiguous single-item block storage at " + pos);
                Object rawItem = entity.get("item");
                if (!(rawItem instanceof Map<?,?> stack) ||
                        stack.keySet().stream().anyMatch(k -> !(k instanceof String)))
                    throw new IOException("Malformed decorated pot item at " + pos);
                @SuppressWarnings("unchecked")
                Map<String, Object> item = (Map<String, Object>) stack;
                BigInteger amount = StoredItemValue.value(item);
                if (amount.signum() > 0) found.put(pos + ":0", amount);
                continue;
            }
            if (id.equals("minecraft:decorated_pot") && entity.containsKey("Items"))
                throw new IOException("Unexpected decorated pot inventory format at " + pos);

            if (!entity.containsKey("Items")) continue;
            Object rawInventory = entity.get("Items");
            if (!(rawInventory instanceof List<?> inventory))
                throw new IOException("Malformed block entity Items at " + pos);

            Set<Integer> slots = new HashSet<>();
            for (Object rawStack : inventory) {
                if (!(rawStack instanceof Map<?, ?> stack))
                    throw new IOException("Malformed item in block entity " + pos);
                Object rawSlot = stack.get("Slot");
                if (!(rawSlot instanceof Number n) ||
                        !(n instanceof Byte || n instanceof Short ||
                          n instanceof Integer || n instanceof Long) ||
                        n.longValue() < 0 || n.longValue() > 255)
                    throw new IOException("Invalid block entity slot at " + pos);
                int slot = n.intValue();
                // Each chest half has 27 physical slots, even when linked.
                // An out-of-range slot is not an independent asset location.
                if ((id.equals("minecraft:chest") || id.equals("minecraft:trapped_chest")) &&
                        slot > 26)
                    throw new IOException("Invalid chest inventory slot: " + slot);
                if (id.equals("minecraft:hopper") && slot > 4)
                    throw new IOException("Invalid hopper inventory slot: " + slot);
                if (id.equals("minecraft:barrel") && slot > 26)
                    throw new IOException("Invalid barrel inventory slot: " + slot);
                if ((id.equals("minecraft:dispenser") || id.equals("minecraft:dropper")) && slot > 8)
                    throw new IOException("Invalid dispenser/dropper inventory slot: " + slot);
                if ((id.equals("minecraft:furnace") || id.equals("minecraft:blast_furnace") ||
                        id.equals("minecraft:smoker")) && slot > 2)
                    throw new IOException("Invalid furnace-family inventory slot: " + slot);
                if (id.equals("minecraft:brewing_stand") && slot > 4)
                    throw new IOException("Invalid brewing stand inventory slot: " + slot);
                if (id.equals("minecraft:crafter") && slot > 8)
                    throw new IOException("Invalid crafter inventory slot: " + slot);
                if ((id.equals("minecraft:shulker_box") ||
                        (id.startsWith("minecraft:") && id.endsWith("_shulker_box"))) && slot > 26)
                    throw new IOException("Invalid placed shulker box slot: " + slot);
                if (id.equals("minecraft:chiseled_bookshelf") && slot > 5)
                    throw new IOException("Invalid chiseled bookshelf inventory slot: " + slot);
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
