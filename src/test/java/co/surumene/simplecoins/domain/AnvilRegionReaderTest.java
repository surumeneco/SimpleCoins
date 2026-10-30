package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnvilRegionReaderTest {
    @TempDir Path directory;

    private static byte[] namedRoot(int marker) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(10); // root compound
            out.writeUTF("");
            out.writeByte(3); out.writeUTF("marker"); out.writeInt(marker);
            out.writeByte(0);
        }
        return bytes.toByteArray();
    }

    private static byte[] compressed(int type, byte[] nbt) throws IOException {
        if (type == 3) return nbt;
        var bytes = new ByteArrayOutputStream();
        try (var out = type == 1 ? new GZIPOutputStream(bytes) : new DeflaterOutputStream(bytes)) {
            out.write(nbt);
        }
        return bytes.toByteArray();
    }

    private Path region(int compression, int marker, boolean external) throws IOException {
        Path file = directory.resolve("r.0.0.mca");
        byte[] region = new byte[4096 * 3];
        ByteBuffer header = ByteBuffer.wrap(region);
        header.putInt(0, (2 << 8) | 1); // local (0,0), sector 2, one sector
        byte[] payload = compressed(compression, namedRoot(marker));
        header.putInt(8192, external ? 1 : 1 + payload.length);
        region[8196] = (byte) (compression | (external ? 0x80 : 0));
        if (!external) System.arraycopy(payload, 0, region, 8197, payload.length);
        Files.write(file, region);
        if (external) Files.write(directory.resolve("c.0.0.mcc"), payload);
        return file;
    }

    @Test void readsZlibAndLeavesExactlyOnePhysicalChunk() throws Exception {
        Path file = region(2, 42, false);
        List<AnvilRegionReader.Chunk> chunks = new ArrayList<>();
        AnvilRegionReader.scan(file, 1024, 1024, chunks::add);
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.getFirst().x());
        assertEquals(0, chunks.getFirst().z());
        assertEquals(42, chunks.getFirst().nbt().get("marker"));
    }

    @Test void supportsGzipUncompressedAndExternalSidecar() throws Exception {
        for (int compression : new int[]{1, 3}) {
            Path file = region(compression, compression, false);
            List<AnvilRegionReader.Chunk> chunks = new ArrayList<>();
            AnvilRegionReader.scan(file, 2048, 2048, chunks::add);
            assertEquals(compression, chunks.getFirst().nbt().get("marker"));
        }
        Path file = region(2, 99, true);
        List<AnvilRegionReader.Chunk> chunks = new ArrayList<>();
        AnvilRegionReader.scan(file, 2048, 2048, chunks::add);
        assertEquals(99, chunks.getFirst().nbt().get("marker"));
    }

    @Test void corruptOffsetAndOverlappingChunksFailClosed() throws Exception {
        Path file = region(2, 1, false);
        byte[] original = Files.readAllBytes(file);
        byte[] malformed = original.clone();
        ByteBuffer.wrap(malformed).putInt(0, (1 << 8) | 1); // header overlap
        Files.write(file, malformed);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 1024, 1024, c -> {}));
        malformed = original.clone();
        ByteBuffer.wrap(malformed).putInt(4, (2 << 8) | 1); // two header entries own one sector
        Files.write(file, malformed);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 1024, 1024, c -> {}));
    }

    @Test void corruptLengthsAndUnknownCompressionDoNotBecomeEmptyChunks() throws Exception {
        Path file = region(2, 1, false);
        byte[] valid = Files.readAllBytes(file);
        byte[] malformed = valid.clone();
        ByteBuffer.wrap(malformed).putInt(8192, 4096); // exceeds reserved sector
        Files.write(file, malformed);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 1024, 1024, c -> {}));
        malformed = valid.clone();
        malformed[8196] = 4; // unsupported LZ4 cannot be silently skipped
        Files.write(file, malformed);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 1024, 1024, c -> {}));
    }

    @Test void inflatedAndCompressedBudgetsAreEnforced() throws Exception {
        Path file = region(2, 3, false);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 2, 2048, c -> {}));
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 2048, 8, c -> {}));
    }

    @Test void missingExternalPayloadIsAReadFailure() throws Exception {
        Path file = region(2, 9, true);
        Files.delete(directory.resolve("c.0.0.mcc"));
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(file, 2048, 2048, c -> {}));
    }
    @Test void chunkCoordinatesMustMatchTheirAnvilHeaderSlot() throws Exception {
        // The header says (0,0), but the NBT claims (1,0). Publishing the
        // latter as the former could misattribute assets in a settlement.
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(3); out.writeUTF("xPos"); out.writeInt(1);
            out.writeByte(3); out.writeUTF("zPos"); out.writeInt(0);
            out.writeByte(0);
        }
        byte[] payload = compressed(2, nbt.toByteArray());
        byte[] bytes = new byte[4096 * 3];
        ByteBuffer.wrap(bytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(8192, 1 + payload.length);
        bytes[8196] = 2;
        System.arraycopy(payload, 0, bytes, 8197, payload.length);
        Path file = directory.resolve("r.0.0.mca");
        Files.write(file, bytes);
        assertThrows(IOException.class,
                () -> AnvilRegionReader.scan(file, 2048, 2048, c -> {}));
    }

    @Test void legacyLevelCoordinatesMustMatchTheirAnvilHeaderSlot() throws Exception {
        // Older chunk formats keep xPos/zPos inside the Level compound.
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(10); out.writeUTF("Level");
            out.writeByte(3); out.writeUTF("xPos"); out.writeInt(1);
            out.writeByte(3); out.writeUTF("zPos"); out.writeInt(0);
            out.writeByte(0); // end Level
            out.writeByte(0); // end root
        }
        byte[] payload = compressed(2, nbt.toByteArray());
        byte[] bytes = new byte[4096 * 3];
        ByteBuffer.wrap(bytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(8192, 1 + payload.length);
        bytes[8196] = 2;
        System.arraycopy(payload, 0, bytes, 8197, payload.length);
        Path file = directory.resolve("r.0.0.mca");
        Files.write(file, bytes);
        assertThrows(IOException.class,
                () -> AnvilRegionReader.scan(file, 2048, 2048, c -> {}));
    }

}
