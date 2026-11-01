package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedPlayerReceiptLocatorTest {
    @TempDir Path temp;
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000017");

    private Path saved(Path root, String layout) throws Exception {
        Path folder = Files.createDirectories(root.resolve(layout));
        return Files.write(folder.resolve(PLAYER + ".dat"), new byte[]{1, 2, 3});
    }

    @Test void oneOrdinaryPlayerdataCandidateIsSelected() throws Exception {
        Path world = Files.createDirectories(temp.resolve("world"));
        Path candidate = saved(world, "playerdata");
        assertEquals(candidate.toRealPath(), SavedPlayerReceiptLocator.findUnique(
                List.of(world), PLAYER));
        Files.delete(candidate);
        Path legacy = saved(world, "players/data");
        assertEquals(legacy.toRealPath(), SavedPlayerReceiptLocator.findUnique(
                List.of(world), PLAYER));
    }

    @Test void missingOrDuplicateCandidatesFailClosed() throws Exception {
        Path first = Files.createDirectories(temp.resolve("world"));
        Path second = Files.createDirectories(temp.resolve("world_nether"));
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(first, second), PLAYER));
        saved(first, "playerdata");
        saved(second, "players/data");
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(first, second), PLAYER));
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(first), null));
    }

    @Test void symlinkedReceiptFileAndDirectoryFailClosed() throws Exception {
        Path world = Files.createDirectories(temp.resolve("world"));
        Path outside = saved(temp.resolve("outside"), "playerdata");
        Path folder = Files.createDirectories(world.resolve("playerdata"));
        Files.createSymbolicLink(folder.resolve(PLAYER + ".dat"), outside.toAbsolutePath());
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(world), PLAYER));

        Files.delete(folder.resolve(PLAYER + ".dat"));
        Files.delete(folder);
        Files.createSymbolicLink(world.resolve("playerdata"),
                outside.getParent().toAbsolutePath());
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(world), PLAYER));
    }

    @Test void nonRegularReceiptCandidateFailsClosed() throws Exception {
        Path world = Files.createDirectories(temp.resolve("world"));
        Path folder = Files.createDirectories(world.resolve("playerdata"));
        Files.createDirectory(folder.resolve(PLAYER + ".dat"));
        assertThrows(IOException.class, () -> SavedPlayerReceiptLocator.findUnique(
                List.of(world), PLAYER));
    }
}
