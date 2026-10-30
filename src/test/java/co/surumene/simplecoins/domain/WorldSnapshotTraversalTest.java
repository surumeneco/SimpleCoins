package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSnapshotTraversalTest {
    @TempDir Path directory;

    private static byte[] nbt(int amount) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(3); out.writeUTF("testValue"); out.writeInt(amount);
            out.writeByte(0);
        }
        return bytes.toByteArray();
    }

    private Path region(String folder, int amount) throws IOException {
        Path path = directory.resolve(folder).resolve("r.0.0.mca");
        Files.createDirectories(path.getParent());
        var compressed = new ByteArrayOutputStream();
        try (var deflate = new DeflaterOutputStream(compressed)) {
            deflate.write(nbt(amount));
        }
        byte[] payload = compressed.toByteArray();
        byte[] region = new byte[3 * 4096];
        var head = ByteBuffer.wrap(region);
        head.putInt(0, (2 << 8) | 1);
        head.putInt(8192, payload.length + 1);
        region[8196] = 2;
        System.arraycopy(payload, 0, region, 8197, payload.length);
        Files.write(path, region);
        return path;
    }

    private Path player(UUID uuid, int amount) throws IOException {
        Path path = directory.resolve("players/data").resolve(uuid + ".dat");
        Files.createDirectories(path.getParent());
        var bytes = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bytes)) {
            gzip.write(nbt(amount));
        }
        Files.write(path, bytes.toByteArray());
        return path;
    }

    @Test void readsEachDiscoveredRegionAndPlayerWithoutSilentlySkippingSources() throws Exception {
        Path terrain = region("region", 10);
        Path entities = region("entities", 20);
        UUID uuid = UUID.randomUUID();
        Path saved = player(uuid, 30);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", terrain, WorldStorageDiscovery.Kind.REGION),
                new WorldStorageDiscovery.SourceFile("world", entities, WorldStorageDiscovery.Kind.ENTITY)),
                Map.of(uuid, saved));
        var values = new java.util.ArrayList<Integer>();
        var visitor = new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                         AnvilRegionReader.Chunk chunk) throws IOException {
                values.add((Integer) chunk.nbt().get("testValue"));
            }
            @Override public void player(UUID player, Map<String,Object> data) throws IOException {
                assertEquals(uuid, player);
                values.add((Integer) data.get("testValue"));
            }
        };
        var report = WorldSnapshotTraversal.traverse(manifest, 4096, 4096, visitor);
        assertEquals(2, report.regionFiles());
        assertEquals(2, report.chunks());
        assertEquals(1, report.playerFiles());
        assertEquals(List.of(10, 20, 30), values);
    }

    @Test void failedDiscoveredPlayerFileCannotYieldSuccessfulTraversalReport() throws Exception {
        Path terrain = region("region", 10);
        UUID uuid = UUID.randomUUID();
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", terrain, WorldStorageDiscovery.Kind.REGION)),
                Map.of(uuid, directory.resolve("players/data").resolve(uuid + ".dat")));
        AtomicInteger observedChunks = new AtomicInteger();
        var visitor = new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile file,
                                         AnvilRegionReader.Chunk chunk) { observedChunks.incrementAndGet(); }
            @Override public void player(UUID player, Map<String,Object> data) {}
        };
        assertThrows(IOException.class, () ->
                WorldSnapshotTraversal.traverse(manifest, 4096, 4096, visitor));
        assertEquals(1, observedChunks.get(), "Early staged data cannot be published after a later error");
    }

    @Test void callbackFailurePropagatesWithoutSuccessReport() throws Exception {
        Path terrain = region("region", 1);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", terrain, WorldStorageDiscovery.Kind.REGION)),
                Map.of());
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile file,
                                                AnvilRegionReader.Chunk chunk) throws IOException {
                        throw new IOException("Invalid inventory");
                    }
                    @Override public void player(UUID player, Map<String,Object> data) {}
                }));
    }
    @Test void rejectsMislabeledEntityRegionBeforeVisitingAssets() throws Exception {
        Path entityFile = region("entities", 10);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", entityFile,
                        WorldStorageDiscovery.Kind.REGION)), Map.of());
        AtomicInteger visited = new AtomicInteger();
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {
                        visited.incrementAndGet();
                    }
                    @Override public void player(UUID player, Map<String,Object> data) {
                        visited.incrementAndGet();
                    }
                }));
        assertEquals(0, visited.get());
    }

    @Test void rejectsPlayerdataManifestWithMismatchedUuidBeforeVisitingAssets() throws Exception {
        UUID actual = UUID.randomUUID();
        UUID claimed = UUID.randomUUID();
        Path file = player(actual, 25);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(),
                Map.of(claimed, file));
        AtomicInteger visited = new AtomicInteger();
        assertThrows(IOException.class, () ->
                WorldSnapshotTraversal.traverse(manifest, 4096, 4096,
                        new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {
                        visited.incrementAndGet();
                    }
                    @Override public void player(UUID player, Map<String,Object> data) {
                        visited.incrementAndGet();
                    }
                }));
        assertEquals(0, visited.get());
    }

}
