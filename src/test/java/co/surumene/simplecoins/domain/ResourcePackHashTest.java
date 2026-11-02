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

    @Test void symlinkedAncestorCannotRedirectGeneratedPackHash() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real-pack-dir"));
        Files.write(real.resolve("generated.zip"), new byte[]{1, 2, 3});
        Path alias = Files.createSymbolicLink(directory.resolve("alias-pack-dir"), real);
        assertThrows(IOException.class,
                () -> ResourcePackHash.sha1(alias.resolve("generated.zip")));
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
