package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePackHashPathBoundaryTest {
    @TempDir Path directory;

    @Test void emptyArchiveHasTwentyByteDigest() throws Exception {
        Path archive = Files.createFile(directory.resolve("empty.zip"));
        assertArrayEquals(MessageDigest.getInstance("SHA-1").digest(new byte[0]),
                ResourcePackHash.sha1(archive));
    }

    @Test void nestedRealArchiveIsHashedExactly() throws Exception {
        Path folder = Files.createDirectories(directory.resolve("real/pack"));
        byte[] content = {2, 4, 6, 8, 10};
        Path archive = Files.write(folder.resolve("pack.zip"), content);
        assertArrayEquals(MessageDigest.getInstance("SHA-1").digest(content),
                ResourcePackHash.sha1(archive));
    }
}
