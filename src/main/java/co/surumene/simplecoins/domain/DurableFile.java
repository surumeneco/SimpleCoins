package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
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
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".pending-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = ByteBuffer.allocate(32 + payload.length).put(hash(payload)).put(payload);
                bytes.flip();
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                directory.force(true);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
    public static byte[] read(Path target, int maximumBytes) throws IOException {
        long size = Files.size(target);
        if (size < 32 || size > maximumBytes + 32L) throw new IOException("Invalid journal size: " + target);
        byte[] all = Files.readAllBytes(target);
        byte[] payload = Arrays.copyOfRange(all, 32, all.length);
        if (!MessageDigest.isEqual(Arrays.copyOf(all, 32), hash(payload)))
            throw new IOException("Journal checksum mismatch: " + target);
        return payload;
    }
    private static byte[] hash(byte[] payload) {
        try { return MessageDigest.getInstance("SHA-256").digest(payload); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
