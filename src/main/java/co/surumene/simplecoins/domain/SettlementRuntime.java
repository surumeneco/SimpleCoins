package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Single product owner of protected startup and normal-session read permission.
 * Does not arm a fence, establish writer coverage, or supply A01 evidence. */
public final class SettlementRuntime {
    public record Captured(SettlementScanRunner.Staged staged, WorldStorageDiscovery.Manifest manifest,
                           BanSourceCoverage bans, Set<UUID> protectedRegistrations, List<OwnerStorageIndex.Change> liveProtectedLocations) {
        public Captured(SettlementScanRunner.Staged staged, WorldStorageDiscovery.Manifest manifest,
                BanSourceCoverage bans, Set<UUID> protectedRegistrations) {
            this(staged,manifest,bans,protectedRegistrations,List.of());
        }
        public Captured(SettlementScanRunner.Staged staged, WorldStorageDiscovery.Manifest manifest) {
            this(staged, manifest, null, Set.of());
        }
        public Captured(SettlementScanRunner.Staged staged, WorldStorageDiscovery.Manifest manifest,
                        BanSourceCoverage bans) {
            this(staged, manifest, bans, Set.of());
        }
        public Captured {
            Objects.requireNonNull(staged); Objects.requireNonNull(manifest);
            protectedRegistrations=Set.copyOf(protectedRegistrations);
            liveProtectedLocations=List.copyOf(liveProtectedLocations);
        }
    }
    @FunctionalInterface public interface SourceStage { Captured capture() throws IOException; }
    public static final class Attempt {
        private final long generation;
        private final WriterEpochFence.Seal seal;
        private final NormalMutationJournal journal;
        private final SettlementProtectedPublisher publisher;
        private final AtomicBoolean preparing = new AtomicBoolean();
        private Attempt(long generation, WriterEpochFence.Seal seal, NormalMutationJournal journal,
                SettlementProtectedPublisher publisher) {
            this.generation=generation; this.seal=seal; this.journal=journal; this.publisher=publisher;
        }
    }
    public static final class Prepared {
        private final Attempt attempt;
        private final SettlementProtectedPublisher.PreparedHandoff handoff;
        private Prepared(Attempt attempt, SettlementProtectedPublisher.PreparedHandoff handoff) {
            this.attempt=attempt; this.handoff=handoff;
        }
    }
    /** New path: a source stage plus an ordinary in-memory accounting handoff. */
    public static final class MemoryAttempt {
        private final long generation;
        private final BooleanSupplier tracking, noOnline;
        private final AtomicBoolean preparing = new AtomicBoolean();
        private MemoryAttempt(long generation, BooleanSupplier tracking, BooleanSupplier noOnline) {
            this.generation = generation; this.tracking = tracking; this.noOnline = noOnline;
        }
    }
    public static final class MemoryPrepared {
        private final MemoryAttempt attempt;
        private final MemoryAccounting accounting;
        private final OwnerStorageIndex locations;
        private MemoryPrepared(MemoryAttempt attempt, MemoryAccounting accounting, OwnerStorageIndex locations) {
            this.attempt = attempt; this.accounting = accounting; this.locations = locations;
        }
        /** Completed reconstruction, not a published statistic or admission permit. */
        public MemoryAccounting.Snapshot summary() { return accounting.snapshot(); }
    }
    private volatile MemoryAttempt memoryAttempt;
    private volatile MemoryAccounting memory;
    private volatile OwnerStorageIndex ownerLocations;
    private final AdmissionBarrier barrier;
    private final WriterEpochFence fence;
    private final RegisteredPlayerHistory registrationHistory;
    private volatile Attempt active;
    public SettlementRuntime(AdmissionBarrier barrier, WriterEpochFence fence) {
        this(barrier, fence, null);
    }
    public SettlementRuntime(AdmissionBarrier barrier, WriterEpochFence fence,
            RegisteredPlayerHistory registrationHistory) {
        this.barrier=Objects.requireNonNull(barrier); this.fence=Objects.requireNonNull(fence);
        this.registrationHistory=registrationHistory;
    }
    public boolean usesBarrier(AdmissionBarrier expected) { return barrier == expected; }
    /** Primary-thread entry only through the owning runtime service. */
    public synchronized Attempt begin(WriterEpochFence.Seal seal, NormalMutationJournal journal,
            BooleanSupplier tracking, BooleanSupplier noOnline) {
        Objects.requireNonNull(journal); Objects.requireNonNull(tracking); Objects.requireNonNull(noOnline);
        if (active!=null || memoryAttempt!=null || barrier.state()!=AdmissionBarrier.State.BLOCKED)
            throw new IllegalStateException("Settlement runtime already started or revoked");
        fence.requireDrained(seal);
        if (!noOnline.getAsBoolean()) throw new IllegalStateException("Players are online");
        var publisher=new SettlementProtectedPublisher(fence,seal,barrier,tracking,noOnline);
        active=new Attempt(barrier.begin(),seal,journal,publisher);
        return active;
    }
    /** Worker phase. Neither the lease nor a caller flag establishes writer coverage. */
    public Prepared prepare(Attempt attempt, SourceStage source) throws IOException {
        requireScanning(attempt);
        if (!attempt.preparing.compareAndSet(false,true))
            throw new IllegalStateException("Settlement source preparation already started");
        try {
            var captured=fence.withDrainedLease(attempt.seal, Objects.requireNonNull(source)::capture);
            requireScanning(attempt);
            var handoff=attempt.publisher.prepareNormalHandoff(captured.staged(),captured.manifest(),attempt.journal);
            requireScanning(attempt);
            return new Prepared(attempt,handoff);
        } catch (IOException | RuntimeException | Error failure) { revoke(); throw failure; }
    }
    /** Called only with fresh independently produced A01 evidence on primary. */
    public SettlementWriterEpochDecision.Verdict complete(Prepared prepared,
            SettlementWriterEpochDecision.Evidence evidence) {
        Objects.requireNonNull(prepared);
        requireScanning(prepared.attempt);
        return prepared.attempt.publisher.completeNormalHandoff(prepared.attempt.generation,
                prepared.handoff,evidence);
    }
    private void requireScanning(Attempt attempt) {
        if (attempt==null || active!=attempt || barrier.state()!=AdmissionBarrier.State.SCANNING)
            throw new IllegalStateException("Unknown or no longer scanning settlement attempt");
    }
    /** Start the revised A01 path without arming the native writer fence.
     * The caller owns the managed static/read-reconciliation strategy. */
    public synchronized MemoryAttempt beginMemory(BooleanSupplier tracking, BooleanSupplier noOnline) {
        Objects.requireNonNull(tracking); Objects.requireNonNull(noOnline);
        if (active != null || memoryAttempt != null || barrier.state() != AdmissionBarrier.State.BLOCKED)
            throw new IllegalStateException("Settlement runtime already started or revoked");
        if (!noOnline.getAsBoolean()) throw new IllegalStateException("Players are online");
        memoryAttempt = new MemoryAttempt(barrier.begin(), tracking, noOnline);
        return memoryAttempt;
    }

