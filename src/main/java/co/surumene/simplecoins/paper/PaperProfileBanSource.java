package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BanEvidence;
import co.surumene.simplecoins.domain.BanSourceCoverage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    /**
     * Enumerate Paper's two standard lists once on primary. An empty IP list
     * proves no account is currently IP-banned; a nonempty list must not be
     * converted to UUIDs without authoritative associations. Other providers
     * require independently captured complete UUID lists and must be declared.
     */
    public static BanSourceCoverage captureCompleteStandard(
            Set<String> requiredSources, Map<String, Set<UUID>> externalCompleteLists)
            throws IOException {
        return captureCompleteStandard(requiredSources,externalCompleteLists,null);
    }

    /** The product supplies the observed UUID/IP association source; the legacy
     * two-argument boundary still rejects a nonempty IP list without a source. */
    public static BanSourceCoverage captureCompleteStandard(
            Set<String> requiredSources, Map<String, Set<UUID>> externalCompleteLists,
            PaperIpBanAssociations associations)throws IOException {
        if (!Bukkit.isPrimaryThread())
            throw new IOException("Paper BAN access requires the primary thread");
        if (requiredSources == null || externalCompleteLists == null
                || !requiredSources.contains(SOURCE_ID) || !requiredSources.contains("paper-ip"))
            throw new IOException("Both Paper standard BAN sources must be declared");
        try {
            Set<String> ips=Bukkit.getIPBans();
            if (associations==null&&!ips.isEmpty())
                throw new IOException("Nonempty IP BAN list requires UUID mapping source");
            Set<UUID> ipBanned=associations==null?Set.of():associations.bannedPlayers(ips);
            Set<UUID> profiles = new HashSet<>();
            for (OfflinePlayer profile : Bukkit.getBannedPlayers()) {
                if (profile == null || profile.getUniqueId() == null)
                    throw new IOException("Incomplete Paper profile BAN identity");
                if (profile.isBanned()) profiles.add(profile.getUniqueId());
            }
            Map<String, Set<UUID>> lists = new HashMap<>(externalCompleteLists);
            if (lists.containsKey(SOURCE_ID) || lists.containsKey("paper-ip"))
                throw new IOException("External provider cannot replace Paper BAN observations");
            lists.put(SOURCE_ID, profiles);
            lists.put("paper-ip", ipBanned);
            return BanSourceCoverage.captureCompleteLists(requiredSources, lists);
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw new IOException("Failed to enumerate complete Paper BAN lists", failure);
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
