package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-006: generated pack image sources stay in their real directory. */
class PackImageSourceTest {
    @TempDir Path directory;

    @Test void realPngAndSafeDefaultDestinationRemainUsable() throws Exception {
        Path images = PackImageSource.ensureDirectory(directory.resolve("images"));
        Path existing = Files.write(images.resolve("100 Coin.png"), new byte[]{1, 2, 3});
        assertEquals(existing, PackImageSource.existingPng(images, "100 Coin.png"));
        assertEquals(images.resolve("500 Coin.png"),
                PackImageSource.newDefaultPng(images, "500 Coin.png"));
        assertFalse(Files.exists(images.resolve("500 Coin.png")));
    }

    @Test void linkedImageDirectoryCannotRedirectPackBuild() throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path linked = Files.createSymbolicLink(directory.resolve("images"), outside);
        assertThrows(IOException.class, () -> PackImageSource.ensureDirectory(linked));
        assertThrows(IOException.class, () -> PackImageSource.existingPng(linked, "100 Coin.png"));
    }

    @Test void selectedPngSymbolicLinkCannotExposeExternalFile() throws Exception {
        Path images = PackImageSource.ensureDirectory(directory.resolve("images"));
        Path secret = Files.writeString(directory.resolve("outside.txt"), "private");
        Files.createSymbolicLink(images.resolve("100 Coin.png"), secret);
        assertThrows(IOException.class, () -> PackImageSource.existingPng(images, "100 Coin.png"));
    }

    @Test void linkedDefaultDestinationIsNotOverwritten() throws Exception {
        Path images = PackImageSource.ensureDirectory(directory.resolve("images"));
        Path outside = Files.writeString(directory.resolve("outside.txt"), "private");
        Files.createSymbolicLink(images.resolve("100 Coin.png"), outside);
        assertThrows(IOException.class, () -> PackImageSource.newDefaultPng(images, "100 Coin.png"));
        assertEquals("private", Files.readString(outside));
    }

    @Test void invalidPngNamesCannotEscapeImageRoot() throws Exception {
        Path images = PackImageSource.ensureDirectory(directory.resolve("images"));
        for (String name : java.util.List.of("../escape.png", "sub/coin.png",
                "sub\\coin.png", "coin..png", ".png", "coin.txt")) {
            assertThrows(IOException.class, () -> PackImageSource.newDefaultPng(images, name));
        }
    }
}
