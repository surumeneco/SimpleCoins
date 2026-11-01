package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: changed authoritative saved files cannot yield a success report. */
class WorldSnapshotSourceMutationTest {
    @TempDir Path directory;

    private static byte[] emptyCompound() {
        return new byte[]{10, 0, 0, 0};
    }

    private Path region() throws IOException {
        Path file = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream output = new DeflaterOutputStream(compressed)) {
            output.write(emptyCompound());
        }
        byte[] payload = compressed.toByteArray();
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, payload.length + 1);
        region[8196] = 2;
        System.arraycopy(payload, 0, region, 8197, payload.length);
        Files.write(file, region);
        return file;
    }

    private Path player(UUID uuid) throws IOException {
        Path file = directory.resolve("playerdata/" + uuid + ".dat");
        Files.createDirectories(file.getParent());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(compressed)) {
            output.write(emptyCompound());
        }
        Files.write(file, compressed.toByteArray());
        return file;
    }

    private WorldStorageDiscovery.Manifest manifest(Path region, UUID uuid, Path player) {
        return new WorldStorageDiscovery.Manifest(
                List.of(new WorldStorageDiscovery.SourceFile(
                        "world", region, WorldStorageDiscovery.Kind.REGION)),
                Map.of(uuid, player));
    }

    @Test void regionMutationDuringChunkCallbackMustAbortTraversal() throws Exception {
        Path region = region();
        UUID uuid = UUID.randomUUID();
        Path player = player(uuid);
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(region, uuid, player), 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) throws IOException {
                        Files.write(region, new byte[]{42}, StandardOpenOption.APPEND);
                    }
                    @Override public void player(UUID playerId, Map<String, Object> data) {}
                }));
    }

    @Test void playerdataMutationDuringPlayerCallbackMustAbortTraversal() throws Exception {
        Path region = region();
        UUID uuid = UUID.randomUUID();
        Path player = player(uuid);
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(region, uuid, player), 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID playerId, Map<String, Object> data)
                            throws IOException {
                        Files.write(player, new byte[]{42}, StandardOpenOption.APPEND);
                    }
                }));
    }

    @Test void unchangedSourcesStillYieldCompleteReport() throws Exception {
        Path region = region();
        UUID uuid = UUID.randomUUID();
        Path player = player(uuid);
        var report = WorldSnapshotTraversal.traverse(
                manifest(region, uuid, player), 4096, 4096,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID playerId, Map<String, Object> data) {}
                });
        assertEquals(1, report.regionFiles());
        assertEquals(1, report.chunks());
        assertEquals(1, report.playerFiles());
    }
}
