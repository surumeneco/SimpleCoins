package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerDataReaderTest {
    @TempDir Path directory;

    private Path playerData(int amount) throws IOException {
        Path file = directory.resolve(UUID.randomUUID() + ".dat");
        var raw = new ByteArrayOutputStream();
        try (var output = new DataOutputStream(raw)) {
            output.writeByte(10);
            output.writeUTF("");
            output.writeByte(9);
            output.writeUTF("Inventory");
            output.writeByte(10);
            output.writeInt(1);
            output.writeByte(3);
            output.writeUTF("testValue");
            output.writeInt(amount);
            output.writeByte(0);
            output.writeByte(0);
        }
        var compressed = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(compressed)) {
            gzip.write(raw.toByteArray());
        }
        Files.write(file, compressed.toByteArray());
        return file;
    }

    @Test void readsGzipPlayerInventoryWithoutDroppingItsContents() throws Exception {
        Path file = playerData(400);
        Map<String,Object> root = PlayerDataReader.read(file, 1024, 1024);
        List<?> inventory = (List<?>) root.get("Inventory");
        assertEquals(400, ((Map<?, ?>) inventory.getFirst()).get("testValue"));
    }

    @Test void missingOrTruncatedPlayerdataIsNotAnEmptyInventory() throws Exception {
        Path file = playerData(99);
        assertThrows(IOException.class, () -> PlayerDataReader.read(directory.resolve(UUID.randomUUID() + ".dat"), 1024, 1024));
        Files.write(file, java.util.Arrays.copyOf(Files.readAllBytes(file), 12));
        assertThrows(IOException.class, () -> PlayerDataReader.read(file, 1024, 1024));
    }

    @Test void enforcesIndependentCompressedAndInflatedBudgets() throws Exception {
        Path file = playerData(33);
        assertThrows(IOException.class, () -> PlayerDataReader.read(file, 2, 1024));
        assertThrows(IOException.class, () -> PlayerDataReader.read(file, 1024, 8));
    }

    @Test void rejectsNonUuidAndSymlinkFiles() throws Exception {
        Path file = playerData(11);
        Path bad = directory.resolve("not-a-uuid.dat");
        Files.copy(file, bad);
        assertThrows(IOException.class, () -> PlayerDataReader.read(bad, 1024, 1024));
        Path symlink = directory.resolve(UUID.randomUUID() + ".dat");
        try {
            Files.createSymbolicLink(symlink, file.toAbsolutePath());
            assertThrows(IOException.class, () -> PlayerDataReader.read(symlink, 1024, 1024));
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            // Some CI filesystems do not permit symbolic links.
        }
    }
}
