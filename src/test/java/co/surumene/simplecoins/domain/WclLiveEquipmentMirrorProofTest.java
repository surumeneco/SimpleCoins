package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WclLiveEquipmentMirrorProofTest {
    private static void field(DataOutputStream out, String name, Object value) throws IOException {
        if (value instanceof String text) {
            out.writeByte(8);
            out.writeUTF(name);
            out.writeUTF(text);
        } else if (value instanceof Integer integer) {
            out.writeByte(3);
            out.writeUTF(name);
            out.writeInt(integer);
        } else if (value instanceof Map<?, ?> nested) {
            out.writeByte(10);
            out.writeUTF(name);
            for (var entry : nested.entrySet())
                field(out, (String)entry.getKey(), entry.getValue());
            out.writeByte(0);
        } else {
            throw new IOException("Unsupported fixture tag: " + name);
        }
    }

    private static byte[] item(int count, int denomination, int version) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var nbt = new DataOutputStream(out)) {
            var data = Map.of(
                    "DataVersion", version,
                    "id", "minecraft:paper",
                    "count", count,
                    "components", Map.of(
                            "minecraft:item_model", "simplecoins:coin_" + denomination,
                            "minecraft:custom_data", Map.of("PublicBukkitValues",
                                    Map.of("simplecoins:coin_denomination", denomination))));
            field(nbt, "", data);
        }
        return out.toByteArray();
    }

    @Test void originalCurrentAndOneWclObservationSameFullNbtAreEquivalent() throws Exception {
        var original = item(1, 10000, 4903);
        WclLiveEquipmentMirrorProof.prove(original, original.clone(),
                original.clone(), 65536, 65536);
    }

    @Test void equalAmountCannotHideDifferentQuantityOrPdcOrDataVersion() throws Exception {
        var original = item(1, 10000, 4903);
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                original, original, item(10, 1000, 4903), 65536, 65536));
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                original, item(2, 10000, 4903), original, 65536, 65536));
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                original, original, item(1, 10000, 4904), 65536, 65536));
    }

    @Test void missingMalformedAndOversizedSourceEvidenceAlwaysReject() throws Exception {
        byte[] item = item(1, 10000, 4903);
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                null, item, item, 65536, 65536));
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                item, new byte[]{1, 2, 3}, item, 65536, 65536));
        assertThrows(IOException.class, () -> WclLiveEquipmentMirrorProof.prove(
                item, item, item, 10, 65536));
    }
}
