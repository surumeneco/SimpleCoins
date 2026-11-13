package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Historical Draft Red for existing symbolic link at final journal
 * write target. Ancestor symlinks are covered by a separate PR.
 */
class DurableFileSymlinkTargetTest {
    @TempDir Path directory;

    @Test void existingSymlinkTargetMustFailWithoutReplacingLinkOrReferent() throws Exception {
        Path outside = directory.resolve("outside.txt");
        Files.writeString(outside, "sentinel");
        Path link = Files.createSymbolicLink(directory.resolve("linked.grant"), outside);
        assertThrows(IOException.class, () -> DurableFile.write(link, new byte[]{1, 2}));
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("sentinel", Files.readString(outside));
    }

    @Test void danglingSymlinkTargetMustNotBecomeNewJournalRecord() throws Exception {
        Path link = Files.createSymbolicLink(
                directory.resolve("dangling.grant"), directory.resolve("missing"));
        assertThrows(IOException.class, () -> DurableFile.write(link, new byte[]{3}));
        assertTrue(Files.isSymbolicLink(link));
    }

    @Test void regularRecordReplacementStillSucceeds() throws Exception {
        Path record = directory.resolve("regular.grant");
        DurableFile.write(record, new byte[]{4});
        DurableFile.write(record, new byte[]{5});
        assertArrayEquals(new byte[]{5}, DurableFile.read(record, 1));
    }
}
