package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Direct fixture for an otherwise regular source reached through a symlink parent. */
class SavedSourceReaderSymlinkAncestorTest {
    @TempDir Path temporary;

    @Test void anvilReaderMustRejectSymbolicDirectoryAncestor() throws Exception {
        Path real = temporary.resolve("actual-world");
        Path region = real.resolve("region/r.0.0.mca");
        Files.createDirectories(region.getParent());
        Files.write(region, new byte[8192]); // Valid empty Anvil header.
        Path alias = Files.createSymbolicLink(temporary.resolve("world-alias"), real);
        assertThrows(IOException.class, () -> AnvilRegionReader.scan(
                alias.resolve("region/r.0.0.mca"), 2048, 2048, ignored -> {}));
    }

    @Test void playerdataReaderMustRejectSymbolicDirectoryAncestor() throws Exception {
        Path real = temporary.resolve("actual-playerdata");
        Path uuidFile = real.resolve(UUID.fromString(
                "00000000-0000-0000-0000-000000000110") + ".dat");
        Files.createDirectories(real);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(raw)) {
            out.writeByte(10);
            out.writeUTF("");
            out.writeByte(0);
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
            gzip.write(raw.toByteArray());
        }
        Files.write(uuidFile, compressed.toByteArray());
        Path alias = Files.createSymbolicLink(temporary.resolve("playerdata-alias"), real);
        assertThrows(IOException.class, () -> PlayerDataReader.read(
                alias.resolve(uuidFile.getFileName()), 2048, 2048));
    }
}
