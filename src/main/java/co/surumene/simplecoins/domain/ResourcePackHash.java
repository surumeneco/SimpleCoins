package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Bounded-memory SHA-1 for the generated Java resource-pack archive. */
public final class ResourcePackHash {
    private ResourcePackHash() {}

    public static byte[] sha1(Path archive) throws IOException {
        if (archive == null || !Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Generated resource-pack archive is missing or not a regular file");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(archive,
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1)
                    digest.update(buffer, 0, read);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-1 unavailable", impossible);
        }
    }
}
