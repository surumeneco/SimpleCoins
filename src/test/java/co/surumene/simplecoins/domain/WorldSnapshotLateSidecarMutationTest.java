package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
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

/** TASK-20261101-003: referenced external payloads remain mandatory until traversal ends. */
class WorldSnapshotLateSidecarMutationTest {
    @TempDir Path directory;

    @Test void playerCallbackChangingEarlierExternalSidecarMustAbort() throws Exception {
        Path region = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        byte[] regionBytes = new byte[3 * 4096];
        ByteBuffer.wrap(regionBytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(regionBytes).putInt(8192, 1);
        regionBytes[8196] = (byte) (0x80 | 2);
        Files.write(region, regionBytes);

        ByteArrayOutputStream external = new ByteArrayOutputStream();
        try (DeflaterOutputStream output = new DeflaterOutputStream(external)) {
            output.write(new byte[]{10, 0, 0, 0});
        }
        Path sidecar = region.resolveSibling("c.0.0.mcc");
        Files.write(sidecar, external.toByteArray());

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Path playerdata = directory.resolve("playerdata/" + playerId + ".dat");
        Files.createDirectories(playerdata.getParent());
        ByteArrayOutputStream playerBytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(playerBytes)) {
            output.write(new byte[]{10, 0, 0, 0});
        }
        Files.write(playerdata, playerBytes.toByteArray());

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
                        Files.write(sidecar, new byte[]{42}, StandardOpenOption.APPEND);
                    }
                }));
    }
}
