package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-UUID BAN evidence from a caller-enumerated set of required sources.
 * UNKNOWN is never silently treated as CLEAR. This class does not discover
 * installed BAN plugins or infer IP-to-UUID mappings.
 */
public final class BanEvidence {
    private BanEvidence() {}

    public enum Status { BANNED, CLEAR, UNKNOWN }
    public record Source(String id, Status status) {}

    public static Status combine(List<Source> sources) throws IOException {
        if (sources == null || sources.isEmpty())
            throw new IOException("Missing required BAN source evidence");
        Set<String> seen = new HashSet<>();
        boolean banned = false;
        boolean unknown = false;
        for (Source source : sources) {
            if (source == null || source.id() == null || source.id().isBlank() ||
                    source.status() == null)
                throw new IOException("Malformed BAN source evidence");
            String sourceKey = source.id().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(sourceKey))
                throw new IOException("Duplicate BAN source evidence: " + source.id());
            banned |= source.status() == Status.BANNED;
            unknown |= source.status() == Status.UNKNOWN;
        }
        return banned ? Status.BANNED : unknown ? Status.UNKNOWN : Status.CLEAR;
    }
    /**
     * Authoritative form: the required source set is independent of received
     * observations. Missing required observations are UNKNOWN, never CLEAR.
     */
    public static Status combine(Set<String> requiredSourceIds, List<Source> sources)
            throws IOException {
        if (requiredSourceIds == null || requiredSourceIds.isEmpty() || sources == null)
            throw new IOException("Missing required BAN source declaration or observations");
        Set<String> required = new HashSet<>();
        for (String raw : requiredSourceIds) {
            if (raw == null || raw.isBlank())
                throw new IOException("Malformed required BAN source ID");
            if (!required.add(raw.trim().toLowerCase(Locale.ROOT)))
                throw new IOException("Duplicate required BAN source ID: " + raw);
        }
        Set<String> observed = new HashSet<>();
        boolean banned = false;
        boolean unknown = false;
        for (Source source : sources) {
            if (source == null || source.id() == null || source.id().isBlank() ||
                    source.status() == null)
                throw new IOException("Malformed BAN source evidence");
            String key = source.id().trim().toLowerCase(Locale.ROOT);
            if (!required.contains(key))
                throw new IOException("Unexpected BAN source evidence: " + source.id());
            if (!observed.add(key))
                throw new IOException("Duplicate BAN source evidence: " + source.id());
            banned |= source.status() == Status.BANNED;
            unknown |= source.status() == Status.UNKNOWN;
        }
        return banned ? Status.BANNED :
                unknown || observed.size() != required.size() ? Status.UNKNOWN : Status.CLEAR;
    }

}
