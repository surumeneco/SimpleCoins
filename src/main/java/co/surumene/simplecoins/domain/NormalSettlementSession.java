package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Internal journal-backed normal accounting session. The physical adapter
 * must independently observe the exact affected sources and verify the
 * final movement. No claim of complete Paper event/writer coverage is made
 * here. Never register this session without actual G0 and normal-tracker
 * evidence. Reopening a journal alone is not such evidence.
 *
 * One barrier monitor serializes cache reads and mutation commit. The
 * journal monitor excludes direct journal mutation throughout a movement.
 * No world-wide scan is done here; only subject balances and D locations
 * are updated. Any uncertain physical effect requires cold reconciliation.
 */
final class NormalSettlementSession {
    enum Outcome { APPLIED, DUPLICATE, HISTORICAL }

    record DeadEndpoint(AssetOwner.DeadReason reason, String location) {
        DeadEndpoint {
            Objects.requireNonNull(reason, "dead reason");
            if (location == null || location.isBlank())
                throw new IllegalArgumentException("Missing dead location");
        }
    }

    record Movement(UUID operationId, NormalMutationJournal.Account from,
                    NormalMutationJournal.Account to, long amount,
                    DeadEndpoint fromDead, DeadEndpoint toDead) {
        Movement {
            Objects.requireNonNull(operationId, "operation ID");
            Objects.requireNonNull(from, "from account");
            Objects.requireNonNull(to, "to account");
            if (amount <= 0 || from.equals(to))
                throw new IllegalArgumentException("Distinct accounts and positive amount required");
            if ((from.kind() == NormalMutationJournal.AccountKind.DEAD) != (fromDead != null)
                    || (to.kind() == NormalMutationJournal.AccountKind.DEAD) != (toDead != null))
                throw new IllegalArgumentException("Dead endpoint does not match account");
        }
    }

    /**
     * Actual synchronous physical operation with authoritative final-result
     * verification. Throw on a canceled, partial, unknown or discrepant
     * effect. No new invocation may be scheduled after returning success.
     */
    @FunctionalInterface
    interface PhysicalCommit {
        void applyAndVerify() throws IOException;
    }

    private final AdmissionBarrier barrier;
    private final WriterEpochFence fence;
    private final WriterEpochFence.OpenEpoch epoch;
    private final NormalMutationJournal journal;
    private final BooleanSupplier tracking;
    private final long startupSequence;
    private final Map<UUID, Movement> applied = new HashMap<>();
    // All mutable fields are guarded by barrier, then journal.
    private NormalMutationJournal.Cursor cursor;
    private SettlementAccumulator.Result result;
    private boolean applying;

    NormalSettlementSession(AdmissionBarrier barrier, WriterEpochFence fence,
                            WriterEpochFence.OpenEpoch epoch, NormalMutationJournal journal,
                            NormalMutationJournal.Cursor cut,
                            SettlementAccumulator.Result baseline, BooleanSupplier tracking) {
        this.barrier = Objects.requireNonNull(barrier);
        this.fence = Objects.requireNonNull(fence);
        this.epoch = Objects.requireNonNull(epoch);
        this.journal = Objects.requireNonNull(journal);
        this.cursor = Objects.requireNonNull(cut);
        this.startupSequence = cut.sequence();
        this.result = Objects.requireNonNull(baseline);
        this.tracking = Objects.requireNonNull(tracking);
    }

    Optional<SettlementAccumulator.Result> current() {
        synchronized (barrier) {
            if (!barrier.mayAdmit() || applying) return Optional.empty();
            try {
                requireCurrent(cursor);
                return Optional.of(result);
            } catch (RuntimeException | Error failure) {
                revoke();
                return Optional.empty();
            }
        }
    }

