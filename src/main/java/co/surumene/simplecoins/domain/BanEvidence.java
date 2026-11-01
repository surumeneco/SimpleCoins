package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Per-UUID BAN evidence from an explicit caller-declared set of required
 * sources. UNKNOWN or missing required sources are never silently CLEAR.
 * This class does not discover plugins or infer IP-to-UUID mappings.
 */
public final class BanEvidence {
    private BanEvidence() {}

    public enum Status { BANNED, CLEAR, UNKNOWN }
    public record Source(String id, Status status) {}

    /**
     * An unforgeable-in-ordinary-code BAN decision produced from declared
     * required sources and observations. A raw Status is not settlement proof.
     */
    public static final class Decision {
        private final Status status;

        private Decision(Status status) { this.status = status; }

        public Status status() { return status; }
    }

    public static Decision evaluate(Set<String> requiredSourceIds, List<Source> sources)
            throws IOException {
        return new Decision(combine(requiredSourceIds, sources));
    }

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
