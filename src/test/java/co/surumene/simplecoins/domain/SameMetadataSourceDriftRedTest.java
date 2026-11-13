package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

/** Draft Red: metadata equality is not sufficient proof of a consistent saved source. */
class SameMetadataSourceDriftRedTest {
    @TempDir Path directory;

    private static byte[] validRegion(int amount) throws IOException {
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(3); out.writeUTF("simplecoins_value"); out.writeInt(amount);
            out.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var deflate = new DeflaterOutputStream(compressed)) {
            deflate.write(nbt.toByteArray());
        }
        byte[] payload = compressed.toByteArray();
        byte[] region = new byte[3 * 4096];
        ByteBuffer.wrap(region).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(region).putInt(8192, payload.length + 1);
        region[8196] = 2;
        System.arraycopy(payload, 0, region, 8197, payload.length);
        return region;
    }

    @Test void changedEconomicNbtWithIdenticalFileSizeAndMtimeMustNotYieldSuccess() throws Exception {
        Path region = directory.resolve("world/region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        byte[] before = validRegion(100);
        byte[] after = validRegion(900);
        Files.write(region, before);
        FileTime originalTime = Files.getLastModifiedTime(region);

        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region, WorldStorageDiscovery.Kind.REGION)), Map.of());
        // Mutation is deliberately triggered *after* the initial chunk was read;
        // both NBT documents are valid but carry different economic values.
        // Preserving size and mtime isolates the unsupported metadata-ABA case.
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest, 4096, 4096, new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) throws IOException {
                        if (((Number) chunk.nbt().get("simplecoins_value")).intValue() != 100)
                            throw new IOException("Fixture must read the original saved amount");
                        if (before.length != after.length)
                            throw new IOException("Fixture changed file size");
                        Files.write(region, after);
                        Files.setLastModifiedTime(region, originalTime);
                    }
                    @Override public void player(UUID player, Map<String, Object> data) {}
                }), "The staged snapshot must be rejected, not mistaken for a coherent source");
    }
}
