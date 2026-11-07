package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import org.junit.jupiter.api.Test;

class NbtReaderTest {
    @Test void readsExactLongAndRejectsTruncatedPayload() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeByte(10); out.writeUTF("");
        out.writeByte(4); out.writeUTF("receipt"); out.writeLong(Long.MAX_VALUE);
        out.writeByte(0);
        assertEquals(Long.MAX_VALUE, NbtReader.read(new ByteArrayInputStream(bytes.toByteArray()), 1024).get("receipt"));
        assertThrows(IOException.class, () -> NbtReader.read(new ByteArrayInputStream(new byte[]{10,0,0,4}), 1024));
    }
}
