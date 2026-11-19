package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Genuine Paper 26.2 dimension data/paper/metadata.dat identity witness. */
class PaperSavedWorldMetadataWitnessTest {
    @TempDir Path temp;
    private static final WorldScope SCOPE =
            new WorldScope(true, Set.of(), Set.of());
    private static final UUID A = UUID.fromString("00000000-0000-4000-8000-0000000000a5");
    private static final UUID B = UUID.fromString("00000000-0000-4000-8000-0000000000b6");

    private Path root(String world) throws IOException {
        Path dir = temp.resolve(world);
        Files.createDirectories(dir.resolve("data/paper"));
        return dir;
    }

    private byte[] metadata(UUID id) throws IOException {
        var clear = new ByteArrayOutputStream();
        try (var nbt = new DataOutputStream(clear)) {
            nbt.writeByte(10); nbt.writeUTF("");
            nbt.writeByte(10); nbt.writeUTF("data");
            nbt.writeByte(11); nbt.writeUTF("uuid");
            nbt.writeInt(4);
            nbt.writeInt((int) (id.getMostSignificantBits() >>> 32));
            nbt.writeInt((int) id.getMostSignificantBits());
            nbt.writeInt((int) (id.getLeastSignificantBits() >>> 32));
            nbt.writeInt((int) id.getLeastSignificantBits());
            nbt.writeByte(0); // data
            nbt.writeByte(0); // NBT root
        }
        var encoded = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(encoded)) {
            gzip.write(clear.toByteArray());
        }
        return encoded.toByteArray();
    }

    private Path writeMetadata(Path dir, UUID id) throws IOException {
        Path file = dir.resolve("data/paper/metadata.dat");
        Files.write(file, metadata(id));
        return file;
    }

    private WorldStorageDiscovery.Manifest discover(Map<String, Path> dirs)
            throws IOException {
        return WorldStorageDiscovery.discover(dirs, SCOPE, 4096);
    }

    @Test void readsPaperMetadataUuidFromAllDiscoveredWorldsEvenWhenNoMcaExists()
            throws Exception {
        Path w1 = root("w1"), w2 = root("w2");
        writeMetadata(w1, A);
        writeMetadata(w2, B);
        var witness = PaperSavedWorldMetadataWitness.capture(
                discover(Map.of("w1", w1, "w2", w2)), Map.of("w1", A, "w2", B));
        assertEquals(Map.of("w1", A, "w2", B), witness.worldIds());
        witness.requireUnchanged();
    }

    @Test void rejectsClaimedUuidOrWorldSetThatDisagreesWithRealDisk() throws Exception {
        Path w = root("w");
        writeMetadata(w, A);
        var manifest = discover(Map.of("w", w));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of("w", B)));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of("other", A)));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of()));
    }

    @Test void refusesDuplicateIdsInTwoIndependentWorldRoots() throws Exception {
        Path a = root("w1"), b = root("w2");
        writeMetadata(a, A);
        writeMetadata(b, A);
        var manifest = discover(Map.of("w1", a, "w2", b));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(
                        manifest, Map.of("w1", A, "w2", A)));
    }

    @Test void noOriginMissingOrMalformedMetadataNeverAuthorizesSavedWorldUuid()
            throws Exception {
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(
                        new WorldStorageDiscovery.Manifest(java.util.List.of(), Map.of()),
                        Map.of("w", A)));
        Path world = root("w");
        var manifest = discover(Map.of("w", world));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of("w", A)));
        Path file = writeMetadata(world, A);
        Files.write(file, new byte[]{1, 2, 3});
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of("w", A)));
    }

    @Test void metadataBytesChangedWithSameSizeAndMtimeInvalidateWitness()
            throws Exception {
        Path dir = root("w");
        Path file = writeMetadata(dir, A);
        var manifest = discover(Map.of("w", dir));
        var witness = PaperSavedWorldMetadataWitness.capture(manifest, Map.of("w", A));
        var before = Files.getLastModifiedTime(file);
        var replacement = metadata(B);
        // If compressed lengths differ, the essential invariant is still
        // that a source-byte mutation cannot pass as the same identity.
        Files.write(file, replacement);
        Files.setLastModifiedTime(file, before);
        assertThrows(IOException.class, witness::requireUnchanged);
    }

    @Test void symlinkedMetadataCannotBeUsedAsAuthoritativeWorldUuid() throws Exception {
        Path dir = root("w");
        Path external = temp.resolve("outside-metadata.dat");
        Files.write(external, metadata(A));
        Files.createSymbolicLink(dir.resolve("data/paper/metadata.dat"), external);
        var manifest = discover(Map.of("w", dir));
        assertThrows(IOException.class, () ->
                PaperSavedWorldMetadataWitness.capture(manifest, Map.of("w", A)));
    }
}
