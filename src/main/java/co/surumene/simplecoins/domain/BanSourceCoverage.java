package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable per-UUID BAN evidence evaluated from a declared, complete set of
 * provider identities. This validates observations, NOT whether a Paper/IP or
 * external plugin adapter actually sampled the authoritative current state.
 *
 * UNKNOWN, missing sources and missing subjects never become CLEAR. Even a
 * positive BAN observation cannot claim a fully checked generation while
 * another mandatory provider was not sampled.
 */
public final class BanSourceCoverage {
    private final Map<UUID, Boolean> bannedByPlayer;
    private final Set<UUID> completeBannedUnion;

    private BanSourceCoverage(Map<UUID, Boolean> bannedByPlayer) {
        this.bannedByPlayer = Map.copyOf(bannedByPlayer);
        this.completeBannedUnion = null;
    }

    private BanSourceCoverage(Set<UUID> completeBannedUnion) {
        this.bannedByPlayer = Map.of();
        this.completeBannedUnion = Set.copyOf(completeBannedUnion);
    }

    /**
     * Alternative representation for providers which authoritatively enumerate
     * their entire current banned-UUID set. Unlike partial UUID observations,
     * complete lists can classify a UUID first encountered during the scan.
     * The caller must establish complete lists and the actual provider roster.
     */
    public static BanSourceCoverage captureCompleteLists(
            Set<String> requiredSourceIds, Map<String, Set<UUID>> completeLists)
            throws IOException {
        if (requiredSourceIds == null || requiredSourceIds.isEmpty() || completeLists == null)
            throw new IOException("Missing complete BAN list declarations");
        Set<String> required = new HashSet<>();
        for (String raw : requiredSourceIds) {
            if (raw == null || raw.isBlank()
                    || !required.add(raw.trim().toLowerCase(Locale.ROOT)))
                throw new IOException("Invalid or duplicate required BAN source");
        }
        Set<String> actual = new HashSet<>();
        Set<UUID> union = new HashSet<>();
        for (var entry : completeLists.entrySet()) {
            String raw = entry.getKey();
            if (raw == null || raw.isBlank() || entry.getValue() == null)
                throw new IOException("Missing complete BAN provider list");
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (!required.contains(id) || !actual.add(id))
                throw new IOException("Unexpected or duplicate complete BAN provider: " + id);
            for (UUID subject : entry.getValue()) {
                if (subject == null) throw new IOException("Missing banned UUID");
                union.add(subject);
            }
        }
        if (!actual.equals(required))
            throw new IOException("Missing complete BAN provider list");
        return new BanSourceCoverage(union);
    }

    public static BanSourceCoverage capture(
            Set<String> requiredSourceIds,
            Map<UUID, List<BanEvidence.Source>> observedByPlayer) throws IOException {
        if (requiredSourceIds == null || requiredSourceIds.isEmpty()
                || observedByPlayer == null)
            throw new IOException("Missing required BAN source declarations or observations");

        Set<String> sourceIds = new HashSet<>();
        for (String sourceId : requiredSourceIds) {
            if (sourceId == null || sourceId.isBlank())
                throw new IOException("Invalid required BAN provider identity");
            String normalized = sourceId.trim().toLowerCase(Locale.ROOT);
            if (!sourceIds.add(normalized))
                throw new IOException("Duplicate required BAN provider identity: " + sourceId);
        }
        Map<UUID, Boolean> evaluated = new HashMap<>();
        for (var entry : observedByPlayer.entrySet()) {
            UUID player = entry.getKey();
            List<BanEvidence.Source> sources = entry.getValue();
            if (player == null || sources == null)
                throw new IOException("Missing BAN subject UUID or source evidence");
            // BanEvidence.combine permits a definitive BANNED conclusion with
            // incomplete observations; a *complete snapshot* must not.
            if (sources.size() != sourceIds.size())
                throw new IOException("Incomplete BAN provider coverage for " + player);
            BanEvidence.Decision decision = BanEvidence.evaluate(sourceIds, sources);
            if (decision.status() == BanEvidence.Status.UNKNOWN ||
                    sources.stream().anyMatch(s -> s.status() == BanEvidence.Status.UNKNOWN))
                throw new IOException("Unknown BAN provider status for " + player);
            evaluated.put(player, decision.status() == BanEvidence.Status.BANNED);
        }
        return new BanSourceCoverage(evaluated);
    }

    /** Compare classification universes, only when both observations are complete. */
    public boolean sameCompleteBannedPlayers(BanSourceCoverage current) {
        return current != null && completeBannedUnion != null
                && completeBannedUnion.equals(current.completeBannedUnion);
    }

    public boolean currentlyBanned(UUID player) throws IOException {
        if (player == null)
            throw new IOException("Missing UUID for BAN coverage");
        if (completeBannedUnion != null) return completeBannedUnion.contains(player);
        if (!bannedByPlayer.containsKey(player))
            throw new IOException("Missing complete BAN source coverage for " + player);
        return bannedByPlayer.get(player);
    }

    /** All supplied identities must have full evidence, not only the banned ones. */
    public Set<UUID> bannedAmong(Set<UUID> requiredPlayers) throws IOException {
        if (requiredPlayers == null)
            throw new IOException("Missing required BAN subject collection");
        Set<UUID> banned = new HashSet<>();
        for (UUID player : requiredPlayers)
            if (currentlyBanned(player)) banned.add(player);
        return Set.copyOf(banned);
    }
}
