package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Validates the local PNG source boundary for generated Java/Bedrock packs.
 * The caller must still open input streams with NOFOLLOW_LINKS and create
 * generated defaults with CREATE_NEW to avoid following late symlink swaps.
 */
public final class PackImageSource {
    private PackImageSource() {}

    public static Path ensureDirectory(Path directory) throws IOException {
        if (directory == null)
            throw new IOException("Missing resource-pack image directory");
        Path root = directory.toAbsolutePath().normalize();
        requireNoSymbolicAncestors(root);
        Files.createDirectories(root);
        requireNoSymbolicAncestors(root);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Resource-pack image root is not a real directory");
        return root;
    }

    public static Path existingPng(Path directory, String filename) throws IOException {
        Path file = resolvePng(directory, filename);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Resource-pack image is not a real PNG file: " + file);
        return file;
    }

    public static Path newDefaultPng(Path directory, String filename) throws IOException {
        Path file = resolvePng(directory, filename);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Default resource-pack image already exists: " + file);
        return file;
    }

    private static Path resolvePng(Path directory, String filename) throws IOException {
        if (filename == null || !filename.endsWith(".png") || filename.length() <= 4 ||
                filename.contains("/") || filename.contains("\\") || filename.contains(".."))
            throw new IOException("Invalid resource-pack PNG basename");
        Path root = ensureDirectory(directory);
        Path file = root.resolve(filename).normalize();
        if (!root.equals(file.getParent()))
            throw new IOException("Resource-pack PNG escapes image directory");
        requireNoSymbolicAncestors(file);
        return file;
    }

    private static void requireNoSymbolicAncestors(Path path) throws IOException {
        for (Path part = path; part != null; part = part.getParent())
            if (Files.isSymbolicLink(part))
                throw new IOException("Resource-pack image path contains a symbolic link: " + part);
    }
}
