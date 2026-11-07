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

/** A successful report must not be returned for visibly drifting disk inputs. */
class WorldSnapshotSourceDriftTest {
    @TempDir Path directory;

    private static byte[] rootNbt() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(10);
            out.writeUTF("");
            out.writeByte(0);
        }
        return bytes.toByteArray();
    }

    private Path region() throws IOException {
        Path path = directory.resolve("world/region/r.0.0.mca");
        Files.createDirectories(path.getParent());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream out = new DeflaterOutputStream(compressed)) {
            out.write(rootNbt());
        }
        byte[] chunk = compressed.toByteArray();
        byte[] bytes = new byte[3 * 4096];
        ByteBuffer header = ByteBuffer.wrap(bytes);
        header.putInt(0, (2 << 8) | 1);
        header.putInt(8192, chunk.length + 1);
        bytes[8196] = 2;
        System.arraycopy(chunk, 0, bytes, 8197, chunk.length);
        Files.write(path, bytes);
        return path;
    }

    private Path player(UUID uuid) throws IOException {
        Path path = directory.resolve("world/playerdata/" + uuid + ".dat");
        Files.createDirectories(path.getParent());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(compressed)) {
            out.write(rootNbt());
        }
        Files.write(path, compressed.toByteArray());
        return path;
    }

    @Test void changedRegionAborts() throws Exception {
        Path path = region();
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(new WorldStorageDiscovery.SourceFile(
                        "world", path, WorldStorageDiscovery.Kind.REGION)), Map.of());
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) throws IOException {
                        Files.write(path, new byte[]{1}, StandardOpenOption.APPEND);
                    }
                    @Override public void player(UUID player, Map<String, Object> data) {}
                }), "A region modified after its chunk was visited must invalidate the report");
    }

    @Test void changedPlayerDataAborts() throws Exception {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000103");
        Path path = player(uuid);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(uuid, path));
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID who, Map<String, Object> data) throws IOException {
                        Files.write(path, new byte[]{1}, StandardOpenOption.APPEND);
                    }
                }), "Playerdata changed during the read must invalidate the report");
    }

    @Test void latePlayerDataAborts() throws Exception {
        Path terrain = region();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000104");
        Path absent = directory.resolve("world/playerdata/" + uuid + ".dat");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", terrain,
                        WorldStorageDiscovery.Kind.REGION)), Map.of(uuid, absent));
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) throws IOException {
                        player(uuid);
                    }
                    @Override public void player(UUID who, Map<String, Object> data) {}
                }), "A playerdata source absent at preflight must not appear and be accepted mid-scan");
    }

    @Test void stableSourcesPass() throws Exception {
        Path terrain = region();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000105");
        Path saved = player(uuid);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", terrain,
                        WorldStorageDiscovery.Kind.REGION)), Map.of(uuid, saved));
        WorldSnapshotTraversal.Report report = WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID who, Map<String, Object> data) {}
                });
        assertEquals(1, report.regionFiles());
        assertEquals(1, report.chunks());
        assertEquals(1, report.playerFiles());
    }
}
