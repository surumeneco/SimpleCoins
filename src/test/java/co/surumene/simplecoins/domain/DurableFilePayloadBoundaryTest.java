package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: current DurableFile behavior only.
 * CAT Draft: simplecoins.journal.durable-file-payload-boundaries (not Confirmed).
 * This suite is not evidence of crash recovery, Paper live behavior, or a CAT Gate.
 */
class DurableFilePayloadBoundaryTest {
    @TempDir Path directory;

    @Test void payloadAtExactByteBudgetSucceedsAndOneByteLessFailsClosed() throws Exception {
        Path record = directory.resolve("1.grant");
        byte[] payload = {3, 1, 4, 1};
        DurableFile.write(record, payload);

        assertArrayEquals(payload, DurableFile.read(record, 4));
        assertThrows(IOException.class, () -> DurableFile.read(record, 3),
                "The bound applies to payload bytes, excluding its checksum");
    }

    @Test void emptyPayloadAndNegativeBudgetAreDistinctBoundaries() throws Exception {
        Path record = directory.resolve("2.grant");
        DurableFile.write(record, new byte[0]);

        assertArrayEquals(new byte[0], DurableFile.read(record, 0));
        assertThrows(IllegalArgumentException.class, () -> DurableFile.read(record, -1));
    }

    @Test void validTargetSymlinkAndDirectoryCannotMasqueradeAsDurableRecord() throws Exception {
        Path record = directory.resolve("3.grant");
        DurableFile.write(record, new byte[]{7});
        Path validTargetAlias = Files.createSymbolicLink(directory.resolve("alias.grant"), record);

        assertThrows(IOException.class, () -> DurableFile.read(validTargetAlias, 1),
                "An alias to a valid record is still a symbolic link");
        assertThrows(IOException.class, () -> DurableFile.read(directory, 1),
                "A directory cannot be interpreted as a checksummed record");
    }

    @Test void replacementDoesNotExposePreviousPayloadOrLeaveTemporaryFiles() throws Exception {
        Path record = directory.resolve("4.grant");
        DurableFile.write(record, new byte[]{1, 2});
        byte[] replacement = {2, 7, 1, 8, 2, 8};
        DurableFile.write(record, replacement);

        assertArrayEquals(replacement, DurableFile.read(record, replacement.length));
        try (Stream<Path> paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".pending-")),
                    "A successful atomic replacement must not leave a pending record behind");
        }
    }
}
