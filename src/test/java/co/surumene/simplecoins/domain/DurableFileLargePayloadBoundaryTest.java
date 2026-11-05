package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: code-to-spec regression for the existing checksummed file API.
 * Does not prove journal replay, atomic crash recovery, or a passed CAT semantic Gate.
 */
class DurableFileLargePayloadBoundaryTest {
    @TempDir Path directory;

    private static byte[] payload(int size, int seed) {
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) ((i * 113 + seed * 37) ^ (i >>> 3));
        }
        return result;
    }

    @Test
    void checksumEnvelopeRoundTripsAcrossEightKilobyteReadBoundaries() throws Exception {
        for (int size : new int[]{8191, 8192, 8193, 16385}) {
            Path target = directory.resolve(size + ".grant");
            byte[] original = payload(size, 1);
            DurableFile.write(target, original);
            assertEquals(size + 32L, Files.size(target), "SHA-256 envelope size " + size);
            assertArrayEquals(original, DurableFile.read(target, size),
                    "No truncation or order changes at size=" + size);
        }
    }

    @Test
    void boundOneByteShortFailsClosedWithoutDamagingRecord() throws Exception {
        Path target = directory.resolve("bounded.grant");
        byte[] original = payload(8193, 2);
        DurableFile.write(target, original);
        byte[] before = Files.readAllBytes(target);

        assertThrows(IOException.class, () -> DurableFile.read(target, 8192));
        assertArrayEquals(before, Files.readAllBytes(target), "Read failure cannot mutate durable bytes");
        assertArrayEquals(original, DurableFile.read(target, 8193));
    }

    @Test
    void payloadMutationBeyondFirstBufferIsRejectedByChecksum() throws Exception {
        Path target = directory.resolve("corrupted.grant");
        byte[] original = payload(8193, 3);
        DurableFile.write(target, original);
        byte[] corrupted = Files.readAllBytes(target);
        corrupted[32 + 8192] ^= 0x4f;
        Files.write(target, corrupted);

        assertThrows(IOException.class, () -> DurableFile.read(target, original.length),
                "A later buffer must be authenticated, not merely the first block");
    }

    @Test
    void crossingBufferBoundaryOnReplacementKeepsOneCompleteRecord() throws Exception {
        Path target = directory.resolve("replacement.grant");
        byte[] replacement = payload(8193, 5);
        DurableFile.write(target, payload(8191, 4));
        DurableFile.write(target, replacement);

        assertArrayEquals(replacement, DurableFile.read(target, replacement.length));
        try (Stream<Path> entries = Files.list(directory)) {
            assertFalse(entries.anyMatch(p -> p.getFileName().toString().startsWith(".pending-")),
                    "A complete replacement must not leave an uncommitted temporary record");
        }
    }
}
