package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePackHashTest {
    @TempDir Path directory;

    @Test void streamsTheExactArchiveSha1() throws Exception {
        byte[] bytes = new byte[1_000_000];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 31);
        Path archive = Files.write(directory.resolve("generated.zip"), bytes);
        assertArrayEquals(MessageDigest.getInstance("SHA-1").digest(bytes),
                ResourcePackHash.sha1(archive));
        assertEquals(20, ResourcePackHash.sha1(archive).length);
    }

    @Test void rejectsAbsentNonregularAndLinkedArchives() throws Exception {
        Path archive = Files.write(directory.resolve("actual.zip"), new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> ResourcePackHash.sha1(null));
        assertThrows(IOException.class, () -> ResourcePackHash.sha1(directory.resolve("missing.zip")));
        assertThrows(IOException.class, () -> ResourcePackHash.sha1(directory));
        Path alias = directory.resolve("alias.zip");
        Files.createSymbolicLink(alias, archive);
        assertThrows(IOException.class, () -> ResourcePackHash.sha1(alias));
    }
}
