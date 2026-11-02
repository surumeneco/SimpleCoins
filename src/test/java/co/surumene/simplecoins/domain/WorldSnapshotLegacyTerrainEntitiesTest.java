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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-003: legacy terrain Entities must never disappear from accounting. */
class WorldSnapshotLegacyTerrainEntitiesTest {
    @TempDir Path directory;

    private enum Layout { ROOT, LEVEL, MALFORMED }

    private static void entityList(DataOutputStream out, boolean occupied) throws IOException {
        out.writeByte(9);
        out.writeUTF("Entities");
        out.writeByte(10); // list of compounds
        out.writeInt(occupied ? 1 : 0);
        if (occupied) {
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chest_minecart");
            out.writeByte(11); out.writeUTF("UUID");
            out.writeInt(4);
            out.writeInt(0); out.writeInt(0); out.writeInt(0); out.writeInt(1);
            out.writeByte(0); // end Entity
        }
    }

    private Path terrain(Layout layout, boolean occupied) throws IOException {
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF(""); // root compound
            if (layout == Layout.LEVEL) {
                out.writeByte(10); out.writeUTF("Level");
                entityList(out, occupied);
                out.writeByte(0); // end Level
            } else if (layout == Layout.MALFORMED) {
                out.writeByte(3); out.writeUTF("Entities"); out.writeInt(1);
            } else {
                entityList(out, occupied);
            }
            out.writeByte(0); // end root
        }
        var compressed = new ByteArrayOutputStream();
        try (var deflate = new DeflaterOutputStream(compressed)) {
            deflate.write(nbt.toByteArray());
        }
        byte[] payload = compressed.toByteArray();
        byte[] bytes = new byte[3 * 4096];
        ByteBuffer.wrap(bytes).putInt(0, (2 << 8) | 1);
        ByteBuffer.wrap(bytes).putInt(8192, payload.length + 1);
        bytes[8196] = 2;
        System.arraycopy(payload, 0, bytes, 8197, payload.length);
        Path path = directory.resolve("region/r.0.0.mca");
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
        return path;
    }

    private static WorldStorageDiscovery.Manifest manifest(Path path) {
        return new WorldStorageDiscovery.Manifest(List.of(
                new WorldStorageDiscovery.SourceFile(
                        "world", path, WorldStorageDiscovery.Kind.REGION)), Map.of());
    }

    private static WorldSnapshotTraversal.Visitor visitor(AtomicInteger calls) {
        return new WorldSnapshotTraversal.Visitor() {
            @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                        AnvilRegionReader.Chunk chunk) {
                calls.incrementAndGet();
            }
            @Override public void player(UUID player, Map<String, Object> data) {
                calls.incrementAndGet();
            }
        };
    }

    @Test void rootLevelLegacyEntitiesCannotBeSilentlySkipped() throws Exception {
        Path path = terrain(Layout.ROOT, true);
        var calls = new AtomicInteger();
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(path), 4096, 4096, visitor(calls)));
        assertEquals(0, calls.get());
    }

    @Test void levelEmbeddedLegacyEntitiesCannotBeSilentlySkipped() throws Exception {
        Path path = terrain(Layout.LEVEL, true);
        var calls = new AtomicInteger();
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(path), 4096, 4096, visitor(calls)));
        assertEquals(0, calls.get());
    }

    @Test void malformedLegacyEntityListMustFailClosed() throws Exception {
        Path path = terrain(Layout.MALFORMED, false);
        var calls = new AtomicInteger();
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(path), 4096, 4096, visitor(calls)));
        assertEquals(0, calls.get());
    }

    @Test void emptyLegacyEntityListsRemainTraversable() throws Exception {
        for (Layout layout : new Layout[]{Layout.ROOT, Layout.LEVEL}) {
            Path path = terrain(layout, false);
            var calls = new AtomicInteger();
            assertEquals(new WorldSnapshotTraversal.Report(1, 1, 0),
                    WorldSnapshotTraversal.traverse(
                            manifest(path), 4096, 4096, visitor(calls)));
            assertEquals(1, calls.get());
        }
    }
}
