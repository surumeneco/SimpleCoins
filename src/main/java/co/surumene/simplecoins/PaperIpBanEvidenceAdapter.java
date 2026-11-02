package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.BanEvidence;
import io.papermc.paper.ban.BanListType;
import java.net.InetAddress;
import java.util.Set;
import java.util.function.Function;
import org.bukkit.Bukkit;

/**
 * Paper IP-ban evidence for a caller-verified UUID/address association.
 * No UUID-to-address mapping is inferred here. Unknown or incomplete mappings
 * cannot prove CLEAR; a definitive ban still proves BANNED.
 */
public final class PaperIpBanEvidenceAdapter {
    private PaperIpBanEvidenceAdapter() {}

    public static BanEvidence.Source readPaper(Set<InetAddress> verifiedAddresses,
                                                boolean mappingComplete) {
        return read(verifiedAddresses, mappingComplete,
                address -> Bukkit.getBanList(BanListType.IP).isBanned(address));
    }

    /** Testable boundary for the current Paper IP ban list. */
    public static BanEvidence.Source read(Set<InetAddress> verifiedAddresses,
                                          boolean mappingComplete,
                                          Function<InetAddress, Boolean> lookup) {
        if (verifiedAddresses == null || verifiedAddresses.isEmpty() || lookup == null)
            return new BanEvidence.Source("ip-ban", BanEvidence.Status.UNKNOWN);
        boolean unknown = !mappingComplete;
        boolean banned = false;
        for (InetAddress address : verifiedAddresses) {
            if (address == null) {
                unknown = true;
                continue;
            }
            try {
                Boolean result = lookup.apply(address);
                if (result == null) unknown = true;
                else if (result) banned = true;
            } catch (RuntimeException unavailable) {
                unknown = true;
            }
        }
        return new BanEvidence.Source("ip-ban", banned ? BanEvidence.Status.BANNED :
                unknown ? BanEvidence.Status.UNKNOWN : BanEvidence.Status.CLEAR);
    }
}
