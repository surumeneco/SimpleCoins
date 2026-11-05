package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: Draft SC-DURABLE-PATH-01..03.
 * Negative cases intentionally Red pending normative CAT confirmation.
 */
class DurableFileAncestorPathIntegrityTest {
    @TempDir Path directory;

    @Test void writeMustNotFollowSymbolicParentIntoUnverifiedDirectory() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real"));
        Path link = Files.createSymbolicLink(directory.resolve("journal"), real);
        Path target = link.resolve("record.grant");
        assertThrows(IOException.class, () -> DurableFile.write(target, new byte[]{1, 2, 3}));
        assertFalse(Files.exists(real.resolve("record.grant")),
                "Rejected write must not create a monetary record outside the trusted root");
    }

    @Test void readMustNotAcceptRegularRecordThroughSymbolicAncestor() throws Exception {
        Path real = Files.createDirectory(directory.resolve("real"));
        DurableFile.write(real.resolve("record.grant"), new byte[]{4, 5, 6});
        Path link = Files.createSymbolicLink(directory.resolve("journal"), real);
        assertThrows(IOException.class, () -> DurableFile.read(link.resolve("record.grant"), 3));
    }

    @Test void realNestedDirectoryRoundTripsWithChecksum() throws Exception {
        Path target = directory.resolve("real/nested/record.grant");
        byte[] payload = new byte[]{7, 8, 9};
        DurableFile.write(target, payload);
        assertArrayEquals(payload, DurableFile.read(target, payload.length));
    }
}