    /** Off-main: require the actual source runner, then initialize a complete unpublished cache.
     * A content witness alone does not resolve known LIVE/SAVED conflicts. */
    public MemoryPrepared prepareMemory(MemoryAttempt attempt, SourceStage source) throws IOException {
        requireMemoryScanning(attempt);
        if (!attempt.preparing.compareAndSet(false, true))
            throw new IllegalStateException("Settlement source preparation already started");
        try {
            var captured = Objects.requireNonNull(source).capture();
            var staged = captured.staged();
            var manifest = captured.manifest();
            var witness = staged.savedContentWitness();
            if (manifest.origin() == null || witness == null || !staged.issuedByVerifiedTraversal())
                throw new IOException("Missing verified traversal origin");
            witness.requireOriginalManifest(manifest);
            if (!staged.requiredSourceIds().containsAll(manifest.requiredSourceIds()))
                throw new IOException("Mandatory saved source omitted");
            witness.requireUnchanged();
            var baseline = SettlementSnapshotPublisher.verifiedCopy(staged.pending().commit());
            var dead = new HashMap<MemoryAccounting.Endpoint, BigInteger>();
            for (var location : baseline.deadLocations())
                dead.merge(MemoryAccounting.Endpoint.dead(location.owner(), location.reason(), location.location()),
                        location.value(), BigInteger::add);
            var balances = new HashMap<>(baseline.personalBalances());
            if (registrationHistory != null && !registrationHistory.snapshot().isEmpty()) {
                if (captured.bans() == null)
                    throw new IOException("Missing current BAN evidence for previously registered players");
                for (UUID id : registrationHistory.eligible(manifest.playerDataFiles().keySet(), captured.bans()))
                    balances.putIfAbsent(id, BigInteger.ZERO);
            }
            if (!captured.protectedRegistrations().isEmpty()) {
                if (captured.bans() == null) throw new IOException("Missing BAN evidence for protected registration");
                for (UUID id : captured.protectedRegistrations())
                    if (manifest.playerDataFiles().containsKey(id) && !captured.bans().currentlyBanned(id))
                        balances.putIfAbsent(id,BigInteger.ZERO);
            }
            var accounting = new MemoryAccounting(balances, baseline.free(), dead);
            requireMemoryScanning(attempt);
            var locations = staged.pending().ownerStorageIndex();
            locations.apply(captured.liveProtectedLocations());
            return new MemoryPrepared(attempt, accounting, locations);
        } catch (IOException | RuntimeException | Error failure) {
            revoke();
            throw failure;
        }
    }

