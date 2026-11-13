package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Discover-time source closure must be checked again after saved NBT callbacks. */
class SavedManifestDiscoveryClosureTest {
    @TempDir Path directory;
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000003001");
    private static final UUID NEXT = UUID.fromString("00000000-0000-0000-0000-000000003002");

    private static byte[] emptyRoot() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeByte(10); out.writeUTF(""); out.writeByte(0);
        }
        return bytes.toByteArray();
    }

    private Path world() throws IOException {
        var root = directory.resolve("world");
        Files.createDirectories(root);
        return root;
    }

    private Path player(UUID id) throws IOException {
        Path path = world().resolve("playerdata/" + id + ".dat");
        Files.createDirectories(path.getParent());
        var zipped = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(zipped)) {
            gzip.write(emptyRoot());
        }
        Files.write(path, zipped.toByteArray());
        return path;
    }

    private WorldStorageDiscovery.Manifest discovered() throws IOException {
        return WorldStorageDiscovery.discover(
                Map.of("world", world()), new WorldScope(true, Set.of(), Set.of()), 100);
    }

    private WorldSnapshotTraversal.Report visit(WorldStorageDiscovery.Manifest manifest,
                                                java.util.function.Consumer<UUID> duringPlayer)
            throws IOException {
        return WorldSnapshotTraversal.traverse(manifest, 65536, 65536,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk chunk) {}
                    @Override public void player(UUID id, Map<String, Object> data)
                            throws IOException {
                        duringPlayer.accept(id);
                    }
                });
    }

    @Test void unchangedDiscoveredSourceSetCompletes() throws Exception {
        player(FIRST);
        var manifest = discovered();
        assertEquals(1, visit(manifest, id -> {}).playerFiles());
    }

    @Test void newlyCreatedOfflinePlayerdataCannotBeSilentlyOmitted() throws Exception {
        player(FIRST);
        var manifest = discovered();
        assertThrows(IOException.class, () -> visit(manifest, id -> {
            try { player(NEXT); } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }));
    }

    @Test void newlyCreatedRegionFileCannotBeSilentlyOmitted() throws Exception {
        player(FIRST);
        var manifest = discovered();
        assertThrows(IOException.class, () -> visit(manifest, id -> {
            try {
                Path file = world().resolve("region/r.1.0.mca");
                Files.createDirectories(file.getParent());
                Files.write(file, new byte[8192]);
            } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }));
    }

    @Test void newlyCreatedEntityRegionFileCannotBeSilentlyOmitted() throws Exception {
        player(FIRST);
        var manifest = discovered();
        assertThrows(IOException.class, () -> visit(manifest, id -> {
            try {
                Path file = world().resolve("entities/r.-1.0.mca");
                Files.createDirectories(file.getParent());
                Files.write(file, new byte[8192]);
            } catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }));
    }

    @Test void manuallyConstructedFixturesRemainUnprovenButUsableForIsolatedReads()
            throws Exception {
        Path existing = player(FIRST);
        var manual = new WorldStorageDiscovery.Manifest(List.of(), Map.of(FIRST, existing));
        assertEquals(1, visit(manual, id -> {}).playerFiles());
        assertFalse(manual.hasSourceClosureProvenance());
        assertTrue(discovered().hasSourceClosureProvenance());
    }
}
