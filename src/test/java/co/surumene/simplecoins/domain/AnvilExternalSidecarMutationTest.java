package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: external Anvil sidecars are authoritative snapshot payloads. */
class AnvilExternalSidecarMutationTest {
    @TempDir Path directory;

    private Path region() throws IOException {
        Path file = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, 1); // external chunk stub
        region[8196] = (byte) (0x80 | 2); // external + zlib
        Files.write(file, region);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream output = new DeflaterOutputStream(compressed)) {
            output.write(new byte[]{10, 0, 0, 0}); // empty root NBT compound
        }
        Files.write(file.resolveSibling("c.0.0.mcc"), compressed.toByteArray());
        return file;
    }

    @Test void externalChunkChangedDuringCallbackMustAbortScan() throws Exception {
        Path file = region();
        Path sidecar = file.resolveSibling("c.0.0.mcc");
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(
                file, 4096, 4096,
                chunk -> Files.write(sidecar, new byte[]{42}, StandardOpenOption.APPEND)));
    }

    @Test void unchangedExternalChunkRemainsReadable() throws Exception {
        Path file = region();
        AtomicInteger observed = new AtomicInteger();
        AnvilRegionReader.scan(file, 4096, 4096, chunk -> {
            assertEquals(0, chunk.x());
            assertEquals(0, chunk.z());
            assertTrue(chunk.nbt().isEmpty());
            observed.incrementAndGet();
        });
        assertEquals(1, observed.get());
    }
}
