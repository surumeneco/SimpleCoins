package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettlementScanRunnerTest {
    @TempDir Path directory;

    private Path playerData(UUID player) throws IOException {
        Path file = directory.resolve("playerdata/" + player + ".dat");
        Files.createDirectories(file.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10); out.writeInt(0);
            out.writeByte(9); out.writeUTF("EnderItems"); out.writeByte(10); out.writeInt(0);
            out.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) {
            gzip.write(nbt.toByteArray());
        }
        Files.write(file, compressed.toByteArray());
        return file;
    }

    private Path emptyRegion() throws IOException {
        Path file = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(file.getParent());
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(9); out.writeUTF("block_entities");
            out.writeByte(10); out.writeInt(0);
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
        Files.write(file, region);
        return file;
    }

    @Test void stagesAllManifestSourcesWithoutAutomaticallyCommitting() throws Exception {
        UUID player = UUID.randomUUID();
        Path region = emptyRegion(), data = playerData(player);
        var manifest = new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile("world", region, WorldStorageDiscovery.Kind.REGION)),
                Map.of(player, data));
        var staged = SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> { throw new IOException("No empty container needs ownership"); },
                (world, chunk) -> Map.of(), 4096, 4096);
        assertEquals(manifest.requiredSourceIds(), staged.requiredSourceIds());
        assertEquals(1, staged.report().regionFiles());
        assertEquals(1, staged.report().playerFiles());
        // The coordinator must leave publication to a separate verified gate.
        assertEquals(BigInteger.ZERO, staged.pending().commit().statistics().worldTotal());
    }

    @Test void invalidPlayerdataAbortsStagingWithoutReturningPartialResult() throws Exception {
        UUID player = UUID.randomUUID();
        Path data = playerData(player);
        Files.write(data, new byte[]{1, 2, 3});
        var manifest = new WorldStorageDiscovery.Manifest(List.of(), Map.of(player, data));
        assertThrows(IOException.class, () -> SettlementScanRunner.stage(manifest,
                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                (world, position) -> null, (world, chunk) -> Map.of(), 4096, 4096));
    }
}
