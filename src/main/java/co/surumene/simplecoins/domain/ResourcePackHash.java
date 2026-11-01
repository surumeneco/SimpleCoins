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

    private static void requireRealAncestors(Path path) throws IOException {
        for (Path part = path; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part))
                throw new IOException("Generated resource-pack path contains a symbolic link: " + part);
        }
    }

    public static byte[] sha1(Path archive) throws IOException {
        if (archive == null)
            throw new IOException("Generated resource-pack archive is missing");
        Path absolute = archive.toAbsolutePath();
        for (Path component : absolute) {
            if ("..".equals(component.toString()))
                throw new IOException("Generated resource-pack path must not contain parent traversal");
        }
        Path source = absolute.normalize();
        requireRealAncestors(source);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Generated resource-pack archive is missing or not a regular file");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream input = Files.newInputStream(source,
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1)
                    digest.update(buffer, 0, read);
            }
            requireRealAncestors(source);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-1 unavailable", impossible);
        }
    }
}