    /** Main-thread final phase: the owner must supply fresh, actual A01 observations. */
    public SettlementWriterEpochDecision.Verdict completeMemory(MemoryPrepared prepared,
            SettlementWriterEpochDecision.Evidence evidence) {
        Objects.requireNonNull(prepared);
        synchronized (barrier) {
            requireMemoryScanning(prepared.attempt);
            try {
                var verdict = SettlementWriterEpochDecision.evaluate(evidence);
                if (verdict == SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED) {
                    revoke();
                    return verdict;
                }
                if (verdict == SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED) return verdict;
                if (!prepared.attempt.noOnline.getAsBoolean() || !prepared.attempt.tracking.getAsBoolean())
                    throw new IllegalStateException("Memory settlement handoff prerequisites lost");
                rememberRegistrations(prepared.accounting.snapshot().personal().keySet());
                memory = prepared.accounting;
                ownerLocations = prepared.locations;
                barrier.complete(prepared.attempt.generation, true, true);
                return verdict;
            } catch (RuntimeException | Error failure) {
                revoke();
                throw failure;
            }
        }
    }
    private void requireMemoryScanning(MemoryAttempt attempt) {
        if (attempt == null || memoryAttempt != attempt || barrier.state() != AdmissionBarrier.State.SCANNING)
            throw new IllegalStateException("Unknown or no longer scanning memory settlement attempt");
    }
    private boolean memoryReady() {
        if (!barrier.mayAdmit() || memory == null) return false;
        try {
            if (memoryAttempt.tracking.getAsBoolean()) return true;
        } catch (RuntimeException | Error failure) {
            revoke();
            return false;
        }
        revoke();
        return false;
    }

    /** Apply only a confirmed physical operation, with one sequence for all its effects.
     * A rejected observed delta means the derived statistics can no longer be trusted. */
    public MemoryAccounting.Outcome recordMemory(long sequence, List<MemoryAccounting.Movement> movements,
            Set<UUID> unregister) {
        synchronized (barrier) {
            if (memoryAttempt == null || !memoryReady())
                throw new IllegalStateException("Normal memory accounting is unavailable");
            var outcome = memory.record(sequence, movements, unregister);
            if (outcome == MemoryAccounting.Outcome.REJECTED) revoke();
            if (outcome == MemoryAccounting.Outcome.APPLIED)
                rememberConfirmedParticipants(movements, Set.of());
            return outcome;
        }
    }

    /** Shared sequence allocation for main-thread physical operations. */
    public boolean memoryAccountingReady() {
        synchronized (barrier) { return memoryAttempt != null && memoryReady(); }
    }
    public void recordConfirmedMemory(List<MemoryAccounting.Movement> movements, Set<UUID> registered) {
        recordConfirmedMemory(movements, registered, List.of());
    }
    public void recordConfirmedMemory(List<MemoryAccounting.Movement> movements, Set<UUID> registered,
            List<OwnerStorageIndex.Change> locations) {
        var observed = List.copyOf(locations);
        synchronized (barrier) {
            if (!memoryAccountingReady())
                throw new IllegalStateException("Normal memory accounting is unavailable");
            boolean newRegistration = registered.stream().anyMatch(id -> !memory.isRegistered(id));
            if ((!movements.isEmpty() || newRegistration) && memory.recordNext(movements, registered) != MemoryAccounting.Outcome.APPLIED) {
                revoke();
                throw new IllegalStateException("Confirmed physical operation disagrees with accounting");
            }
            ownerLocations.apply(observed);
            rememberConfirmedParticipants(movements, registered);
        }
    }

