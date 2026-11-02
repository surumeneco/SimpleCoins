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

    private static void requireRealPublicationPath(Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        for (Path component : absolute) {
            if ("..".equals(component.toString()))
                throw new IOException("Generated Java pack path must not traverse parents");
        }
        for (Path part = absolute.normalize(); part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part))
                throw new IOException("Generated Java pack path contains a symbolic link: " + part);
        }
    }

    public static void publish(Path temporary, Path archive) throws IOException {
        publish(temporary, archive, (from, to, options) -> Files.move(from, to, options));
    }

    static void publish(Path temporary, Path archive, Move move) throws IOException {
        if (temporary == null || archive == null || move == null ||
                !Files.isRegularFile(temporary, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Generated Java pack candidate is missing or not a regular file");
        // Refuse redirection through a symlinked parent before any publication.
        // This is a best-effort check; it is not a filesystem transaction.
        requireRealPublicationPath(temporary);
        requireRealPublicationPath(archive);
        // An unsupported atomic move is a checked failure, not permission to
        // expose a partially replaced archive to concurrent HTTP readers.
        move.apply(temporary, archive, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }
}
