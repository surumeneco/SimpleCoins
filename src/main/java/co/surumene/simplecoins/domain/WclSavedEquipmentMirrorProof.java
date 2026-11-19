package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * An exact item-level match between a selected SAVED Entity's vanilla
 * equipment and the single already-verified WCL StoredItem byte payload.
 *
 * Equality is structural NBT equality, not equal denomination, value, type,
 * UUID or display label. This does not check monetary ownership/amount or
 * source epoch: the accumulator separately requires the SAME native asset.
 */
public final class WclSavedEquipmentMirrorProof {
    private WclSavedEquipmentMirrorProof() {}

    public static void prove(Map<String, Object> savedChunk, UUID entityId,
                             String providerId, String providerSlot, byte[] storedItem,
                             int maxSerializedBytes, int maxInflatedBytes) throws IOException {
        proveWithVersion(savedChunk, entityId, providerId, providerSlot, storedItem,
                maxSerializedBytes, maxInflatedBytes, null);
    }

    /**
     * Paper 26.2 ItemStack.serializeAsBytes() can include root DataVersion,
     * whereas the same Entity's vanilla persisted equipment compound does not.
     * Ignore ONLY that root wrapper when it exactly matches the current Paper
     * server data version. Never strip unknown fields or rewrite item content.
     */
    public static void prove(Map<String, Object> savedChunk, UUID entityId,
                             String providerId, String providerSlot, byte[] storedItem,
                             int maxSerializedBytes, int maxInflatedBytes,
                             int expectedPaperDataVersion) throws IOException {
        if (expectedPaperDataVersion <= 0)
            throw new IOException("Invalid Paper data-version authority for WCL equipment");
        proveWithVersion(savedChunk, entityId, providerId, providerSlot, storedItem,
                maxSerializedBytes, maxInflatedBytes, expectedPaperDataVersion);
    }

    private static void proveWithVersion(Map<String, Object> savedChunk, UUID entityId,
                             String providerId, String providerSlot, byte[] storedItem,
                             int maxSerializedBytes, int maxInflatedBytes,
                             Integer expectedPaperDataVersion) throws IOException {
        Objects.requireNonNull(savedChunk, "SAVED Entity chunk");
        Objects.requireNonNull(entityId, "selected SAVED Entity UUID");
        Objects.requireNonNull(providerId, "verified WCL provider");
        Objects.requireNonNull(providerSlot, "verified WCL physical slot");
        if (maxSerializedBytes < 1 || maxInflatedBytes < 1
                || storedItem == null || storedItem.length < 2
                || storedItem.length > maxSerializedBytes)
            throw new IOException("Unusable WCL equipment serialized ItemStack budget");
        String vanillaSlot;
        String species;
        if ("www".equals(providerId) && "weapon".equals(providerSlot)) {
            vanillaSlot = "mainhand";
            species = "minecraft:wolf";
        } else if ("wwc".equals(providerId) && "head".equals(providerSlot)) {
            vanillaSlot = "head";
            species = "minecraft:chicken";
        } else {
            throw new IOException("WCL slot has no approved vanilla equipment mirror");
        }
        if (!(savedChunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Selected SAVED chunk lacks Entity NBT list");
        Map<?, ?> target = null;
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity))
                throw new IOException("Malformed SAVED Entity during WCL mirror proof");
            Object id = entity.get("UUID");
            if (!(id instanceof int[] parts) || parts.length != 4)
                continue; // other unobserved Entity; full traversal validates all
            UUID present = new UUID(
                    ((long)parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long)parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            if (present.equals(entityId)) {
                if (target != null)
                    throw new IOException("Ambiguous SAVED WCL Entity UUID in equipment mirror");
                target = entity;
            }
        }
        if (target == null || !species.equals(target.get("id")))
            throw new IOException("SAVED WCL Entity UUID/species mismatch for mirror");
        if (!(target.get("equipment") instanceof Map<?, ?> equipment)
                || !(equipment.get(vanillaSlot) instanceof Map<?, ?> vanillaItem))
            throw new IOException("SAVED WCL mirror has no independently stored vanilla equipment");

        final Map<String, Object> reported;
        InputStream raw = new ByteArrayInputStream(storedItem);
        try (var decoded = (storedItem[0] == (byte)0x1f && storedItem[1] == (byte)0x8b)
                ? new GZIPInputStream(raw) : raw) {
            reported = NbtReader.read(decoded, maxInflatedBytes);
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid WCL mirrored ItemStack NBT", invalid);
        }

        Map<String, Object> comparable = reported;
        if (reported.containsKey("DataVersion")) {
            Object version = reported.get("DataVersion");
            if (expectedPaperDataVersion == null
                    || !(version instanceof Integer serializedVersion)
                    || !serializedVersion.equals(expectedPaperDataVersion))
                throw new IOException("WCL ItemStack DataVersion differs from active Paper version");
            comparable = new java.util.LinkedHashMap<>(reported);
            comparable.remove("DataVersion");
        }
        if (!equalNbt(vanillaItem, comparable))
            throw new IOException("WCL ItemStack contents differ from SAVED vanilla equipment");
    }

    private static boolean equalNbt(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left instanceof Map<?,?> a && right instanceof Map<?,?> b) {
            if (!a.keySet().equals(b.keySet())) return false;
            for (var entry : a.entrySet())
                if (!equalNbt(entry.getValue(), b.get(entry.getKey()))) return false;
            return true;
        }
        if (left instanceof List<?> a && right instanceof List<?> b) {
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++)
                if (!equalNbt(a.get(i), b.get(i))) return false;
            return true;
        }
        if (left instanceof byte[] a && right instanceof byte[] b)
            return Arrays.equals(a, b);
        if (left instanceof int[] a && right instanceof int[] b)
            return Arrays.equals(a, b);
        if (left instanceof long[] a && right instanceof long[] b)
            return Arrays.equals(a, b);
        return left.getClass() == right.getClass() && left.equals(right);
    }
}
