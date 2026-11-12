package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** External .mcc data is a real saved-value authority, not untracked MCA metadata. */
class ExternalSidecarSourceDriftTest {
    @TempDir Path directory;

    private static byte[] payload(int marker) throws IOException {
        var nbt = new ByteArrayOutputStream();
        try (var output = new DataOutputStream(nbt)) {
            output.writeByte(10); output.writeUTF("");
            output.writeByte(3); output.writeUTF("marker"); output.writeInt(marker);
            output.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var deflater = new DeflaterOutputStream(compressed)) {
            deflater.write(nbt.toByteArray());
        }
        return compressed.toByteArray();
    }

    private Path sidecarRegion(int marker) throws IOException {
        Path region = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        byte[] header = new byte[3 * 4096];
        ByteBuffer.wrap(header).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(header).putInt(8192, 1); // external stub holds only compression byte
        header[8196] = (byte) (0x80 | 2); // zlib in c.0.0.mcc
        Files.write(region, header);
        Files.write(region.resolveSibling("c.0.0.mcc"), payload(marker));
        return region;
    }

    private static WorldSnapshotTraversal.Visitor visitor(
            AnvilRegionReader.ChunkConsumer chunk) {
        return new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile file,
                                        AnvilRegionReader.Chunk input) throws IOException {
                chunk.accept(input);
            }
            @Override public void player(UUID player, Map<String,Object> data) {}
        };
    }

    @Test void detectsChangedSidecarEvenIfSizeAndMtimeAreRestored() throws Exception {
        Path mca = sidecarRegion(123), mcc = mca.resolveSibling("c.0.0.mcc");
        FileTime originalTime = Files.getLastModifiedTime(mcc);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", mca, WorldStorageDiscovery.Kind.REGION)),
                Map.of());
        byte[] rewritten = payload(124);
        assertEquals(Files.size(mcc), rewritten.length);
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, visitor(chunk -> {
                    assertEquals(123, chunk.nbt().get("marker"));
                    Files.write(mcc, rewritten);
                    Files.setLastModifiedTime(mcc, originalTime);
                })), "MCA hash and mtime alone are insufficient for a changing external chunk");
    }

    @Test void detectsSidecarDeletionAfterChunkVisit() throws Exception {
        Path mca = sidecarRegion(321), mcc = mca.resolveSibling("c.0.0.mcc");
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", mca, WorldStorageDiscovery.Kind.REGION)),
                Map.of());
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, visitor(chunk -> Files.delete(mcc))));
    }

    @Test void stableExternalChunkStillTraversesSuccessfully() throws Exception {
        Path mca = sidecarRegion(42);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", mca, WorldStorageDiscovery.Kind.REGION)),
                Map.of());
        var report = WorldSnapshotTraversal.traverse(manifest, 4096, 4096,
                visitor(chunk -> assertEquals(42, chunk.nbt().get("marker"))));
        assertEquals(1, report.regionFiles());
        assertEquals(1, report.chunks());
    }
}
