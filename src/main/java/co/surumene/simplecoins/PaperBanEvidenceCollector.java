package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.BanEvidence;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Collect required Paper profile, IP and explicitly declared external BAN
 * sources for one UUID. Caller must prove complete external-plugin discovery
 * and UUID/address mapping; no plugin or IP history is inferred here.
 */
public final class PaperBanEvidenceCollector {
    private PaperBanEvidenceCollector() {}

    public static BanEvidence.Decision readPaper(
            UUID owner, Set<InetAddress> verifiedAddresses,
            boolean addressMappingComplete, boolean externalInventoryComplete,
            Map<String, Function<UUID, Boolean>> externalLookups) throws IOException {
        if (owner == null)
            throw new IOException("Missing owner UUID for complete BAN evidence");
        BanEvidence.Source profile = PaperBanEvidenceAdapter.readPaper(owner);
        BanEvidence.Source ip;
        try {
            ip = PaperIpBanEvidenceAdapter.readPaper(
                    verifiedAddresses, addressMappingComplete);
        } catch (RuntimeException unavailable) {
            ip = new BanEvidence.Source("ip-ban", BanEvidence.Status.UNKNOWN);
        }
        return read(owner, profile, ip, externalInventoryComplete, externalLookups);
    }

    /** Testable source aggregation without accessing a live Paper server. */
    public static BanEvidence.Decision read(
            UUID owner, BanEvidence.Source profile, BanEvidence.Source ip,
            boolean externalInventoryComplete,
            Map<String, Function<UUID, Boolean>> externalLookups) throws IOException {
        if (owner == null || profile == null || ip == null || externalLookups == null ||
                !"paper-profile".equalsIgnoreCase(profile.id()) ||
                !"ip-ban".equalsIgnoreCase(ip.id()))
            throw new IOException("Missing or invalid required Paper BAN source evidence");

        Set<String> required = new HashSet<>(Set.of("paper-profile", "ip-ban"));
        List<BanEvidence.Source> sources = new ArrayList<>(List.of(profile, ip));
        for (var entry : externalLookups.entrySet()) {
            String raw = entry.getKey();
            if (raw == null || raw.isBlank())
                throw new IOException("Missing external BAN plugin ID");
            String id = "external:" + raw.trim().toLowerCase(Locale.ROOT);
            if (!required.add(id))
                throw new IOException("Duplicate external BAN plugin ID: " + raw);
            BanEvidence.Status status = BanEvidence.Status.UNKNOWN;
            if (entry.getValue() != null) {
                try {
                    Boolean banned = entry.getValue().apply(owner);
                    if (banned != null)
                        status = banned ? BanEvidence.Status.BANNED : BanEvidence.Status.CLEAR;
                } catch (RuntimeException unavailable) {
                    status = BanEvidence.Status.UNKNOWN;
                }
            }
            sources.add(new BanEvidence.Source(id, status));
        }
        if (!externalInventoryComplete) {
            required.add("external-inventory");
            sources.add(new BanEvidence.Source(
                    "external-inventory", BanEvidence.Status.UNKNOWN));
        }
        return BanEvidence.evaluate(required, sources);
    }
}
