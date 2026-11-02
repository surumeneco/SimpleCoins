package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableFileTest {
    @TempDir Path directory;

    @Test void boundedJournalStreamRejectsGrowthPastByteLimit() throws Exception {
        byte[] input = new byte[17];
        assertEquals(16, DurableFile.readBounded(
                new ByteArrayInputStream(Arrays.copyOf(input, 16)), 16).length);
        assertThrows(IOException.class, () -> DurableFile.readBounded(
                new ByteArrayInputStream(input), 16));
    }

    @Test void stableChecksummedJournalRecordStillRoundTrips() throws Exception {
        Path path = directory.resolve("1.grant");
        byte[] payload = new byte[]{1, 2, 3, 4};
        DurableFile.write(path, payload);
        assertArrayEquals(payload, DurableFile.read(path, 4));
    }

    @Test void tamperedTruncatedOrLinkedJournalRecordsFailClosed() throws Exception {
        Path record = directory.resolve("1.grant");
        DurableFile.write(record, new byte[]{1, 2, 3, 4});
        byte[] saved = java.nio.file.Files.readAllBytes(record);
        saved[saved.length - 1] ^= 1;
        java.nio.file.Files.write(record, saved);
        assertThrows(IOException.class, () -> DurableFile.read(record, 4),
                "Checksum mismatch must never replay a corrupted grant");
        java.nio.file.Files.write(record, java.util.Arrays.copyOf(saved, 31));
        assertThrows(IOException.class, () -> DurableFile.read(record, 4),
                "Truncated checksum must never be interpreted as empty data");
        java.nio.file.Files.delete(record);
        assertThrows(IOException.class, () -> DurableFile.read(record, 4));
        Path alias = java.nio.file.Files.createSymbolicLink(
                directory.resolve("alias.grant"), directory.resolve("missing.grant"));
        assertThrows(IOException.class, () -> DurableFile.read(alias, 4),
                "Dangling symbolic links are not journal records");
    }
}
