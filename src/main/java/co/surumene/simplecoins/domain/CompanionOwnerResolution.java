package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.UUID;

/**
 * Verified WWC/WWW companion ownership evidence. A known commander overrides
 * the owner. UNKNOWN means the source is unavailable or unresolved, not a
 * verified absence. The caller must obtain authoritative evidence separately.
 */
public final class CompanionOwnerResolution {
    private CompanionOwnerResolution() {}

    public enum State { KNOWN, ABSENT, UNKNOWN }

    public record Identity(State state, UUID uuid) {
        public Identity {
            if (state == null || (state == State.KNOWN) != (uuid != null))
                throw new IllegalArgumentException("Inconsistent companion identity evidence");
        }
    }

    public static Identity known(UUID uuid) {
        return new Identity(State.KNOWN, uuid);
    }

    public static Identity absent() {
        return new Identity(State.ABSENT, null);
    }

    public static Identity unknown() {
        return new Identity(State.UNKNOWN, null);
    }

    public static ExternalOwnerDecision.Evidence resolve(Identity commander, Identity owner)
            throws IOException {
        if (commander == null || owner == null)
            throw new IOException("Missing companion identity evidence");
        if (commander.state() == State.KNOWN)
            return ExternalOwnerDecision.personal(commander.uuid());
        if (commander.state() == State.UNKNOWN)
            return ExternalOwnerDecision.unknown();
        if (owner.state() == State.KNOWN)
            return ExternalOwnerDecision.personal(owner.uuid());
        if (owner.state() == State.UNKNOWN)
            return ExternalOwnerDecision.unknown();
        return ExternalOwnerDecision.free();
    }
}
