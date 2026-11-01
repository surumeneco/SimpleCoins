package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.UUID;

/**
 * Explicit external ownership evidence. UNKNOWN means an unavailable API,
 * missing snapshot or unresolved identity, never verified FREE ownership.
 * Conversion is permitted only after the caller has checked all required
 * sources and current BAN evidence.
 */
public final class ExternalOwnerDecision {
    private ExternalOwnerDecision() {}

    public enum Kind { PERSONAL, FREE, UNKNOWN }

    public record Evidence(Kind kind, UUID owner) {
        public Evidence {
            if (kind == null || (kind == Kind.PERSONAL) != (owner != null))
                throw new IllegalArgumentException("Inconsistent external ownership evidence");
        }
    }

    public static Evidence personal(UUID owner) {
        return new Evidence(Kind.PERSONAL, owner);
    }

    public static Evidence free() {
        return new Evidence(Kind.FREE, null);
    }

    public static Evidence unknown() {
        return new Evidence(Kind.UNKNOWN, null);
    }

    public static BlockEntitySettlementImport.Ownership toBlock(
            Evidence evidence, BanEvidence.Decision ban, boolean hasPlayerdata)
            throws IOException {
        requireKnown(evidence, ban, hasPlayerdata);
        if (evidence.kind() == Kind.FREE)
            return new BlockEntitySettlementImport.Ownership(null, false, false, false);
        return new BlockEntitySettlementImport.Ownership(evidence.owner(), false,
                ban.status() == BanEvidence.Status.BANNED, hasPlayerdata);
    }

    public static ResolvedEntitySettlementImport.Ownership toEntity(
            Evidence evidence, BanEvidence.Decision ban, boolean hasPlayerdata)
            throws IOException {
        requireKnown(evidence, ban, hasPlayerdata);
        if (evidence.kind() == Kind.FREE)
            return new ResolvedEntitySettlementImport.Ownership(null, false, false);
        return new ResolvedEntitySettlementImport.Ownership(evidence.owner(),
                ban.status() == BanEvidence.Status.BANNED, hasPlayerdata);
    }

    private static void requireKnown(Evidence evidence, BanEvidence.Decision ban,
                                     boolean hasPlayerdata) throws IOException {
        if (evidence == null || evidence.kind() == Kind.UNKNOWN)
            throw new IOException("External ownership is unresolved");
        if (evidence.kind() == Kind.FREE) {
            if (hasPlayerdata)
                throw new IOException("Ownerless asset cannot carry playerdata evidence");
            return;
        }
        if (ban == null || ban.status() == BanEvidence.Status.UNKNOWN)
            throw new IOException("Current BAN status is unresolved for personal owner");
    }
}
