package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-006: reject symlinked ancestors before publishing a pack. */
class PackArchivePublicationAncestorTest {
    @TempDir Path directory;

    @Test void sourceAncestorLinkCannotRedirectPackPublication() throws Exception {
        Path realSource = Files.createDirectory(directory.resolve("real-source"));
        Path candidate = Files.write(realSource.resolve("candidate.zip"), new byte[]{4, 5, 6});
        Path alias = Files.createSymbolicLink(directory.resolve("source-alias"), realSource);
        Path old = Files.write(directory.resolve("pack.zip"), new byte[]{1, 2, 3});
        AtomicInteger calls = new AtomicInteger();

        assertThrows(IOException.class, () -> PackArchivePublication.publish(
                alias.resolve("candidate.zip"), old, (from, to, options) -> {
                    calls.incrementAndGet();
                    Files.move(from, to, options);
                }));
        assertEquals(0, calls.get(), "Reject before invoking any move");
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(old));
        assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(candidate));
    }

    @Test void destinationAncestorLinkCannotRedirectPackPublication() throws Exception {
        Path realDestination = Files.createDirectory(directory.resolve("real-output"));
        Path alias = Files.createSymbolicLink(directory.resolve("output-alias"), realDestination);
        Path candidate = Files.write(directory.resolve("candidate.zip"), new byte[]{4, 5, 6});
        AtomicInteger calls = new AtomicInteger();

        assertThrows(IOException.class, () -> PackArchivePublication.publish(
                candidate, alias.resolve("pack.zip"), (from, to, options) -> {
                    calls.incrementAndGet();
                    Files.move(from, to, options);
                }));
        assertEquals(0, calls.get(), "Reject before invoking any move");
        assertTrue(Files.exists(candidate));
        assertFalse(Files.exists(realDestination.resolve("pack.zip")));
    }
}
