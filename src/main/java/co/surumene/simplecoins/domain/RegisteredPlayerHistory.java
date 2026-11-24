package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Historical registration only. Never persists or restores monetary balances.
 * Eligibility is independently derived from the current decision and playerdata. */
public final class RegisteredPlayerHistory {
    private final Path path;
    private Set<UUID> known;

    public RegisteredPlayerHistory(Path path) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        var ids = new HashSet<UUID>();
        if (Files.exists(this.path, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(this.path, LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Invalid registered-player history file");
            try (var lines = Files.lines(this.path, StandardCharsets.UTF_8)) {
                var iterator = lines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    UUID id = UUID.fromString(line);
                    if (!id.toString().equals(line) || !ids.add(id))
                        throw new IOException("Invalid or duplicate registered-player UUID");
                }
            } catch (IllegalArgumentException | java.io.UncheckedIOException failure) {
                throw new IOException("Malformed registered-player history", failure);
            }
        } else if (!Files.notExists(this.path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Registered-player history could not be inspected");
        }
        known = Set.copyOf(ids);
    }

    public synchronized Set<UUID> snapshot() { return known; }

    public Set<UUID> eligible(Set<UUID> currentPlayerdata, BanSourceCoverage bans) throws IOException {
        Set<UUID> eligible = new HashSet<>();
        for (UUID id : snapshot()) {
            if (currentPlayerdata.contains(id) && !bans.currentlyBanned(id))
                eligible.add(id);
        }
        return Set.copyOf(eligible);
    }

    /** Update only on first registration, not every monetary movement. */
    public synchronized void remember(Set<UUID> registered) throws IOException {
        var additions = Set.copyOf(registered);
        if (known.containsAll(additions)) return;
        var next = new HashSet<>(known);
        next.addAll(additions);
        Path parent = path.getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "registered-players-", ".tmp");
        try {
            String content = next.stream().map(UUID::toString).sorted()
                    .collect(Collectors.joining("\n", "", "\n"));
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            known = Set.copyOf(next);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
