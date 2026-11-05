package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005, code-to-spec Draft; not a full crash-recovery or CAT Gate. */
class DurableFileWriteFailureRollbackTest {
    @TempDir Path directory;

    private void assertNoPendingFiles() throws IOException {
        try (Stream<Path> paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(path ->
                    path.getFileName().toString().startsWith(".pending-")),
                    "A failed or successful write must not leave an uncommitted temp file");
        }
    }

    @Test void nullReplacementPreservesPreviouslyCommittedPayloadAndCleansTemp() throws Exception {
        Path record = directory.resolve("existing.grant");
        byte[] original = {1, 2, 3, 4};
        DurableFile.write(record, original);
        byte[] bytesBeforeFailure = Files.readAllBytes(record);

        assertThrows(NullPointerException.class, () -> DurableFile.write(record, null));

        assertArrayEquals(original, DurableFile.read(record, original.length));
        assertArrayEquals(bytesBeforeFailure, Files.readAllBytes(record),
                "Previously committed record bytes must remain intact");
        assertNoPendingFiles();
    }

    @Test void failedReplacementOfNonemptyDirectoryDoesNotRemoveDirectoryOrLeaveTemp() throws Exception {
        Path nonemptyTarget = directory.resolve("occupied.grant");
        Files.createDirectory(nonemptyTarget);
        Path marker = nonemptyTarget.resolve("existing-marker.txt");
        Files.writeString(marker, "existing value");

        assertThrows(IOException.class, () -> DurableFile.write(nonemptyTarget, new byte[]{9, 8}));

        assertTrue(Files.isDirectory(nonemptyTarget));
        assertEquals("existing value", Files.readString(marker));
        assertNoPendingFiles();
    }

    @Test void savedPayloadDoesNotAliasMutableCallerArrayAfterSuccessfulWrite() throws Exception {
        Path record = directory.resolve("copied.grant");
        byte[] callerArray = {4, 5, 6, 7};
        byte[] original = Arrays.copyOf(callerArray, callerArray.length);
        DurableFile.write(record, callerArray);
        Arrays.fill(callerArray, (byte) 0);

        assertArrayEquals(original, DurableFile.read(record, original.length));
        assertNoPendingFiles();
    }
}
