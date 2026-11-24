package co.surumene.simplecoins.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One coherent playerdata receipt/cursor. The physical inventory and this
 * record must be saved in the same authoritative playerdata generation by a
 * separate Paper delivery adapter; this value does not prove that save itself.
 */
public record GrantReceiptState(long completedSequence, long activeSequence,
                                UUID activeId, long partialValue) {
    public GrantReceiptState {
        if (completedSequence < 0 || activeSequence < 0 || partialValue < 0)
            throw new IllegalArgumentException("Negative grant receipt or progress");
        if (activeSequence == 0) {
            if (activeId != null || partialValue != 0)
                throw new IllegalArgumentException("Idle receipt cannot hold active progress");
        } else if (activeId == null || activeSequence <= completedSequence) {
            throw new IllegalArgumentException("Active receipt has missing or stale grant identity");
        }
    }

    public static GrantReceiptState empty() {
        return new GrantReceiptState(0, 0, null, 0);
    }

    public boolean active() {
        return activeSequence != 0;
    }

    /**
     * Recheck identity and progression with the currently recovered durable
     * journal. No incomplete earlier reservation for this player can be
     * bypassed by selecting a later active sequence.
     */
    public void verify(GrantLedger ledger, UUID player) {
        verify(Objects.requireNonNull(ledger, "issuance journal").verifiedPlayerSnapshot(player));
    }

    /** Read-only planning predicate; does not establish current durable evidence. */
    public void verify(GrantLedger.VerifiedPlayerSnapshot view) {
        Objects.requireNonNull(view, "verified planning view");
        view.validateReceipt(completedSequence);
        List<GrantLedger.Grant> pending = view.pending();
        for (var grant : pending) {
            if (grant.confirmedPartial() > 0 && completedSequence < grant.sequence()
                    && (!active() || activeSequence != grant.sequence()
                        || !grant.id().equals(activeId) || partialValue < grant.confirmedPartial()))
                throw new IllegalStateException("Receipt rolled back behind durable partial progress");
        }
        if (!active()) return;
        GrantLedger.Grant matching = null;
        for (var grant : pending) {
            if (grant.sequence() < activeSequence)
                throw new IllegalStateException("Active receipt skipped an undelivered reservation");
            if (grant.sequence() == activeSequence)
                matching = grant;
        }
        if (matching == null || !matching.id().equals(activeId))
            throw new IllegalStateException("Receipt does not identify a pending durable grant");
        try {
            GrantDeliveryBatchPlanner.next(matching, partialValue, 0);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalStateException("Receipt is not a canonical delivered stack prefix", invalid);
        }
    }

    /** Start one pending grant after the preceding receipt was verified. */
    public GrantReceiptState begin(GrantLedger ledger, UUID player, GrantLedger.Grant handle) {
        return begin(Objects.requireNonNull(ledger, "issuance journal").verifiedPlayerSnapshot(player), handle);
    }

    public GrantReceiptState begin(GrantLedger.VerifiedPlayerSnapshot view, GrantLedger.Grant handle) {
        verify(view);
        GrantLedger.Grant grant = requireAuthenticPending(view, handle);
        if (active() || grant.sequence() <= completedSequence)
            throw new IllegalStateException("Cannot start stale or concurrent issuance receipt");
        for (var pending : view.pending())
            if (pending.sequence() < grant.sequence())
                throw new IllegalStateException("Cannot start after an earlier pending grant");
        return new GrantReceiptState(completedSequence, grant.sequence(), grant.id(), 0);
    }

    /**
     * Accept only the exact next batch produced from the current canonical
     * prefix. The caller must physically save these stacks and this next
     * receipt together before exposing the inventory or acknowledging a grant.
     */
    public GrantReceiptState advance(GrantLedger ledger, UUID player,
                                     GrantLedger.Grant handle, GrantDeliveryBatchPlanner.Batch batch) {
        return advance(Objects.requireNonNull(ledger, "issuance journal").verifiedPlayerSnapshot(player), handle, batch);
    }

    public GrantReceiptState advance(GrantLedger.VerifiedPlayerSnapshot view,
                                     GrantLedger.Grant handle, GrantDeliveryBatchPlanner.Batch batch) {
        verify(view);
        GrantLedger.Grant grant = requireAuthenticPending(view, handle);
        requireSamePending(grant);
        Objects.requireNonNull(batch, "delivery batch");
        if (batch.stacks().isEmpty())
            throw new IllegalStateException("No saved receipt advance for an empty delivery batch");
        var expected = GrantDeliveryBatchPlanner.next(grant, partialValue, batch.stacks().size());
        if (!expected.equals(batch))
            throw new IllegalStateException("Receipt advance does not match canonical next batch");
        return new GrantReceiptState(completedSequence, activeSequence, activeId, batch.nextValue());
    }

    /**
     * Transition from saved partial evidence to an acknowledged full receipt.
     * The separate Paper adapter must persist the inventory and this state
     * together before calling GrantLedger.acknowledge.
     */
    public GrantReceiptState complete(GrantLedger ledger, UUID player, GrantLedger.Grant handle) {
        return complete(Objects.requireNonNull(ledger, "issuance journal").verifiedPlayerSnapshot(player), handle);
    }

    public GrantReceiptState complete(GrantLedger.VerifiedPlayerSnapshot view, GrantLedger.Grant handle) {
        verify(view);
        GrantLedger.Grant grant = requireAuthenticPending(view, handle);
        requireSamePending(grant);
        if (partialValue != grant.amount())
            throw new IllegalStateException("Cannot complete a partially delivered grant");
        var checked = GrantDeliveryBatchPlanner.next(grant, partialValue, 0);
        if (!checked.complete())
            throw new IllegalStateException("Grant was not fully delivered");
        return new GrantReceiptState(grant.sequence(), 0, null, 0);
    }

    /** Never trust a caller-created grant solely because its UUID and sequence match. */
    private static GrantLedger.Grant requireAuthenticPending(GrantLedger.VerifiedPlayerSnapshot view,
                                                             GrantLedger.Grant handle) {
        Objects.requireNonNull(view, "verified planning view");
        Objects.requireNonNull(handle, "grant handle");
        for (var pending : view.pending()) {
            if (pending.sequence() == handle.sequence()) {
                if (!pending.equals(handle))
                    throw new IllegalStateException("Grant handle differs from durable reservation");
                return pending;
            }
        }
        throw new IllegalStateException("Grant is missing, delivered or belongs to another player");
    }

    private void requireSamePending(GrantLedger.Grant grant) {
        Objects.requireNonNull(grant, "grant");
        if (!active() || grant.delivered()
                || grant.sequence() != activeSequence || !grant.id().equals(activeId))
            throw new IllegalStateException("Receipt handle is not the active pending grant");
    }
}
