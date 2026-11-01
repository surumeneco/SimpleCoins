package co.surumene.simplecoins.domain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Owns only explicitly recorded pack files. Changed or unrelated files are never overwritten. */
public final class ManagedResources {
    private final Path root;
    public ManagedResources(Path root) { this.root = root.toAbsolutePath().normalize(); }

    public void replace(Map<String, byte[]> desired) throws IOException {
        Files.createDirectories(root);
        Path manifest = root.resolve("simplecoins-managed-resources");
        Map<String,String> previous = new TreeMap<>();
        if (Files.exists(manifest)) {
            for (String line : new String(DurableFile.read(manifest, 16384), StandardCharsets.UTF_8).split("\n")) {
                if (line.isBlank()) continue;
                String[] fields = line.split(" ", 2);
                if (fields.length != 2 || !fields[0].matches("[a-f0-9]{64}") || previous.put(fields[1], fields[0]) != null)
                    throw new IOException("Invalid managed resource manifest");
                target(fields[1]);
            }
        }
        // Validate the complete old/new set before any mutation.
        Set<String> paths = new TreeSet<>(previous.keySet());
        paths.addAll(desired.keySet());
        for (String path : paths) {
            Path file = target(path);
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !previous.containsKey(path)
                        || !previous.get(path).equals(digest(Files.readAllBytes(file))))
                    throw new IOException("Unowned or modified Geyser resource: " + file);
            } else if (previous.containsKey(path)) {
                throw new IOException("Managed Geyser resource is missing: " + file);
            }
        }
        for (String path : paths) {
            Path file = target(path);
            if (!desired.containsKey(path)) Files.delete(file);
            else {
                Files.createDirectories(file.getParent());
                Path temp = Files.createTempFile(file.getParent(), "simplecoins-", ".tmp");
                try {
                    Files.write(temp, desired.get(path));
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
        }
        StringBuilder contents = new StringBuilder();
        for (String path : new TreeSet<>(desired.keySet())) contents.append(digest(desired.get(path))).append(' ').append(path).append('\n');
        DurableFile.write(manifest, contents.toString().getBytes(StandardCharsets.UTF_8));
        // An interrupted multi-file update is detected on next startup and fails closed.
    }

    private Path target(String name) throws IOException {
        if (!name.matches("(?:packs/simplecoins-generated\\.mcpack|custom_mappings/simplecoins-generated\\.json|packs/simplecoins\\.mcpack|custom_mappings/simplecoins\\.json)"))
            throw new IOException("Not a managed resource path: " + name);
        Path target = root.resolve(name);
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(target.getParent()) || Files.isSymbolicLink(target))
            throw new IOException("Managed resource cannot be a symbolic link");
        return target;
    }

    private static String digest(byte[] content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
