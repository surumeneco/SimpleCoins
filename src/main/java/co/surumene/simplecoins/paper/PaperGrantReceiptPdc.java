package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.GrantReceiptState;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Strict single-field PDC codec for the receipt and current partial issuance
 * cursor. The same Player.saveData generation must later persist both this
 * PDC field and the physical inventory. This class DOES NOT perform that save
 * or claim exactly-once delivery. Paper PDC calls belong on the main thread.
 */
public final class PaperGrantReceiptPdc {
    public static final NamespacedKey RECEIPT =
            new NamespacedKey("simplecoins", "grant_receipt_v1");
    private static final int MAGIC = 0x53435231; // SCR1
    private static final int BODY_BYTES = 44;
    private static final int SHA_BYTES = 32;
    private static final int RECORD_BYTES = BODY_BYTES + SHA_BYTES;

    private PaperGrantReceiptPdc() {}

    /** Only an ABSENT receipt key means no previous delivery. */
    public static GrantReceiptState read(PersistentDataContainer container) {
        Objects.requireNonNull(container, "player PDC");
        if (!container.has(RECEIPT)) return GrantReceiptState.empty();
        byte[] raw;
        try {
            raw = container.get(RECEIPT, PersistentDataType.BYTE_ARRAY);
        } catch (RuntimeException badType) {
            throw new IllegalStateException("Unreadable player grant receipt PDC", badType);
        }
        if (raw == null)
            throw new IllegalStateException("Grant receipt exists but has wrong PDC type");
        return decode(raw);
    }

    /** Replaces one PDC byte[] rather than writing several independently torn keys. */
    public static void write(PersistentDataContainer container, GrantReceiptState state) {
        Objects.requireNonNull(container, "player PDC");
        Objects.requireNonNull(state, "receipt");
        container.set(RECEIPT, PersistentDataType.BYTE_ARRAY, encode(state));
    }

    /** Exact playerdata BYTE_ARRAY envelope; callers must still verify against GrantLedger. */
    public static byte[] toSavedBytes(GrantReceiptState receipt) {
        return encode(Objects.requireNonNull(receipt, "receipt"));
    }

    /** Decode saved NBT PDC bytes without interpreting a missing field as empty. */
    public static GrantReceiptState fromSavedBytes(byte[] savedBytes) {
        return decode(Objects.requireNonNull(savedBytes, "saved receipt bytes"));
    }

    private static byte[] encode(GrantReceiptState state) {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_BYTES);
        buffer.putInt(MAGIC);
        buffer.putLong(state.completedSequence());
        buffer.putLong(state.activeSequence());
        UUID id = state.activeId();
        buffer.putLong(id == null ? 0 : id.getMostSignificantBits());
        buffer.putLong(id == null ? 0 : id.getLeastSignificantBits());
        buffer.putLong(state.partialValue());
        byte[] bytes = buffer.array();
        byte[] hash = sha256(Arrays.copyOf(bytes, BODY_BYTES));
        System.arraycopy(hash, 0, bytes, BODY_BYTES, SHA_BYTES);
        return bytes;
    }

    private static GrantReceiptState decode(byte[] raw) {
        if (raw.length != RECORD_BYTES)
            throw new IllegalStateException("Invalid player grant receipt length");
        byte[] observed = Arrays.copyOfRange(raw, BODY_BYTES, RECORD_BYTES);
        if (!MessageDigest.isEqual(observed, sha256(Arrays.copyOf(raw, BODY_BYTES))))
            throw new IllegalStateException("Player grant receipt checksum mismatch");
        ByteBuffer buffer = ByteBuffer.wrap(raw, 0, BODY_BYTES);
        if (buffer.getInt() != MAGIC)
            throw new IllegalStateException("Unknown grant receipt schema");
        long completed = buffer.getLong();
        long active = buffer.getLong();
        long most = buffer.getLong();
        long least = buffer.getLong();
        long partial = buffer.getLong();
        if (active == 0 && (most != 0 || least != 0))
            throw new IllegalStateException("Idle player grant receipt contains a UUID");
        UUID id = active == 0 ? null : new UUID(most, least);
        try {
            return new GrantReceiptState(completed, active, id, partial);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException("Invalid player grant receipt state", invalid);
        }
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
