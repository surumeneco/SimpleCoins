package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Draft DURABLE-BOUNDARY vectors against the existing develop primitive. */
class DurableFileRecordBoundaryTest {
    @TempDir Path directory;

    @Test void emptyPayloadIsAValidChecksummedRecordWithZeroBudget() throws Exception {
        Path record = directory.resolve("empty.grant");
        DurableFile.write(record, new byte[0]);
        assertEquals(32L, Files.size(record));
        assertArrayEquals(new byte[0], DurableFile.read(record, 0));
    }

    @Test void exactReadBudgetSucceedsButOneByteLessFails() throws Exception {
        Path record = directory.resolve("bounded.grant");
        byte[] payload = {1, 2, 3, 4, 5};
        DurableFile.write(record, payload);
        assertArrayEquals(payload, DurableFile.read(record, payload.length));
        assertThrows(IOException.class, () -> DurableFile.read(record, payload.length - 1));
    }

    @Test void atomicReplacementDoesNotLeavePendingRecordOrOldPayload() throws Exception {
        Path record = directory.resolve("replace.grant");
        DurableFile.write(record, new byte[]{1, 2, 3});
        DurableFile.write(record, new byte[]{4, 5});
        assertArrayEquals(new byte[]{4, 5}, DurableFile.read(record, 2));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".pending-")));
        }
    }

    @Test void missingNestedParentIsCreatedForRecordWrite() throws Exception {
        Path record = directory.resolve("journal").resolve("nested").resolve("1.grant");
        DurableFile.write(record, new byte[]{7});
        assertArrayEquals(new byte[]{7}, DurableFile.read(record, 1));
    }

    @Test void liveSymlinkAndDirectoryCannotBeReadAsRecords() throws Exception {
        Path record = directory.resolve("real.grant");
        DurableFile.write(record, new byte[]{9});
        Path alias = Files.createSymbolicLink(directory.resolve("alias.grant"), record);
        assertThrows(IOException.class, () -> DurableFile.read(alias, 1));
        assertThrows(IOException.class, () -> DurableFile.read(directory, 1));
    }

    @Test void invalidReadBudgetsAndStreamsAreRejected() throws Exception {
        Path record = directory.resolve("valid.grant");
        DurableFile.write(record, new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> DurableFile.read(record, -1));
        assertThrows(IllegalArgumentException.class, () -> DurableFile.readBounded(null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> DurableFile.readBounded(new ByteArrayInputStream(new byte[]{1}), -1));
        assertArrayEquals(new byte[]{1, 2}, DurableFile.readBounded(
                new ByteArrayInputStream(new byte[]{1, 2}), 2));
    }
}
