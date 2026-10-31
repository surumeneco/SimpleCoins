package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Bounded, read-only decoding of a single saved Java playerdata file.
 * The caller must supply a consistent, authoritative snapshot. Reading a live
 * playerdata file cannot prove that a world-wide settlement is consistent.
 */
public final class PlayerDataReader {
    private PlayerDataReader() {}

    public static Map<String, Object> read(Path file, int maxCompressedBytes,
                                            int maxInflatedBytes) throws IOException {
        Objects.requireNonNull(file);
        if (maxCompressedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Playerdata budgets must be positive");

        String name = file.getFileName().toString();
        if (!name.endsWith(".dat"))
            throw new IOException("Not a playerdata file: " + file);
        String stem = name.substring(0, name.length() - 4);
        try {
            if (!UUID.fromString(stem).toString().equalsIgnoreCase(stem))
                throw new IOException("Non-canonical playerdata UUID: " + file);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid playerdata UUID: " + file, invalid);
        }

        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing or symbolic playerdata: " + file);
        long size = Files.size(file);
        if (size == 0 || size > maxCompressedBytes)
            throw new IOException("Playerdata exceeds compressed-byte budget: " + file);

        // Bound the entire compressed read even if the source changes after size().
        ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) Math.min(size, 8192));
        try (var input = Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) {
                if (n > maxCompressedBytes - bytes.size())
                    throw new IOException("Playerdata changed or exceeds compressed-byte budget: " + file);
                bytes.write(buffer, 0, n);
            }
        }
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return NbtReader.read(gzip, maxInflatedBytes);
        }
    }
}