    public record OwnerReadPlan(UUID owner, Set<OwnerStorageIndex.Location> locations, long indexRevision, long ledgerSequence) {}

    /** Snapshot identity for an asynchronous targeted physical re-read. */
    public OwnerReadPlan planOwnerRead(UUID owner) {
        synchronized (barrier) {
            if (!memoryAccountingReady()) throw new IllegalStateException("Normal memory accounting is unavailable");
            return new OwnerReadPlan(owner, ownerLocations.locations(owner), ownerLocations.revision(), memory.sequence());
        }
    }

    /** Apply a complete observation only to the exact still-current ledger/index generation.
     * A changed generation is a retry, not permission to publish a stale observation. */
    public boolean reconcileOwner(OwnerReadPlan plan, boolean banned, boolean hasPlayerdata,
            boolean registrationBasis, Map<String, BigInteger> observed) {
        synchronized (barrier) {
            if (!memoryAccountingReady() || plan.indexRevision() != ownerLocations.revision()
                    || plan.ledgerSequence() != memory.sequence()
                    || !plan.locations().equals(ownerLocations.locations(plan.owner()))) return false;
            boolean historical = registrationHistory != null && registrationHistory.snapshot().contains(plan.owner());
            if (memory.reconcileOwner(plan.owner(),true,banned,hasPlayerdata,registrationBasis || historical,observed)
                    != MemoryAccounting.Outcome.APPLIED) return false;
            if (memory.isRegistered(plan.owner())) rememberRegistrations(Set.of(plan.owner()));
            return true;
        }
    }

    private void rememberConfirmedParticipants(List<MemoryAccounting.Movement> movements, Set<UUID> registered) {
        if (registrationHistory == null) return;
        var participants = new java.util.HashSet<>(registered);
        for (var move : movements) {
            for (var endpoint : List.of(move.from(), move.to())) {
                if (endpoint.owner() != null && endpoint.owner().kind() == AssetOwner.Kind.PERSONAL)
                    participants.add(endpoint.owner().player());
            }
        }
        participants.removeIf(id -> !memory.isRegistered(id));
        rememberRegistrations(participants);
    }

    private void rememberRegistrations(Set<UUID> registered) {
        if (registrationHistory == null) return;
        try {
            registrationHistory.remember(registered);
        } catch (IOException | RuntimeException failure) {
            revoke();
            throw new IllegalStateException("Cannot persist confirmed registered-player history", failure);
        }
    }

    public Optional<SettlementAccumulator.Result> current() {
        if (memoryAttempt != null) {
            synchronized (barrier) {
                if (!memoryReady()) return Optional.empty();
                var snapshot = memory.snapshot();
                var locations = snapshot.dead().entrySet().stream().map(entry ->
                        new SettlementAccumulator.DeadLocation(entry.getKey().owner().player(),
                                entry.getKey().owner().reason(), entry.getValue(), entry.getKey().location())).toList();
                var dead = locations.stream().map(SettlementAccumulator.DeadLocation::value)
                        .reduce(BigInteger.ZERO, BigInteger::add);
                return Optional.of(new SettlementAccumulator.Result(
                        AssetStatistics.calculate(snapshot.personal(), snapshot.free(), dead),
                        snapshot.personal(), snapshot.free(), dead, locations));
            }
        }
        var attempt=active;
        if (attempt==null || !barrier.mayAdmit()) return Optional.empty();
        return attempt.publisher.current();
    }
    public boolean mayAdmit() {
        if (memoryAttempt != null) {
            synchronized (barrier) { return memoryReady(); }
        }
        return current().isPresent();
    }
    /** Terminal gate first; the new path has no armed fence to manage. */
    public void revoke() {
        barrier.fatalFailure();
        memory = null;
        ownerLocations = null;
        if (memoryAttempt == null) fence.invalidate();
    }
}
