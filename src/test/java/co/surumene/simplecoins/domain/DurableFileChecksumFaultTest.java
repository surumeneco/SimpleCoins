package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005; code-to-spec Draft DURABLE-PAYLOAD-05..08.
 * Existing checksum behavior only: this is not a crash-recovery or CAT Gate verdict.
 */
class DurableFileChecksumFaultTest {
    @TempDir Path directory;

    private Path writeSigned(byte[] payload) throws IOException {
        Path file = directory.resolve("record.grant");
        DurableFile.write(file, payload);
        return file;
    }

    @Test void changedPayloadCannotPassAnUnmodifiedDigestOrRewriteEvidence() throws Exception {
        byte[] payload = {9, 8, 7, 6, 5};
        Path file = writeSigned(payload);
        byte[] corrupted = Files.readAllBytes(file);
        corrupted[32 + 2] ^= 0x40;
        Files.write(file, corrupted);

        assertThrows(IOException.class, () -> DurableFile.read(file, payload.length));
        assertArrayEquals(corrupted, Files.readAllBytes(file),
                "A failed read must not overwrite or repair corruption evidence");
    }

    @Test void changedDigestCannotPassWhenPayloadIsStillIntact() throws Exception {
        byte[] payload = {9, 8, 7, 6, 5};
        Path file = writeSigned(payload);
        byte[] corrupted = Files.readAllBytes(file);
        corrupted[8] ^= 0x02;
        Files.write(file, corrupted);

        assertThrows(IOException.class, () -> DurableFile.read(file, payload.length));
        assertArrayEquals(corrupted, Files.readAllBytes(file));
    }

    @Test void truncatedHeaderAndTruncatedPayloadBothFailClosed() throws Exception {
        byte[] payload = {9, 8, 7, 6, 5};
        Path file = writeSigned(payload);
        byte[] original = Files.readAllBytes(file);
        Files.write(file, Arrays.copyOf(original, 31));
        assertThrows(IOException.class, () -> DurableFile.read(file, payload.length),
                "An incomplete 32-byte digest is not an empty journal record");

        Files.write(file, Arrays.copyOf(original, original.length - 1));
        assertThrows(IOException.class, () -> DurableFile.read(file, payload.length),
                "A missing payload byte cannot be interpreted as a valid shorter grant");
    }

    @Test void appendedPayloadFailsDespiteLargerBudgetAndRestorationRecovers() throws Exception {
        byte[] payload = {9, 8, 7, 6, 5};
        Path file = writeSigned(payload);
        byte[] original = Files.readAllBytes(file);
        byte[] appended = Arrays.copyOf(original, original.length + 1);
        appended[appended.length - 1] = 123;
        Files.write(file, appended);

        assertThrows(IOException.class, () -> DurableFile.read(file, payload.length + 1),
                "Permissive file-size limits must never disable checksum validation");

        Files.write(file, original);
        assertArrayEquals(payload, DurableFile.read(file, payload.length));
    }
}
