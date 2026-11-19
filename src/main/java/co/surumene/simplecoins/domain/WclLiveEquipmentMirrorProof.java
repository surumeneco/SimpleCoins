package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Strict full-NBT, read-only equivalence of the originally captured native
 * equipment ItemStack, the current same-Entity equipment ItemStack, and the
 * one-read WCL provider ItemStack. No monetary amount is re-evaluated here.
 *
 * The Paper adapter must independently prove physical Entity/Chunk/slot
 * provenance and use the very same scanned native monetary Value. This
 * helper is not a writer lock, global snapshot or settlement authorization.
 */
public final class WclLiveEquipmentMirrorProof {
    private WclLiveEquipmentMirrorProof() {}

    public static void prove(byte[] originallyScannedNative, byte[] currentNative,
                             byte[] wclReadOnce, int maxSerializedBytes,
                             int maxInflatedBytes) throws IOException {
        var nativeAtScan = decoded(originallyScannedNative,
                maxSerializedBytes, maxInflatedBytes);
        var nativeNow = decoded(currentNative, maxSerializedBytes, maxInflatedBytes);
        var provider = decoded(wclReadOnce, maxSerializedBytes, maxInflatedBytes);
        if (!sameNbt(nativeAtScan, nativeNow)
                || !sameNbt(nativeAtScan, provider))
            throw new IOException("LIVE WCL equipment and captured/current native NBT differ");
    }

    private static Map<String,Object> decoded(byte[] bytes,
                                               int maxSerialized, int maxInflated)
            throws IOException {
        if (maxSerialized < 1 || maxInflated < 1 || bytes == null
                || bytes.length < 2 || bytes.length > maxSerialized)
            throw new IOException("Invalid or missing LIVE equipment ItemStack evidence");
        InputStream raw = new ByteArrayInputStream(bytes);
        try (var input = bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b
                ? new GZIPInputStream(raw) : raw) {
            return NbtReader.read(input, maxInflated);
        } catch (RuntimeException invalid) {
            throw new IOException("Malformed LIVE mirrored ItemStack NBT", invalid);
        }
    }

    private static boolean sameNbt(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof Map<?,?> left && b instanceof Map<?,?> right) {
            if (!left.keySet().equals(right.keySet())) return false;
            for (var entry : left.entrySet())
                if (!sameNbt(entry.getValue(), right.get(entry.getKey()))) return false;
            return true;
        }
        if (a instanceof List<?> left && b instanceof List<?> right) {
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++)
                if (!sameNbt(left.get(i), right.get(i))) return false;
            return true;
        }
        if (a instanceof byte[] left && b instanceof byte[] right)
            return Arrays.equals(left, right);
        if (a instanceof int[] left && b instanceof int[] right)
            return Arrays.equals(left, right);
        if (a instanceof long[] left && b instanceof long[] right)
            return Arrays.equals(left, right);
        return a.getClass() == b.getClass() && Objects.equals(a,b);
    }
}
