package co.surumene.simplecoins.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Read-only, budgeted Anvil chunk reader. A caller must first obtain a stable
 * world snapshot; reading live files is NOT a consistent economic settlement.
 * The callback must stage results and publish only after ALL sources succeed.
 */
public final class AnvilRegionReader {
    private AnvilRegionReader() {}

    private static final int SECTOR = 4096;
    private static final int HEADER = 8192;
    private static final int SLOTS = 1024;

    public record Chunk(int x, int z, Map<String, Object> nbt) {}

    @FunctionalInterface
    public interface ChunkConsumer {
        void accept(Chunk chunk) throws IOException;
    }

    private record Entry(int slot, int offset, int count) {}

    /** Unknown compression IDs, truncated/corrupt data or exhausted budgets abort the entire scan. */
    public static void scan(Path regionFile, int maxCompressedBytes, int maxInflatedBytes,
                            ChunkConsumer callback) throws IOException {
        Objects.requireNonNull(regionFile);
        Objects.requireNonNull(callback);
        if (maxCompressedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Chunk budgets must be positive");

        String[] parts = regionFile.getFileName().toString().split("\\.");
        if (parts.length != 4 || !"r".equals(parts[0]) || !"mca".equals(parts[3]))
            throw new IOException("Invalid Anvil region filename: " + regionFile);
        int rx, rz;
        try {
            rx = Integer.parseInt(parts[1]);
            rz = Integer.parseInt(parts[2]);
        } catch (NumberFormatException failure) {
            throw new IOException("Invalid Anvil region coordinates: " + regionFile, failure);
        }

        if (!Files.isRegularFile(regionFile, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Region file missing or symbolic: " + regionFile);
        try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size < HEADER || size % SECTOR != 0)
                throw new IOException("Incomplete or unaligned region file: " + regionFile);
            ByteBuffer header = read(channel, 0L, SECTOR);
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < SLOTS; i++) {
                int address = header.getInt(i * 4);
                int offset = address >>> 8, count = address & 0xff;
                if (offset == 0 && count == 0) continue;
                if (offset < 2 || count < 1 || ((long) offset + count) * SECTOR > size)
                    throw new IOException("Invalid Anvil sector allocation for chunk slot " + i);
                for (Entry previous : entries) {
                    if (offset < (long) previous.offset + previous.count &&
                            previous.offset < (long) offset + count)
                        throw new IOException("Overlapping Anvil sector allocations: " + i + ", " + previous.slot);
                }
                entries.add(new Entry(i, offset, count));
            }

            // Validate the complete allocation table before delivering any chunk.
            for (Entry entry : entries) {
                long position = (long) entry.offset * SECTOR;
                ByteBuffer prefix = read(channel, position, 5);
                int length = prefix.getInt();
                int flag = Byte.toUnsignedInt(prefix.get());
                boolean external = (flag & 0x80) != 0;
                int compression = flag & 0x7f;
                if (length < 1 || length > entry.count * SECTOR - 4)
                    throw new IOException("Invalid compressed chunk length at slot " + entry.slot);
                if (external && length != 1)
                    throw new IOException("External chunk has an invalid stub at slot " + entry.slot);

                int localX = entry.slot % 32, localZ = entry.slot / 32;
                int x, z;
                try {
                    x = Math.addExact(Math.multiplyExact(rx, 32), localX);
                    z = Math.addExact(Math.multiplyExact(rz, 32), localZ);
                } catch (ArithmeticException overflow) {
                    throw new IOException("Anvil chunk coordinates overflow", overflow);
                }

                byte[] payload;
                if (external) {
                    Path sidecar = regionFile.resolveSibling("c." + x + "." + z + ".mcc");
                    if (!Files.isRegularFile(sidecar, LinkOption.NOFOLLOW_LINKS))
                        throw new IOException("Missing or symbolic external Anvil chunk: " + sidecar);
                    long sidecarSize = Files.size(sidecar);
                    if (sidecarSize < 1 || sidecarSize > maxCompressedBytes)
                        throw new IOException("External chunk exceeds input budget: " + sidecar);
                    payload = Files.readAllBytes(sidecar);
                } else {
                    int payloadSize = length - 1;
                    if (payloadSize < 1 || payloadSize > maxCompressedBytes)
                        throw new IOException("Anvil chunk exceeds input budget at slot " + entry.slot);
                    payload = read(channel, position + 5, payloadSize).array();
                }

                try (InputStream stream = decompressor(compression, payload)) {
                    Map<String, Object> nbt = NbtReader.read(stream, maxInflatedBytes);
                    // The Anvil header locates the physical chunk. If its NBT
                    // declares coordinates, they must describe that same chunk;
                    // otherwise settlement could attribute assets to a wrong slot.
                    Object declaredX = nbt.get("xPos");
                    Object declaredZ = nbt.get("zPos");
                    if (declaredX != null || declaredZ != null) {
                        if (!(declaredX instanceof Integer nbtX) ||
                                !(declaredZ instanceof Integer nbtZ) ||
                                nbtX != x || nbtZ != z)
                            throw new IOException("Chunk NBT coordinates disagree with Anvil header: " +
                                    regionFile + " at " + x + "," + z);
                    }
                    // Legacy Anvil chunks place the same coordinates inside
                    // Level. A mismatch is just as unsafe as a modern root
                    // mismatch and must not reach the settlement visitor.
                    Object rawLevel = nbt.get("Level");
                    if (rawLevel != null) {
                        if (!(rawLevel instanceof Map<?, ?> level))
                            throw new IOException("Invalid legacy chunk Level compound: " + regionFile);
                        Object levelX = level.get("xPos");
                        Object levelZ = level.get("zPos");
                        if (levelX != null || levelZ != null) {
                            if (!(levelX instanceof Integer nbtX) ||
                                    !(levelZ instanceof Integer nbtZ) ||
                                    nbtX != x || nbtZ != z)
                                throw new IOException("Legacy chunk coordinates disagree with Anvil header: " +
                                        regionFile + " at " + x + "," + z);
                        }
                    }
                    callback.accept(new Chunk(x, z, nbt));
                }
            }
        }
    }

    private static InputStream decompressor(int compression, byte[] bytes) throws IOException {
        InputStream input = new ByteArrayInputStream(bytes);
        return switch (compression) {
            case 1 -> new GZIPInputStream(input);
            case 2 -> new InflaterInputStream(input);
            case 3 -> input;
            default -> throw new IOException("Unsupported Anvil compression method: " + compression);
        };
    }

    private static ByteBuffer read(FileChannel channel, long offset, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            int count = channel.read(buffer, offset + buffer.position());
            if (count <= 0) throw new IOException("Truncated Anvil region payload");
        }
        buffer.flip();
        return buffer;
    }
}
