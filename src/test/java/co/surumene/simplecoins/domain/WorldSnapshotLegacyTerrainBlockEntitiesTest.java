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

/**
 * TASK-20261101-003: Draft SC-LEGACY-BLOCK-01..06.
 * Test preparation only: the normative CAT Process/PI/TCE and machine gates
 * are not Confirmed. These negative cases are expected Red until the guard
 * is authorized and implemented. No partial asset total may be published.
 */
class WorldSnapshotLegacyTerrainBlockEntitiesTest {
    @TempDir Path directory;

    private enum Layout {
        ROOT_NONEMPTY, LEVEL_NONEMPTY, ROOT_WRONG_TYPE, LEVEL_WRONG_TYPE,
        LEVEL_NOT_COMPOUND, ROOT_EMPTY, LEVEL_EMPTY, ROOT_EMPTY_LEVEL_NONEMPTY, ROOT_NONEMPTY_LEVEL_EMPTY, ABSENT
    }

    private static void tileEntities(DataOutputStream out, boolean occupied)
            throws IOException {
        out.writeByte(9); // TAG_List
        out.writeUTF("TileEntities");
        out.writeByte(10); // list of TAG_Compound
        out.writeInt(occupied ? 1 : 0);
        if (occupied) {
            out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:chest");
            out.writeByte(0); // end block entity compound
        }
    }

    private Path terrain(Layout layout) throws IOException {
        var nbt = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(nbt)) {
            out.writeByte(10); out.writeUTF(""); // root TAG_Compound
            switch (layout) {
                case ROOT_NONEMPTY -> tileEntities(out, true);
                case ROOT_WRONG_TYPE -> {
                    out.writeByte(3); out.writeUTF("TileEntities"); out.writeInt(1);
                }
                case ROOT_EMPTY -> tileEntities(out, false);
                case ROOT_EMPTY_LEVEL_NONEMPTY, ROOT_NONEMPTY_LEVEL_EMPTY -> {
                    tileEntities(out, layout == Layout.ROOT_NONEMPTY_LEVEL_EMPTY);
                    out.writeByte(10); out.writeUTF("Level");
                    tileEntities(out, layout == Layout.ROOT_EMPTY_LEVEL_NONEMPTY);
                    out.writeByte(0);
                }
                case LEVEL_NONEMPTY, LEVEL_WRONG_TYPE, LEVEL_EMPTY -> {
                    out.writeByte(10); out.writeUTF("Level"); // nested compound
                    if (layout == Layout.LEVEL_WRONG_TYPE) {
                        out.writeByte(3); out.writeUTF("TileEntities"); out.writeInt(1);
                    } else {
                        tileEntities(out, layout == Layout.LEVEL_NONEMPTY);
                    }
                    out.writeByte(0); // end Level
                }
                case LEVEL_NOT_COMPOUND -> {
                    out.writeByte(3); out.writeUTF("Level"); out.writeInt(1);
                }
                case ABSENT -> { }
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

    @Test void nonemptyRootLegacyBlockEntitiesMustAbortBeforeVisitor() throws Exception {
        assertRejected(Layout.ROOT_NONEMPTY);
    }

    @Test void nonemptyLevelLegacyBlockEntitiesMustAbortBeforeVisitor() throws Exception {
        assertRejected(Layout.LEVEL_NONEMPTY);
    }

    @Test void nonListRootLegacyBlockEntitiesMustAbortBeforeVisitor() throws Exception {
        assertRejected(Layout.ROOT_WRONG_TYPE);
    }

    @Test void nonListLevelLegacyBlockEntitiesMustAbortBeforeVisitor() throws Exception {
        assertRejected(Layout.LEVEL_WRONG_TYPE);
    }

    @Test void nonCompoundLevelCannotBeTreatedAsAbsent() throws Exception {
        assertRejected(Layout.LEVEL_NOT_COMPOUND);
    }

    @Test void emptyRootCannotMaskNonemptyLevelList() throws Exception {
        assertRejected(Layout.ROOT_EMPTY_LEVEL_NONEMPTY);
    }

    @Test void nonemptyRootCannotBeMaskedByEmptyLevelList() throws Exception {
        assertRejected(Layout.ROOT_NONEMPTY_LEVEL_EMPTY);
    }

    private void assertRejected(Layout layout) throws Exception {
        Path path = terrain(layout);
        var calls = new AtomicInteger();
        assertThrows(IOException.class, () -> WorldSnapshotTraversal.traverse(
                manifest(path), 4096, 4096, visitor(calls)));
        assertEquals(0, calls.get(), "No unaccounted source may reach visitor");
    }

    @Test void absentAndEmptyLegacyListsRemainTraversable() throws Exception {
        for (Layout layout : new Layout[]{Layout.ROOT_EMPTY, Layout.LEVEL_EMPTY, Layout.ABSENT}) {
            Path path = terrain(layout);
            var calls = new AtomicInteger();
            assertEquals(new WorldSnapshotTraversal.Report(1, 1, 0),
                    WorldSnapshotTraversal.traverse(
                            manifest(path), 4096, 4096, visitor(calls)));
            assertEquals(1, calls.get(), "Empty legacy tags must not suppress modern traversal");
        }
    }
}
