package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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
            if (!seen.add(source.id()))
                throw new IOException("Duplicate BAN source evidence: " + source.id());
            banned |= source.status() == Status.BANNED;
            unknown |= source.status() == Status.UNKNOWN;
        }
        return banned ? Status.BANNED : unknown ? Status.UNKNOWN : Status.CLEAR;
    }
}
