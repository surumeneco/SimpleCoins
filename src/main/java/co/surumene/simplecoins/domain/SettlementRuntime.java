package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/** Single product owner of protected startup and normal-session read permission.
 * Does not arm a fence, establish writer coverage, or supply A01 evidence. */
public final class SettlementRuntime {
    public record Captured(SettlementScanRunner.Staged staged, WorldStorageDiscovery.Manifest manifest) {
        public Captured { Objects.requireNonNull(staged); Objects.requireNonNull(manifest); }
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
    private final AdmissionBarrier barrier;
    private final WriterEpochFence fence;
    private volatile Attempt active;
    public SettlementRuntime(AdmissionBarrier barrier, WriterEpochFence fence) {
        this.barrier=Objects.requireNonNull(barrier); this.fence=Objects.requireNonNull(fence);
    }
    public boolean usesBarrier(AdmissionBarrier expected) { return barrier == expected; }
    /** Primary-thread entry only through the owning runtime service. */
    public synchronized Attempt begin(WriterEpochFence.Seal seal, NormalMutationJournal journal,
            BooleanSupplier tracking, BooleanSupplier noOnline) {
        Objects.requireNonNull(journal); Objects.requireNonNull(tracking); Objects.requireNonNull(noOnline);
        if (active!=null || barrier.state()!=AdmissionBarrier.State.BLOCKED)
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
    public Optional<SettlementAccumulator.Result> current() {
        var attempt=active;
        if (attempt==null || !barrier.mayAdmit()) return Optional.empty();
        return attempt.publisher.current();
    }
    public boolean mayAdmit() { return current().isPresent(); }
    /** Must not wait on a worker-held publisher monitor. Terminal gate first. */
    public void revoke() { barrier.fatalFailure(); fence.invalidate(); }
}
