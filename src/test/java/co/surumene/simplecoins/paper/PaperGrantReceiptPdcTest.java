package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.GrantReceiptState;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

class PaperGrantReceiptPdcTest {
    private final Map<NamespacedKey, Object> data = new HashMap<>();
    private final PersistentDataContainer container = (PersistentDataContainer) Proxy.newProxyInstance(
            PersistentDataContainer.class.getClassLoader(),
            new Class<?>[]{PersistentDataContainer.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "has": return data.containsKey(args[0]);
                    case "get": {
                        Object value = data.get(args[0]);
                        return value instanceof byte[] bytes ? bytes.clone() : null;
                    }
                    case "set": data.put((NamespacedKey) args[0],
                            ((byte[]) args[2]).clone()); return null;
                    case "remove": data.remove(args[0]); return null;
                    case "getKeys": return data.keySet();
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });

    @Test void absentPdcIsEmptyButOneFieldPreservesReceiptAndPartialGrantIdentity() {
        assertEquals(GrantReceiptState.empty(), PaperGrantReceiptPdc.read(container));
        var partial = new GrantReceiptState(3, 8, UUID.randomUUID(), 5000);
        PaperGrantReceiptPdc.write(container, partial);
        assertEquals(1, data.size());
        assertEquals(partial, PaperGrantReceiptPdc.read(container));
        PaperGrantReceiptPdc.write(container, new GrantReceiptState(8, 0, null, 0));
        assertEquals(new GrantReceiptState(8, 0, null, 0),
                PaperGrantReceiptPdc.read(container));
    }

    @Test void tamperedTruncatedAndWrongTypeReceiptsAreNeverEmpty() {
        PaperGrantReceiptPdc.write(container,
                new GrantReceiptState(0, 5, UUID.randomUUID(), 500));
        byte[] raw = ((byte[]) data.get(PaperGrantReceiptPdc.RECEIPT)).clone();
        raw[20] ^= 1;
        data.put(PaperGrantReceiptPdc.RECEIPT, raw);
        assertThrows(IllegalStateException.class,
                () -> PaperGrantReceiptPdc.read(container));
        data.put(PaperGrantReceiptPdc.RECEIPT, new byte[]{1, 2, 3});
        assertThrows(IllegalStateException.class,
                () -> PaperGrantReceiptPdc.read(container));
        data.put(PaperGrantReceiptPdc.RECEIPT, "wrong-type");
        assertThrows(IllegalStateException.class,
                () -> PaperGrantReceiptPdc.read(container));
    }

    @Test void checksumValidButInvalidIdleUuidIsRejected() throws Exception {
        PaperGrantReceiptPdc.write(container, GrantReceiptState.empty());
        byte[] raw = ((byte[]) data.get(PaperGrantReceiptPdc.RECEIPT)).clone();
        raw[20] = 1;
        byte[] sha = java.security.MessageDigest.getInstance("SHA-256")
                .digest(java.util.Arrays.copyOf(raw, 44));
        System.arraycopy(sha, 0, raw, 44, sha.length);
        data.put(PaperGrantReceiptPdc.RECEIPT, raw);
        assertThrows(IllegalStateException.class,
                () -> PaperGrantReceiptPdc.read(container));
    }
}
