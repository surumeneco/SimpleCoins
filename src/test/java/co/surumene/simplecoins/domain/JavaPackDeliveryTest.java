package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

final class JavaPackDeliveryTest {
    @TempDir Path directory;

    @Test void approvedRequestPartition() {
        for (int bits = 0; bits < 8; bits++) {
            assertEquals(bits == 6, JavaPackDelivery.shouldRequest((bits & 1) != 0,
                    (bits & 2) != 0, (bits & 4) != 0));
        }
    }

    @Test void preparedRequestUsesArchiveHashAndDoesNotSendToBedrockOrNexo() throws Exception {
        Path archive = directory.resolve("coins.zip");
        byte[] content = {1,2,3};
        Files.write(archive, content);
        var expectedHash = MessageDigest.getInstance("SHA-1").digest(content);
        var prepared = JavaPackDelivery.prepare("https://example.org/coins.zip", archive).orElseThrow();
        prepared.sha1()[0] = 0;
        assertArrayEquals(expectedHash, prepared.sha1());
        var requests = new ArrayList<String>();
        for (int i = 0; i < 4; i++) {
            prepared.request((i & 1) != 0, (i & 2) != 0, (id, url, hash) -> {
                assertEquals(JavaPackDelivery.PACK_ID, id);
                assertEquals("https://example.org/coins.zip", url);
                assertArrayEquals(expectedHash, hash);
                requests.add(url);
            });
        }
        assertEquals(1, requests.size());
    }

    @Test void missingOrInvalidConfigurationCannotPrepareRequest() throws Exception {
        Path absent = directory.resolve("absent.zip");
        assertTrue(JavaPackDelivery.prepare("", absent).isEmpty());
        assertThrows(IOException.class, () -> JavaPackDelivery.prepare("file:///coins.zip", absent));
        assertThrows(IOException.class, () -> JavaPackDelivery.prepare("https://example.org/coins.zip", absent));
    }
}
