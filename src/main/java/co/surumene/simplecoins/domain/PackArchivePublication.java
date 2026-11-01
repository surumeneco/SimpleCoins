package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomically publishes the generated Java pack archive. */
public final class PackArchivePublication {
    private PackArchivePublication() {}

    @FunctionalInterface
    interface Move {
        void apply(Path from, Path to, java.nio.file.CopyOption... options) throws IOException;
    }

    public static void publish(Path temporary, Path archive) throws IOException {
        publish(temporary, archive, (from, to, options) -> Files.move(from, to, options));
    }

    static void publish(Path temporary, Path archive, Move move) throws IOException {
        if (temporary == null || archive == null || move == null ||
                !Files.isRegularFile(temporary, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Generated Java pack candidate is missing or not a regular file");
        try {
            move.apply(temporary, archive, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            move.apply(temporary, archive, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
