package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Locate exactly one saved Paper playerdata receipt without following links.
 * This only selects the candidate path; the caller must still read and verify
 * the saved NBT receipt after Paper saveData() has completed.
 */
public final class SavedPlayerReceiptLocator {
    private SavedPlayerReceiptLocator() {}

    public static Path findUnique(Collection<Path> worldRoots, UUID player) throws IOException {
        if (player == null || worldRoots == null || worldRoots.isEmpty())
            throw new IOException("Missing player UUID or world roots for saved receipt");
        Path found = null;
        Set<Path> visited = new HashSet<>();
        for (Path rawRoot : worldRoots) {
            if (rawRoot == null)
                throw new IOException("Null world root for saved receipt");
            Path root = rawRoot.toAbsolutePath().normalize();
            rejectSymbolicAncestors(root);
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Saved receipt world root is unavailable: " + root);
            if (!visited.add(root)) continue;
            for (String layout : new String[]{"players/data", "playerdata"}) {
                Path candidate = root.resolve(layout).resolve(player + ".dat");
                rejectSymbolicAncestors(candidate);
                if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) continue;
                if (!Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Saved receipt candidate is not a regular file: " + candidate);
                Path verified = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
                if (found != null)
                    throw new IOException("Ambiguous saved playerdata receipt for " + player);
                found = verified;
            }
        }
        if (found == null)
            throw new IOException("Cannot identify saved playerdata receipt for " + player);
        return found;
    }

    private static void rejectSymbolicAncestors(Path path) throws IOException {
        for (Path current = path; current != null; current = current.getParent())
            if (Files.isSymbolicLink(current))
                throw new IOException("Saved receipt path contains a symbolic link: " + current);
    }
}
