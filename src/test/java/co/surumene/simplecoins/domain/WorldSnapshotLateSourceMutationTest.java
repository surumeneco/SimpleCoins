package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.GZIPOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: revalidate earlier saved sources after later callbacks. */
class WorldSnapshotLateSourceMutationTest {
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
        byte[] bytes = new byte[3 * 4096];
        ByteBuffer.wrap(bytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(8192, payload.length + 1);
        bytes[8196] = 2;
        System.arraycopy(payload, 0, bytes, 8197, payload.length);
        Files.write(file, bytes);
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

    @Test void laterPlayerCallbackChangingEarlierRegionMustAbort() throws Exception {
        Path region = region();
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Path playerdata = player(playerId);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(new WorldStorageDiscovery.SourceFile(
                        "world", region, WorldStorageDiscovery.Kind.REGION)),
                Map.of(playerId, playerdata));
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID player, Map<String, Object> data)
                            throws IOException {
                        Files.write(region, new byte[]{42}, StandardOpenOption.APPEND);
                    }
                }));
    }

    @Test void laterPlayerCallbackChangingEarlierPlayerdataMustAbort() throws Exception {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Path earlier = player(first);
        Path later = player(second);
        var manifest = new WorldStorageDiscovery.Manifest(
                List.of(), Map.of(first, earlier, second, later));
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID player, Map<String, Object> data)
                            throws IOException {
                        if (second.equals(player))
                            Files.write(earlier, new byte[]{42}, StandardOpenOption.APPEND);
                    }
                }));
    }
}
