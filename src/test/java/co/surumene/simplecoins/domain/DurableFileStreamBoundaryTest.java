package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: checksummed record boundaries across 8192-byte reads. */
class DurableFileStreamBoundaryTest {
    @TempDir Path directory;

    private static byte[] payload(int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
            bytes[i] = (byte) (i * 31 + 7);
        return bytes;
    }

    @Test void checksummedRecordsRoundTripAcrossReadBufferBoundaries() throws Exception {
        for (int length : new int[]{8191, 8192, 8193, 16384}) {
            Path record = directory.resolve(length + ".grant");
            byte[] expected = payload(length);
            DurableFile.write(record, expected);
            assertArrayEquals(expected, DurableFile.read(record, length));
        }
    }

    @Test void tornAndExtendedRecordsCannotBeReplayed() throws Exception {
        Path record = directory.resolve("torn.grant");
        DurableFile.write(record, payload(8193));
        byte[] complete = Files.readAllBytes(record);
        Files.write(record, Arrays.copyOf(complete, complete.length - 1));
        assertThrows(IOException.class, () -> DurableFile.read(record, 8193));
        Files.write(record, Arrays.copyOf(complete, complete.length + 1));
        assertThrows(IOException.class, () -> DurableFile.read(record, 8194));
    }

    @Test void mutationAtPhysicalReadBufferBoundaryFailsChecksum() throws Exception {
        Path record = directory.resolve("boundary.grant");
        DurableFile.write(record, payload(16384));
        byte[] bytes = Files.readAllBytes(record);
        bytes[8192] ^= 0x40;
        Files.write(record, bytes);
        assertThrows(IOException.class, () -> DurableFile.read(record, 16384));
    }

    @Test void boundedStreamNeverReturnsATruncatedPrefixAsSuccess() throws Exception {
        byte[] data = payload(8193);
        assertArrayEquals(data, DurableFile.readBounded(new ByteArrayInputStream(data), 8193));
        assertThrows(IOException.class, () -> DurableFile.readBounded(
                new ByteArrayInputStream(data), 8192));
    }
}
