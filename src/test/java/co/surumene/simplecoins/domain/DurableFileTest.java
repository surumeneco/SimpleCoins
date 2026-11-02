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
}
