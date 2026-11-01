package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.BanEvidence;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Read-only Paper profile-ban evidence. IP bans and external ban plugins are
 * separate required sources. Call readPaper only on an appropriate server
 * execution thread and combine with all required sources before settlement.
 */
public final class PaperBanEvidenceAdapter {
    private PaperBanEvidenceAdapter() {}

    public static BanEvidence.Source readPaper(UUID owner) {
        return read(owner, id -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(id);
            return player == null ? null : player.isBanned();
        });
    }

    /** Testable boundary for a Paper profile-ban lookup. */
    public static BanEvidence.Source read(UUID owner, Function<UUID, Boolean> lookup) {
        if (owner == null)
            throw new IllegalArgumentException("Missing owner UUID for Paper BAN lookup");
        BanEvidence.Status status = BanEvidence.Status.UNKNOWN;
        if (lookup != null) {
            try {
                Boolean banned = lookup.apply(owner);
                if (banned != null)
                    status = banned ? BanEvidence.Status.BANNED : BanEvidence.Status.CLEAR;
            } catch (RuntimeException unavailable) {
                // A failed lookup is not a verified CLEAR result.
            }
        }
        return new BanEvidence.Source("paper-profile", status);
    }
}
