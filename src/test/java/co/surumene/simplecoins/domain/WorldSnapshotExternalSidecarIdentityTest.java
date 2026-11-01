package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: physical identity of referenced external Anvil payloads. */
class WorldSnapshotExternalSidecarIdentityTest {
    @TempDir Path directory;

    private Path region(boolean hardlink) throws IOException {
        Path file = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        byte[] bytes = new byte[4 * 4096];
        ByteBuffer.wrap(bytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(4, (3 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(8192, 1);
        ByteBuffer.wrap(bytes).putInt(3 * 4096, 1);
        bytes[8196] = (byte) (0x80 | 2);
        bytes[3 * 4096 + 4] = (byte) (0x80 | 2);
        Files.write(file, bytes);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream output = new DeflaterOutputStream(compressed)) {
            output.write(new byte[]{10, 0, 0, 0});
        }
        Path first = file.resolveSibling("c.0.0.mcc");
        Path second = file.resolveSibling("c.1.0.mcc");
        Files.write(first, compressed.toByteArray());
        if (hardlink) Files.createLink(second, first);
        else Files.write(second, compressed.toByteArray());
        return file;
    }

    private WorldSnapshotTraversal.Report traverse(Path region) throws IOException {
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(new WorldStorageDiscovery.SourceFile(
                        "world", region, WorldStorageDiscovery.Kind.REGION)), Map.of());
        return WorldSnapshotTraversal.traverse(manifest, 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(java.util.UUID id, Map<String, Object> data) {}
                });
    }

    @Test void hardLinkedExternalSidecarsMustNotCountAsIndependentInputs() throws Exception {
        assertThrows(IOException.class, () -> traverse(region(true)));
    }

    @Test void distinctExternalSidecarsRemainReadable() throws Exception {
        var report = traverse(region(false));
        assertEquals(1, report.regionFiles());
        assertEquals(2, report.chunks());
    }
}
