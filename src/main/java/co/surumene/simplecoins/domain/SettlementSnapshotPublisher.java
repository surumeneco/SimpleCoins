package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/**
 * Single-process, fail-closed publication boundary for a COMPLETE settlement.
 *
 * An actual complete authoritative source set and quiescent world must be
 * established by the caller. No disk data is modified or persisted here.
 * Admission and statistics read permission are both gated by a published
 * Result, not by the startup barrier's READY flag alone.
 */
public final class SettlementSnapshotPublisher {
    private final AdmissionBarrier barrier;
    private final BooleanSupplier journalActive;
    private final AtomicReference<SettlementAccumulator.Result> staged =
            new AtomicReference<>();

    /**
     * An unbound journal monitor can never authorize player admission.
     * The actual settlement integration must supply a live monitor signal.
     */
    public SettlementSnapshotPublisher(AdmissionBarrier barrier) {
        this(barrier, () -> false);
    }

    /**
     * This signal is advisory evidence from the authoritative journal owner.
     * Its caller must ensure that the monitor is running before opening the
     * admission barrier, and revoke the gate on any monitor failure.
     */
    public SettlementSnapshotPublisher(AdmissionBarrier barrier, BooleanSupplier journalActive) {
        this.barrier = Objects.requireNonNull(barrier, "barrier");
        this.journalActive = Objects.requireNonNull(journalActive, "journalActive");
    }

    /** Forbid sharing a publisher with a different startup generation barrier. */
    public boolean usesBarrier(AdmissionBarrier expected) {
        return barrier == expected;
    }

    /**
     * Called by the main-thread publication callback while SCANNING.
     * A missing or failed required source makes commit() throw and permanently
     * revokes readiness. Never accept a caller-created Result as evidence.
     */
    public synchronized void publish(SettlementAccumulator pending) {
        try {
            if (barrier.state() != AdmissionBarrier.State.SCANNING)
                throw new IllegalStateException("Settlement not scanning at publication");
            SettlementAccumulator.Result result =
                    Objects.requireNonNull(pending, "pending settlement").commit();
            SettlementAccumulator.Result complete = verifiedCopy(result);
            if (!staged.compareAndSet(null, complete))
                throw new IllegalStateException("Settlement published more than once");
            if (barrier.state() != AdmissionBarrier.State.SCANNING)
                throw new IllegalStateException("Settlement failed during publication");
        } catch (RuntimeException failure) {
            staged.set(null);
            barrier.fatalFailure();
            throw failure;
        }
    }

    /** A failed or stopped gate can never expose a previously staged result. */
    public Optional<SettlementAccumulator.Result> current() {
        synchronized (barrier) {
            if (!barrier.mayAdmit()) return Optional.empty();
            boolean monitoring;
            try {
                monitoring = journalActive.getAsBoolean();
            } catch (RuntimeException | Error failure) {
                // A failing monitor signal must never turn into an allowed login.
                barrier.fatalFailure();
                staged.set(null);
                return Optional.empty();
            }
            if (!monitoring) {
                // Even after a successful publication, losing the journal is
                // terminal for this generation. It cannot simply reopen later.
                barrier.fatalFailure();
                staged.set(null);
                return Optional.empty();
            }
            return Optional.ofNullable(staged.get());
        }
    }

    public boolean mayAdmit() {
        return current().isPresent();
    }

    public void revoke() {
        barrier.fatalFailure();
        staged.set(null);
    }

    private static SettlementAccumulator.Result verifiedCopy(SettlementAccumulator.Result result) {
        Objects.requireNonNull(result, "settlement result");
        Map<UUID, BigInteger> balances = Map.copyOf(result.personalBalances());
        List<SettlementAccumulator.DeadLocation> locations = List.copyOf(result.deadLocations());
        BigInteger free = Objects.requireNonNull(result.free());
        BigInteger dead = Objects.requireNonNull(result.dead());
        AssetStatistics.Snapshot calculated = AssetStatistics.calculate(balances, free, dead);
        if (!calculated.equals(result.statistics()))
            throw new IllegalStateException("Settlement totals disagree with classified balances");

        BigInteger locatedDead = BigInteger.ZERO;
        for (SettlementAccumulator.DeadLocation entry : locations) {
            if (entry.owner() == null || entry.reason() == null ||
                    entry.location() == null || entry.location().isBlank() ||
                    entry.value() == null || entry.value().signum() <= 0)
                throw new IllegalStateException("Invalid dead-asset location");
            locatedDead = locatedDead.add(entry.value());
        }
        if (!locatedDead.equals(dead))
            throw new IllegalStateException("Dead-asset total disagrees with location evidence");
        return new SettlementAccumulator.Result(calculated, balances, free, dead, locations);
    }
}
