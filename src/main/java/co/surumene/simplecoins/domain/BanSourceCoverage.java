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

    private BanSourceCoverage(Map<UUID, Boolean> bannedByPlayer) {
        this.bannedByPlayer = Map.copyOf(bannedByPlayer);
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

    public boolean currentlyBanned(UUID player) throws IOException {
        if (player == null || !bannedByPlayer.containsKey(player))
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
