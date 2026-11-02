package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackArchivePublicationTest {
    @TempDir Path directory;

    @Test void unsupportedAtomicMoveNeverFallsBackToNonAtomicReplacement() throws Exception {
        Path old = Files.write(directory.resolve("pack.zip"), new byte[]{1, 2, 3});
        Path candidate = Files.write(directory.resolve("candidate.zip"), new byte[]{4, 5, 6});
        AtomicInteger calls = new AtomicInteger();
        assertThrows(java.nio.file.AtomicMoveNotSupportedException.class,
                () -> PackArchivePublication.publish(candidate, old, (from, to, options) -> {
                    calls.incrementAndGet();
                    if (java.util.Arrays.asList(options).contains(StandardCopyOption.ATOMIC_MOVE))
                        throw new java.nio.file.AtomicMoveNotSupportedException(
                                from.toString(), to.toString(), "injected");
                    Files.move(from, to, options);
                }));
        assertEquals(1, calls.get());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(old));
        assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(candidate));
    }

    @Test void supportedAtomicReplacementPublishesExactCandidate() throws Exception {
        Path old = Files.write(directory.resolve("pack.zip"), new byte[]{1});
        Path candidate = Files.write(directory.resolve("candidate.zip"), new byte[]{4, 5, 6});
        PackArchivePublication.publish(candidate, old);
        assertArrayEquals(new byte[]{4, 5, 6}, Files.readAllBytes(old));
        assertFalse(Files.exists(candidate));
    }

    @Test void missingCandidateDoesNotMutateOldArchive() throws Exception {
        Path old = Files.write(directory.resolve("pack.zip"), new byte[]{1});
        assertThrows(IOException.class, () -> PackArchivePublication.publish(
                directory.resolve("missing.zip"), old));
        assertArrayEquals(new byte[]{1}, Files.readAllBytes(old));
    }
}
