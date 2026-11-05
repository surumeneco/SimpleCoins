package co.surumene.simplecoins.domain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Small checksummed records. No non-atomic fallback for monetary journals. */
public final class DurableFile {
    private DurableFile() {}
    public static void write(Path target, byte[] payload) throws IOException {
        requireNoSymbolicAncestors(target);
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        requireNoSymbolicAncestors(target);
        Path temporary = Files.createTempFile(parent, ".pending-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = ByteBuffer.allocate(32 + payload.length).put(hash(payload)).put(payload);
                bytes.flip();
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            requireNoSymbolicAncestors(target);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                directory.force(true);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    public static byte[] read(Path target, int maximumBytes) throws IOException {
        if (maximumBytes < 0) throw new IllegalArgumentException("Negative journal budget");
        requireNoSymbolicAncestors(target);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Journal record is missing, non-regular or symbolic: " + target);
        long size = Files.size(target);
        if (size < 32 || size > maximumBytes + 32L || size > Integer.MAX_VALUE)
            throw new IOException("Invalid journal size: " + target);
        byte[] all;
        requireNoSymbolicAncestors(target);
        try (InputStream input = Files.newInputStream(target,
                StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            all = readBounded(input, (int) size);
        }
        requireNoSymbolicAncestors(target);
        if (all.length != size)
            throw new IOException("Journal changed while reading: " + target);
        byte[] payload = Arrays.copyOfRange(all, 32, all.length);
        if (!MessageDigest.isEqual(Arrays.copyOf(all, 32), hash(payload)))
            throw new IOException("Journal checksum mismatch: " + target);
        return payload;
    }
    static byte[] readBounded(InputStream input, int maximumBytes) throws IOException {
        if (input == null || maximumBytes < 0)
            throw new IllegalArgumentException("Invalid bounded journal input");
        var output = new ByteArrayOutputStream(Math.min(maximumBytes, 8192));
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count > maximumBytes - output.size())
                throw new IOException("Journal stream exceeds byte budget");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    /** Reject redirection through links anywhere in a durable journal record path. */
    private static void requireNoSymbolicAncestors(Path target) throws IOException {
        if (target == null) throw new IOException("Missing durable record path");
        Path absolute = target.toAbsolutePath();
        for (Path component : absolute)
            if ("..".equals(component.toString()))
                throw new IOException("Durable record path traverses parent directories");
        for (Path part = absolute.normalize(); part != null; part = part.getParent())
            if (Files.isSymbolicLink(part))
                throw new IOException("Durable record path contains a symbolic link: " + part);
    }

    private static byte[] hash(byte[] payload) {
        try { return MessageDigest.getInstance("SHA-256").digest(payload); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
