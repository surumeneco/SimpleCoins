package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WclSavedEquipmentMirrorProofTest {
    private static final UUID ENTITY =
            UUID.fromString("00000000-0000-4000-8000-0000000001a5");
    private static final UUID OTHER =
            UUID.fromString("00000000-0000-4000-8000-0000000001b6");

    private static Map<String, Object> money(int denom, int count) {
        return Map.of("id", "minecraft:paper", "count", count,
                "components", Map.of("minecraft:item_model", "simplecoins:coin_" + denom,
                    "minecraft:custom_data", Map.of("PublicBukkitValues",
                            Map.of("simplecoins:coin_denomination", denom))));
    }

    private static int[] uuid(UUID u) {
        return new int[] {(int) (u.getMostSignificantBits() >>> 32),
                (int) u.getMostSignificantBits(),
                (int) (u.getLeastSignificantBits() >>> 32),
                (int) u.getLeastSignificantBits()};
    }

    private static Map<String, Object> wolf(UUID id, Map<String,Object> stack) {
        return Map.of("id", "minecraft:wolf", "UUID", uuid(id),
                "equipment", Map.of("mainhand", stack));
    }

    private static Map<String, Object> chicken(UUID id, Map<String,Object> stack) {
        return Map.of("id", "minecraft:chicken", "UUID", uuid(id),
                "equipment", Map.of("head", stack));
    }

    private static void tag(DataOutputStream out, String name, Object value) throws IOException {
        if (value instanceof String x) {
            out.writeByte(8); out.writeUTF(name); out.writeUTF(x);
        } else if (value instanceof Integer x) {
            out.writeByte(3); out.writeUTF(name); out.writeInt(x);
        } else if (value instanceof Map<?,?> map) {
            out.writeByte(10); out.writeUTF(name);
            for (var entry : map.entrySet())
                tag(out, (String)entry.getKey(), entry.getValue());
            out.writeByte(0);
        } else {
            throw new IOException("Unsupported test NBT tag");
        }
    }

    private static byte[] serialized(Map<String,Object> item) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            tag(out, "", item);
        }
        return bytes.toByteArray();
    }

    @Test void exactWwwWeaponAndWwcHeadMatchSavedEquipmentContent() throws Exception {
        var item = money(10000, 1);
        var wolfChunk = Map.<String,Object>of("Entities", List.of(wolf(ENTITY, item)));
        var chickenChunk = Map.<String,Object>of("Entities", List.of(chicken(ENTITY, item)));
        WclSavedEquipmentMirrorProof.prove(wolfChunk, ENTITY, "www", "weapon",
                serialized(item), 4096, 4096);
        WclSavedEquipmentMirrorProof.prove(chickenChunk, ENTITY, "wwc", "head",
                serialized(item), 4096, 4096);
    }

    @Test void equalFaceValueOrMaterialWithoutFullItemEqualityIsNotProof() throws Exception {
        var item = money(10000, 1);
        var chunk = Map.<String,Object>of("Entities", List.of(wolf(ENTITY, item)));
        var wrong = money(1000, 10);
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", serialized(wrong), 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", serialized(money(10000, 2)), 4096, 4096));
    }

    @Test void differentEntitySlotSpeciesAndMissingGearFailClosed() throws Exception {
        var item = money(10000, 1);
        var chunk = Map.<String,Object>of("Entities", List.of(wolf(ENTITY, item)));
        byte[] bytes = serialized(item);
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, OTHER, "www", "weapon", bytes, 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "wwc", "head", bytes, 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                Map.of("Entities", List.of(Map.of("id", "minecraft:wolf", "UUID", uuid(ENTITY)))),
                ENTITY, "www", "weapon", bytes, 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "cargo:7", bytes, 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                Map.of("Entities", List.of(wolf(ENTITY, item), wolf(ENTITY, item))),
                ENTITY, "www", "weapon", bytes, 4096, 4096));
    }

    @Test void corruptOrOversizedSerializedWclItemIsNotProof() throws Exception {
        var item = money(10000, 1);
        var chunk = Map.<String,Object>of("Entities", List.of(wolf(ENTITY, item)));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", new byte[]{1,2,3}, 4096, 4096));
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", serialized(item), 8, 4096));
    }

    @Test void paperRootDataVersionIsAllowedOnlyWithMatchingRuntimeVersion() throws Exception {
        var item = money(10000, 1);
        var wclEncoding = new java.util.HashMap<String,Object>(item);
        wclEncoding.put("DataVersion", 4903);
        var chunk = Map.<String,Object>of("Entities", List.of(wolf(ENTITY, item)));
        byte[] wrapped = serialized(wclEncoding);
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", wrapped, 4096, 4096));
        WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", wrapped, 4096, 4096, 4903);
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", wrapped, 4096, 4096, 4904));
        wclEncoding.put("unrelated", 1);
        assertThrows(IOException.class, () -> WclSavedEquipmentMirrorProof.prove(
                chunk, ENTITY, "www", "weapon", serialized(wclEncoding), 4096, 4096, 4903));
    }
}
