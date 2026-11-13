package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanEvidence;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Read-only Paper standard profile BAN source. It does NOT inspect IP bans or
 * external BAN providers. The caller must sample all required independent
 * providers, with reliable UUID mappings, before constructing a complete
 * BanSourceCoverage. It must not equate profile CLEAR with total BAN CLEAR.
 */
public final class PaperProfileBanSource {
    public static final String SOURCE_ID = "paper-profile";

    private PaperProfileBanSource() {}

    /**
     * Observe Paper's current standard profile ban list by UUID. This must
     * execute on Paper's primary thread; a result is not a frozen epoch.
     */
    public static BanEvidence.Source observe(UUID expectedPlayer) throws IOException {
        if (expectedPlayer == null)
            throw new IOException("Missing UUID for Paper profile BAN observation");
        if (!Bukkit.isPrimaryThread())
            throw new IOException("Paper profile BAN access requires the primary thread");
        try {
            return fromProfile(expectedPlayer, Bukkit.getOfflinePlayer(expectedPlayer));
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IOException("Failed to read Paper profile BAN for " + expectedPlayer, failure);
        }
    }

    /** Package-private adapter boundary: full observations can be unit tested
     * without starting Paper or claiming provider readiness. */
    static BanEvidence.Source fromProfile(UUID expectedPlayer, OfflinePlayer profile)
            throws IOException {
        if (expectedPlayer == null || profile == null)
            throw new IOException("Missing Paper profile BAN identity or response");
        try {
            if (!expectedPlayer.equals(profile.getUniqueId()))
                throw new IOException("Paper profile BAN response UUID mismatch");
            return new BanEvidence.Source(SOURCE_ID,
                    profile.isBanned()
                    ? BanEvidence.Status.BANNED : BanEvidence.Status.CLEAR);
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IOException("Unreliable Paper profile BAN response", failure);
        }
    }
}
