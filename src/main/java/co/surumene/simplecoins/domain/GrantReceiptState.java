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
        Objects.requireNonNull(ledger, "issuance journal");
        Objects.requireNonNull(player, "player UUID");
        ledger.validateReceipt(player, completedSequence);
        if (!active()) return;
        List<GrantLedger.Grant> pending = ledger.pending(player);
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
    public GrantReceiptState begin(GrantLedger.Grant grant) {
        Objects.requireNonNull(grant, "grant");
        if (active() || grant.delivered() || grant.sequence() <= completedSequence)
            throw new IllegalStateException("Cannot start stale or concurrent issuance receipt");
        return new GrantReceiptState(completedSequence, grant.sequence(), grant.id(), 0);
    }

    /**
     * Accept only the exact next batch produced from the current canonical
     * prefix. The caller must physically save these stacks and this next
     * receipt together before exposing the inventory or acknowledging a grant.
     */
    public GrantReceiptState advance(GrantLedger.Grant grant, GrantDeliveryBatchPlanner.Batch batch) {
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
    public GrantReceiptState complete(GrantLedger.Grant grant) {
        requireSamePending(grant);
        if (partialValue != grant.amount())
            throw new IllegalStateException("Cannot complete a partially delivered grant");
        var checked = GrantDeliveryBatchPlanner.next(grant, partialValue, 0);
        if (!checked.complete())
            throw new IllegalStateException("Grant was not fully delivered");
        return new GrantReceiptState(grant.sequence(), 0, null, 0);
    }

    private void requireSamePending(GrantLedger.Grant grant) {
        Objects.requireNonNull(grant, "grant");
        if (!active() || grant.delivered()
                || grant.sequence() != activeSequence || !grant.id().equals(activeId))
            throw new IllegalStateException("Receipt handle is not the active pending grant");
    }
}
