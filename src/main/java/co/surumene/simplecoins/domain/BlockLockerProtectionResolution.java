package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Read-only ownership aggregation for already verified BlockLocker 1.14.1
 * sign evidence and physical block states. Callers must decode and reconcile
 * visible text/PDC before constructing VerifiedSign; this does not itself
 * authenticate a sign, load chunks or commit settlement results.
 */
public final class BlockLockerProtectionResolution {
    private BlockLockerProtectionResolution() {}

    public record VerifiedSign(BlockLockerSignAttachment.Position position,
                               BlockLockerSignAttachment.Mount mount,
                               BlockLockerHeaderMatcher.Type header,
                               ExternalOwnerDecision.Evidence primary,
                               boolean everyone) {
        public VerifiedSign {
            Objects.requireNonNull(position);
            Objects.requireNonNull(mount);
            Objects.requireNonNull(header);
            if ((header == BlockLockerHeaderMatcher.Type.PRIVATE) != (primary != null))
                throw new IllegalArgumentException("Inconsistent sign header/primary evidence");
        }
    }

    @FunctionalInterface
    private interface Attachment {
        boolean matches(VerifiedSign sign) throws IOException;
    }

    public static ExternalOwnerDecision.Evidence single(
            BlockLockerSignAttachment.Position container, List<VerifiedSign> signs)
            throws IOException {
        if (container == null)
            throw new IOException("Missing verified protected container position");
        return resolve(signs, sign -> BlockLockerSignAttachment.attaches(
                sign.position(), sign.mount(), container));
    }

    public static ExternalOwnerDecision.Evidence doubleChest(
            BlockLockerChestLinkage.Part first, BlockLockerChestLinkage.Part second,
            List<VerifiedSign> signs) throws IOException {
        if (!BlockLockerChestLinkage.linked(first, second))
            throw new IOException("Unverified double-chest physical linkage");
        return resolve(signs, sign -> BlockLockerChestLinkage.signAttachedToPair(
                sign.position(), sign.mount(), first, second));
    }

    private static ExternalOwnerDecision.Evidence resolve(
            List<VerifiedSign> signs, Attachment attached) throws IOException {
        if (signs == null)
            throw new IOException("Missing verified protection sign evidence");
        ExternalOwnerDecision.Evidence primary = null;
        boolean everyone = false;
        for (VerifiedSign sign : signs) {
            if (sign == null)
                throw new IOException("Null protection sign evidence");
            if (!attached.matches(sign)) continue;
            everyone |= sign.everyone();
            if (sign.header() == BlockLockerHeaderMatcher.Type.PRIVATE) {
                if (primary != null)
                    throw new IOException("Conflicting attached BlockLocker primary signs");
                primary = sign.primary();
            }
        }
        if (primary == null)
            return ExternalOwnerDecision.unknown();
        // An unresolved PRIVATE primary cannot establish public ownership,
        // even when a visible Everyone tag was independently verified.
        if (primary.kind() == ExternalOwnerDecision.Kind.UNKNOWN)
            return ExternalOwnerDecision.unknown();
        return everyone ? ExternalOwnerDecision.free() : primary;
    }
}
