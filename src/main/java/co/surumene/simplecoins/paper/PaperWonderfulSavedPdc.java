package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Read-only restoration of the PDC fields consumed by the pinned WWC/WWW
 * WonderfulStorageReader from one separately verified saved Entity NBT.
 *
 * This never loads a chunk or writes the original Entity/world/source file.
 * Caller still owns complete saved-source discovery, content witnesses,
 * LIVE/SAVED authority, generation closure and external BAN/ownership proof.
 */
public final class PaperWonderfulSavedPdc {
    private static final String CHICKEN = "whatawonderfulchicken";
    private static final String WOLF = "whatawonderfulwolf";

    private PaperWonderfulSavedPdc() {}

    public static PersistentDataContainer reconstruct(
            UUID expectedEntityId, String providerId, Map<String, Object> savedEntity,
            int maximumSerializedBytes) throws IOException {
        Objects.requireNonNull(expectedEntityId, "expected source Entity UUID");
        Objects.requireNonNull(providerId, "expected WCL provider");
        Objects.requireNonNull(savedEntity, "saved Entity NBT");
        if (maximumSerializedBytes < 1)
            throw new IllegalArgumentException("Positive saved PDC byte budget required");
        if (!Bukkit.isPrimaryThread())
            throw new IOException("Saved WCL PDC restoration requires Paper main thread");

        String type = switch (providerId) {
            case "wwc" -> "minecraft:chicken";
            case "www" -> "minecraft:wolf";
            default -> throw new IOException("Unsupported saved WCL provider: " + providerId);
        };
        if (!type.equals(savedEntity.get("id")))
            throw new IOException("Saved WCL Entity species/provider mismatch");
        if (!(savedEntity.get("UUID") instanceof int[] uuidParts) || uuidParts.length != 4)
            throw new IOException("Missing canonical saved WCL Entity UUID");
        UUID actual = new UUID(((long) uuidParts[0] << 32)
                | Integer.toUnsignedLong(uuidParts[1]),
                ((long) uuidParts[2] << 32)
                | Integer.toUnsignedLong(uuidParts[3]));
        if (!expectedEntityId.equals(actual))
            throw new IOException("Saved WCL Entity UUID disagrees with selected source");
        if (!(savedEntity.get("BukkitValues") instanceof Map<?, ?> raw))
            throw new IOException("Missing or malformed saved Entity BukkitValues");
        for (Object key : raw.keySet())
            if (!(key instanceof String))
                throw new IOException("Non-string saved Entity BukkitValues key");

        ItemMeta meta = new ItemStack(Material.PAPER).getItemMeta();
        if (meta == null)
            throw new IOException("Detached Paper ItemMeta unavailable for saved PDC");
        PersistentDataContainer target = meta.getPersistentDataContainer();
        if ("wwc".equals(providerId)) {
            copyString(raw, target, CHICKEN, "type", true, false);
            if (!"wonderful_chicken".equals(target.get(
                    new NamespacedKey(CHICKEN, "type"), PersistentDataType.STRING)))
                throw new IOException("Saved Entity is not a verified wonderful chicken");
            Integer version = copyInt(raw, target, CHICKEN, "data_version", false);
            if (version != null && version != 2 && version != 3 && version != 4)
                throw new IOException("Unsupported saved WWC PDC version: " + version);
            copyString(raw, target, CHICKEN, "follow_target", false, true);
            String genome = copyString(raw, target, CHICKEN, "genome_ref",
                    version != null && version == 4, true);
            if (version != null && version == 4 && genome == null)
                throw new IOException("WWC v4 saved PDC lacks required genome reference");
            copyBytes(raw, target, CHICKEN, "equipment_carpet", false, maximumSerializedBytes);
            copyBytes(raw, target, CHICKEN, "equipment_shulker_box", false, maximumSerializedBytes);
            copyBytes(raw, target, CHICKEN, "equipment_head", false, maximumSerializedBytes);
        } else {
            copyString(raw, target, WOLF, "type", true, false);
            if (!"wonderful_wolf".equals(target.get(
                    new NamespacedKey(WOLF, "type"), PersistentDataType.STRING)))
                throw new IOException("Saved Entity is not a verified wonderful wolf");
            Integer version = copyInt(raw, target, WOLF, "schema_version", true);
            if (version == null || version != 1 && version != 2)
                throw new IOException("Unsupported saved WWW PDC schema");
            copyString(raw, target, WOLF, "genome_ref", version == 2, true);
            copyBytes(raw, target, WOLF, "runtime_state", true, maximumSerializedBytes);
        }
        return target;
    }

    private static String copyString(Map<?, ?> raw, PersistentDataContainer target,
                                     String namespace, String name, boolean required,
                                     boolean canonicalUuid) throws IOException {
        String key = namespace + ":" + name;
        Object value = raw.get(key);
        if (!raw.containsKey(key)) {
            if (required) throw new IOException("Missing saved WCL string: " + key);
            return null;
        }
        if (!(value instanceof String string) || string.isBlank())
            throw new IOException("Malformed saved WCL string: " + key);
        if (canonicalUuid) {
            try {
                if (!UUID.fromString(string).toString().equals(string))
                    throw new IOException("Noncanonical saved WCL UUID: " + key);
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid saved WCL UUID: " + key, invalid);
            }
        }
        target.set(new NamespacedKey(namespace, name), PersistentDataType.STRING, string);
        return string;
    }

    private static Integer copyInt(Map<?, ?> raw, PersistentDataContainer target,
                                   String namespace, String name, boolean required)
            throws IOException {
        String key = namespace + ":" + name;
        if (!raw.containsKey(key)) {
            if (required) throw new IOException("Missing saved WCL version: " + key);
            return null;
        }
        if (!(raw.get(key) instanceof Integer value))
            throw new IOException("Malformed saved WCL integer: " + key);
        target.set(new NamespacedKey(namespace, name), PersistentDataType.INTEGER, value);
        return value;
    }

    private static void copyBytes(Map<?, ?> raw, PersistentDataContainer target,
                                  String namespace, String name, boolean required,
                                  int maximumSerializedBytes) throws IOException {
        String key = namespace + ":" + name;
        if (!raw.containsKey(key)) {
            if (required) throw new IOException("Missing saved WCL bytes: " + key);
            return;
        }
        if (!(raw.get(key) instanceof byte[] bytes)
                || bytes.length == 0 || bytes.length > maximumSerializedBytes)
            throw new IOException("Malformed/oversized saved WCL bytes: " + key);
        target.set(new NamespacedKey(namespace, name), PersistentDataType.BYTE_ARRAY,
                bytes.clone());
    }
}