    /**
     * The adapter owns the exact physical effect and its proof; this session
     * owns the durable preparation, deduplication and atomic accounting
     * transition. A historical operation is never rerun or reported as a
     * newly executed effect. A canceled/failed operation leaves PREPARED.
     */
    Outcome apply(Movement movement, PhysicalCommit physical) throws IOException {
        synchronized (barrier) {
            synchronized (journal) {
                try {
                    Objects.requireNonNull(movement, "movement");
                    Objects.requireNonNull(physical, "physical commit");
                    if (applying)
                        throw new IllegalStateException("Recursive normal accounting operation");
                    requireCurrent(cursor);
                    var previous = journal.priorOperation(movement.operationId());
                    if (previous != null) {
                        if (!previous.from().equals(movement.from())
                                || !previous.to().equals(movement.to())
                                || previous.amount() != movement.amount())
                            throw new IllegalStateException("Conflicting duplicate monetary operation");
                        if (previous.sequence() <= startupSequence)
                            return Outcome.HISTORICAL;
                        if (previous.phase() != NormalMutationJournal.Phase.EVENT_APPLIED
                                || !movement.equals(applied.get(movement.operationId())))
                            throw new IllegalStateException("Normal operation lacks matching committed accounting");
                        return Outcome.DUPLICATE;
                    }

                    // Validate the entire delta before any physical mutation or
                    // durable preparation. Unknown registration or a missing
                    // D location cannot silently become a zero balance.
                    var next = applyDelta(result, movement);
                    applying = true;
                    try {
                        var prepared = journal.prepare(movement.operationId(), movement.from(),
                                movement.to(), movement.amount());
                        var reserved = journal.checkpoint();
                        if (!prepared.newlyCreated()
                                || prepared.mutation().phase() != NormalMutationJournal.Phase.PREPARED
                                || reserved.sequence() != Math.addExact(cursor.sequence(), 1)
                                || prepared.mutation().sequence() != reserved.sequence())
                            throw new IllegalStateException("Normal monetary preparation diverged");
                        requireCurrent(reserved);
                        physical.applyAndVerify();
                        // A recursive direct journal call, changed phase,
                        // stale epoch or lost monitor may not be hidden by ACK.
                        requireCurrent(reserved);
                        var recorded = journal.markEventApplied(movement.operationId());
                        if (!recorded.equals(prepared.mutation().eventApplied()))
                            throw new IllegalStateException("Normal journal application diverged");
                        var committed = journal.checkpoint();
                        if (committed.sequence() != reserved.sequence())
                            throw new IllegalStateException("Normal journal sequence changed");
                        requireCurrent(committed);
                        cursor = committed;
                        result = next;
                        applied.put(movement.operationId(), movement);
                        return Outcome.APPLIED;
                    } finally {
                        applying = false;
                    }
                } catch (IOException | RuntimeException | Error failure) {
                    revoke();
                    throw failure;
                }
            }
        }
    }

    private void requireCurrent(NormalMutationJournal.Cursor expected) {
        if (!barrier.mayAdmit() || !tracking.getAsBoolean())
            throw new IllegalStateException("Normal monetary monitoring unavailable");
        fence.requireOpenEpoch(epoch);
        journal.requireCurrentCursor(expected);
    }

    private void revoke() {
        barrier.fatalFailure();
        fence.invalidate();
    }

    private static SettlementAccumulator.Result applyDelta(
            SettlementAccumulator.Result before, Movement movement) {
        var personal = new HashMap<>(before.personalBalances());
        var deadLocations = new ArrayList<>(before.deadLocations());
        var amount = BigInteger.valueOf(movement.amount());
        var free = adjust(personal, deadLocations, before.free(),
                movement.from(), movement.fromDead(), amount.negate());
        free = adjust(personal, deadLocations, free,
                movement.to(), movement.toDead(), amount);
        var dead = deadLocations.stream().map(SettlementAccumulator.DeadLocation::value)
                .reduce(BigInteger.ZERO, BigInteger::add);
        var stats = AssetStatistics.calculate(personal, free, dead);
        return new SettlementAccumulator.Result(stats, Map.copyOf(personal),
                free, dead, List.copyOf(deadLocations));
    }

    private static BigInteger adjust(Map<UUID, BigInteger> personal,
            List<SettlementAccumulator.DeadLocation> deadLocations, BigInteger free,
            NormalMutationJournal.Account account, DeadEndpoint endpoint,
            BigInteger delta) {
        switch (account.kind()) {
            case EXTERNAL -> { return free; }
            case FREE -> { return nonnegative(free.add(delta)); }
            case PLAYER -> {
                var previous = personal.get(account.player());
                if (previous == null)
                    throw new IllegalStateException("Unregistered normal personal account");
                personal.put(account.player(), nonnegative(previous.add(delta)));
                return free;
            }
            case DEAD -> {
                if (personal.containsKey(account.player()))
                    throw new IllegalStateException("Dead owner still registered; authoritative reclassification required");
                // Multiple items in the same logical location can have been
                // accumulated separately. Aggregate only this exact owner,
                // reason and location; never deduct from another location.
                BigInteger value = BigInteger.ZERO;
                for (var it = deadLocations.iterator(); it.hasNext();) {
                    var old = it.next();
                    if (account.player().equals(old.owner())
                            && endpoint.reason() == old.reason()
                            && endpoint.location().equals(old.location())) {
                        value = value.add(old.value());
                        it.remove();
                    }
                }
                var next = nonnegative(value.add(delta));
                if (next.signum() > 0)
                    deadLocations.add(new SettlementAccumulator.DeadLocation(
                            account.player(), endpoint.reason(), next, endpoint.location()));
                return free;
            }
            default -> throw new IllegalStateException("Unknown monetary account");
        }
    }

    private static BigInteger nonnegative(BigInteger value) {
        if (value.signum() < 0)
            throw new IllegalStateException("Normal monetary balance underflow");
        return value;
    }
}
