package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A strictly local owner candidate from a separately verified WCL snapshot.
 * This cannot prove registration, Entity identity, source generation, BAN,
 * playerdata or authority to publish money/statistics.
 */
public final class WclOwnerEvidence {
    private WclOwnerEvidence() {}

    public enum Kind { OWNER, FREE, HOLD }

    /** Unknown is distinct from a verified ownerless vanilla/tame source. */
    public record TameOwner(boolean verified, UUID owner) {
        public TameOwner {
            if (!verified && owner != null)
                throw new IllegalArgumentException("Unverified tame owner UUID");
        }
        public static TameOwner unknown() { return new TameOwner(false, null); }
        public static TameOwner verifiedAbsent() { return new TameOwner(true, null); }
        public static TameOwner verified(UUID owner) {
            return new TameOwner(true, Objects.requireNonNull(owner, "verified tame owner"));
        }
    }

    public record Decision(Kind kind, UUID owner) {
        public Decision {
            Objects.requireNonNull(kind, "decision kind");
            if ((kind == Kind.OWNER) != (owner != null))
                throw new IllegalArgumentException("Owner evidence and result disagree");
        }
        private static Decision hold() { return new Decision(Kind.HOLD, null); }
        private static Decision free() { return new Decision(Kind.FREE, null); }
        private static Decision owned(UUID owner) { return new Decision(Kind.OWNER, owner); }
    }

    /**
     * @param verifiedRead only true if the full required provider registration,
     *   request/snapshot identity and successful read have independent proof;
     *   a locally constructed relationship map is not such proof.
     * @param tame independent same-entity owner observation; verified-absent
     *   is a positive determination, not null caused by an incomplete read.
     */
    public static Decision decide(boolean verifiedRead, String providerId,
                                  Map<String, UUID> relationships,
                                  TameOwner tame) throws IOException {
        Objects.requireNonNull(relationships, "WCL relationships");
        Objects.requireNonNull(tame, "independent tame owner observation");
        Set<String> allowed = switch (providerId) {
            case "wwc" -> Set.of("follow_target");
            case "www" -> Set.of("commander", "owner");
            default -> throw new IOException("Unknown WCL provider for owner evidence: " + providerId);
        };
        for (var entry : relationships.entrySet()) {
            if (!allowed.contains(entry.getKey()) || entry.getValue() == null)
                throw new IOException("Unexpected WCL relationship: " + entry.getKey());
        }
        if (!verifiedRead) return Decision.hold();

        if ("wwc".equals(providerId)) {
            UUID commander = relationships.get("follow_target");
            if (commander != null) return Decision.owned(commander);
            if (!tame.verified()) return Decision.hold();
            return tame.owner() != null ? Decision.owned(tame.owner()) : Decision.free();
        }

        UUID commander = relationships.get("commander");
        UUID reportedOwner = relationships.get("owner");
        // WWW's owner and independently verified vanilla tame owner must not
        // contradict each other within the candidate. Unknown is not proof
        // of a mismatch and cannot invalidate a positive WCL owner report.
        if (tame.verified() && !Objects.equals(reportedOwner, tame.owner()))
            return Decision.hold();
        if (commander != null) return Decision.owned(commander);
        if (reportedOwner != null) return Decision.owned(reportedOwner);
        return tame.verified() ? Decision.free() : Decision.hold();
    }
}
