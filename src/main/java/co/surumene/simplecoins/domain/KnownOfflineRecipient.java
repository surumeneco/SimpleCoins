package co.surumene.simplecoins.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Exact, fail-closed lookup for an already known offline grant recipient. */
public final class KnownOfflineRecipient {
    private KnownOfflineRecipient() {}

    public record Identity(UUID uuid, String name, boolean hasPlayedBefore) {}

    public static UUID resolve(String input, Collection<Identity> known) {
        if (input == null || input.isBlank() || input.startsWith("@") || known == null)
            throw new IllegalArgumentException("Invalid offline recipient query");
        Set<UUID> matches = new HashSet<>();
        for (Identity candidate : known) {
            if (candidate == null || candidate.uuid() == null)
                throw new IllegalArgumentException("Invalid known player identity");
            if (!candidate.hasPlayedBefore()) continue;
            if (candidate.uuid().toString().equalsIgnoreCase(input) ||
                    (candidate.name() != null && candidate.name().equalsIgnoreCase(input)))
                matches.add(candidate.uuid());
        }
        if (matches.size() != 1)
            throw new IllegalArgumentException("Unknown or ambiguous offline player");
        return matches.iterator().next();
    }
}
