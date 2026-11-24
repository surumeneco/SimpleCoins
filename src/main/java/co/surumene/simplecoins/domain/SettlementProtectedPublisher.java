package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Internal, deliberately unregistered A01/G0 final-publication composition.
 *
 * NOT a running Paper settlement coordinator. The current eight-family
 * writer Agent does not cover every installed Paper/NMS/Plugin/file/async
 * writer, and no complete LIVE/SAVED attribution and journal handoff
 * certificate is wired to bootstrap. Therefore this class cannot be invoked
 * from SimpleCoinsPlugin or registered as an AdmissionPermit provider yet.
 *
 * Its sole local guarantee is that an already proven, completely drained
 * writer epoch stays SEALED through the final atomic accumulator snapshot
 * replacement and barrier READY transition. Other A01 evidence is supplied
 * by a separately accountable authoritative observer, not inferred here.
 */
final class SettlementProtectedPublisher {
    private final WriterEpochFence fence;
    private final WriterEpochFence.Seal seal;
    private final AdmissionBarrier barrier;
    private final BooleanSupplier journalActive;
    private final BooleanSupplier noOnlinePlayers;
    private final SettlementSnapshotPublisher publication;
    private PreparedHandoff outstanding;
    private volatile NormalSettlementSession normal;

    /**
     * One source-verified, immutable unpublished baseline plus its genuine
     * durable journal cut. Only this publisher can prepare and consume it.
     */
    static final class PreparedHandoff {
        private final NormalMutationJournal journal;
        private final NormalMutationJournal.Cursor cut;
        private final SettlementAccumulator.Result baseline;
        private PreparedHandoff(NormalMutationJournal journal,
                NormalMutationJournal.Cursor cut, SettlementAccumulator.Result baseline) {
            this.journal = journal;
            this.cut = cut;
            this.baseline = baseline;
        }
    }

    SettlementProtectedPublisher(WriterEpochFence fence, WriterEpochFence.Seal seal,
                                 AdmissionBarrier barrier, BooleanSupplier journalActive,
                                 BooleanSupplier noOnlinePlayers) {
        this.fence = Objects.requireNonNull(fence, "writer fence");
        this.seal = seal; // null/unbound must fail-closed at the first commit
        this.barrier = Objects.requireNonNull(barrier, "settlement barrier");
        this.journalActive = Objects.requireNonNull(journalActive, "journal monitoring");
        this.noOnlinePlayers = Objects.requireNonNull(noOnlinePlayers, "Paper player absence");
        // A later writer fault/resume, even AFTER this method returns,
        // must make every reader of the published snapshot revoke admission.
        // The unbound publication constructor must never be used to bypass
        // this fence-generation monitor.
        this.publication = new SettlementSnapshotPublisher(barrier, this::monitoringAlive);
    }

    /**
     * One synchronous final commit, with a real staged accumulator rather than
     * an arbitrary caller-created Result. No expensive world scan or writer
     * drain is allowed in the critical callback. Caller must independently
     * prove EVERY Boolean of the Confirmed A01 PI against the actual server.
     */
    synchronized SettlementWriterEpochDecision.Verdict evaluateAndPublish(
            long generation, SettlementAccumulator candidate,
            SettlementWriterEpochDecision.Evidence evidence) {
        try {
            var verdict = SettlementWriterEpochDecision.evaluate(evidence);
            if (verdict == SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED)
                return verdict; // do not consume a partially staged candidate
            if (verdict == SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED) {
                revoke();
                return verdict;
            }
            if (verdict != SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH)
                throw new IllegalStateException("Unexpected A01 publication decision");

            return fence.withDrainedSeal(seal, () -> {
                if (barrier.state() != AdmissionBarrier.State.SCANNING)
                    throw new IllegalStateException("Settlement not scanning at atomic commit");
                if (!noOnlinePlayers.getAsBoolean())
                    throw new IllegalStateException("Paper players are online");
                if (!journalActive.getAsBoolean())
                    throw new IllegalStateException("Normal journal monitoring is not ready");

                // This never exposes any partial accumulated sums: publisher
                // validates the complete source set, ownership and totals.
                publication.publish(candidate);
                if (!noOnlinePlayers.getAsBoolean())
                    throw new IllegalStateException("Paper players arrived during commit");
                // Must be the LAST mutating operation in the callback.
                // withDrainedSeal prevents another thread from reopening the
                // writer epoch before it finishes and rechecks the seal.
                barrier.complete(generation, true, true);
                return verdict;
            });
        } catch (RuntimeException | Error failure) {
            revoke();
            throw failure;
        }
    }

    /**
     * Accept an actual source-staged SAVED generation (also used by the
     * one-shot PaperCapturedHybridSettlement), not just a bare accumulator.
     *
     * The content witness and its EXACT original source Manifest are rechecked
     * while this same already drained epoch is LEASED, preventing independent
     * resume while source files are hashed. The final accumulator publish is
     * the existing short atomic callback under the SAME seal and lock.
     *
     * A real loaded-world/WCL/BAN/Journals/installed-writers attestation is
     * still independently required before any runtime call: these immutable
     * input records alone are not G0 authorization.
     */
    synchronized SettlementWriterEpochDecision.Verdict evaluateAndPublishStaged(
            long generation, SettlementScanRunner.Staged staged,
            WorldStorageDiscovery.Manifest manifest,
            SettlementWriterEpochDecision.Evidence evidence) throws IOException {
        try {
            Objects.requireNonNull(staged, "real staged SAVED source candidate");
            Objects.requireNonNull(manifest, "original source Manifest");
            var decision = SettlementWriterEpochDecision.evaluate(evidence);
            if (decision != SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH)
                return evaluateAndPublish(generation, staged.pending(), evidence);

            var witness = staged.savedContentWitness();
            if (manifest.origin() == null || witness == null
                    || !staged.issuedByVerifiedTraversal())
                throw new IOException("Missing authentic source-runner origin for saved settlement");

            // The original source traversal, not a caller-provided record
            // attaching a genuine witness to a different accumulator, must
            // have produced these monetary values. Other full-G0 evidence is
            // still independently unproven at runtime.

            return fence.withDrainedLease(seal, () -> {
                witness.requireOriginalManifest(manifest);
                if (!staged.requiredSourceIds().containsAll(manifest.requiredSourceIds()))
                    throw new IOException("Mandatory saved source omitted from staged settlement");
                witness.requireUnchanged();
                return evaluateAndPublish(generation, staged.pending(), evidence);
            });
        } catch (IOException | RuntimeException | Error failure) {
            revoke();
            throw failure;
        }
    }

    /**
     * Worker phase: full saved-content and journal audit within the same
     * drained lease, with no player admission and no Bukkit API access.
     * The complete baseline absorbs historical movements by physical
     * reconciliation; the journal itself is retained, never replayed.
     */
    synchronized PreparedHandoff prepareNormalHandoff(SettlementScanRunner.Staged staged,
            WorldStorageDiscovery.Manifest manifest, NormalMutationJournal journal) throws IOException {
        try {
            if (barrier.state() != AdmissionBarrier.State.SCANNING || outstanding != null
                    || normal != null)
                throw new IllegalStateException("Normal handoff is one-shot while scanning");
            Objects.requireNonNull(staged, "source-verified candidate");
            Objects.requireNonNull(manifest, "original saved manifest");
            Objects.requireNonNull(journal, "normal journal");
            var witness = staged.savedContentWitness();
            if (manifest.origin() == null || witness == null
                    || !staged.issuedByVerifiedTraversal())
                throw new IOException("Missing authentic source-runner origin for normal handoff");
            var prepared = fence.withDrainedLease(seal, () -> {
                witness.requireOriginalManifest(manifest);
                if (!staged.requiredSourceIds().containsAll(manifest.requiredSourceIds()))
                    throw new IOException("Mandatory saved source omitted from normal handoff");
                witness.requireUnchanged();
                var cut = journal.verifiedCheckpoint();
                var baseline = SettlementSnapshotPublisher.verifiedCopy(staged.pending().commit());
                journal.requireCurrentCursor(cut);
                return new PreparedHandoff(journal, cut, baseline);
            });
            outstanding = prepared;
            return prepared;
        } catch (IOException | RuntimeException | Error failure) {
            revoke();
            throw failure;
        }
    }

    /**
     * Main-thread final phase (the Paper coordinator must enforce affinity).
     * The same seal still protects the entire generation. Bind the actual
     * audited journal/session and new writer epoch before readers may
     * observe READY. The caller must prove all A01 observations, including
     * real complete normal writer tracking; an injected true is not proof.
     */
    synchronized SettlementWriterEpochDecision.Verdict completeNormalHandoff(long generation,
            PreparedHandoff prepared, SettlementWriterEpochDecision.Evidence evidence) {
        try {
            var verdict = SettlementWriterEpochDecision.evaluate(evidence);
            if (verdict == SettlementWriterEpochDecision.Verdict.REVOKE_PUBLISHED) {
                revoke();
                return verdict;
            }
            if (prepared == null || prepared != outstanding || normal != null)
                throw new IllegalStateException("Unknown or consumed normal handoff");
            if (verdict == SettlementWriterEpochDecision.Verdict.HOLD_UNPUBLISHED)
                return verdict;
            if (verdict != SettlementWriterEpochDecision.Verdict.TRY_ATOMIC_PUBLISH)
                throw new IllegalStateException("Unexpected normal handoff verdict");

            // The barrier excludes readers until the fence really is OPEN.
            // Holding the actual journal monitor also excludes a phase or
            // tail change between the final cursor check and the transition.
            synchronized (barrier) {
                synchronized (prepared.journal) {
                    prepared.journal.requireCurrentCursor(prepared.cut);
                    fence.handoffDrainedSeal(seal, epoch -> {
                        if (barrier.state() != AdmissionBarrier.State.SCANNING
                                || !noOnlinePlayers.getAsBoolean()
                                || !journalActive.getAsBoolean())
                            throw new IllegalStateException("Normal journal handoff prerequisites lost");
                        prepared.journal.requireInMemoryCursor(prepared.cut);
                        normal = new NormalSettlementSession(barrier, fence, epoch,
                                prepared.journal, prepared.cut, prepared.baseline, journalActive);
                        if (!journalActive.getAsBoolean() || !noOnlinePlayers.getAsBoolean())
                            throw new IllegalStateException("Normal handoff prerequisites changed");
                        prepared.journal.requireInMemoryCursor(prepared.cut);
                        barrier.complete(generation, true, true);
                    });
                    outstanding = null;
                }
            }
            return verdict;
        } catch (RuntimeException | Error failure) {
            revoke();
            throw failure;
        }
    }

    NormalSettlementSession normalSession() {
        var session = normal;
        if (session == null || session.current().isEmpty())
            throw new IllegalStateException("No valid normal accounting session");
        return session;
    }

    private boolean monitoringAlive() {
        if (!journalActive.getAsBoolean()) return false;
        try {
            // No future grant is permitted after the writer seal went stale,
            // resumed, or faulted, even if a previous snapshot was complete.
            fence.requireDrained(seal);
            return true;
        } catch (IllegalStateException invalidatedEpoch) {
            return false;
        }
    }

    /** A result is externally visible only while both monitors stay valid. */
    Optional<SettlementAccumulator.Result> current() {
        synchronized (barrier) {
            var session = normal;
            return session == null ? publication.current() : session.current();
        }
    }

    boolean mayAdmit() {
        return current().isPresent();
    }

    void revoke() {
        synchronized (barrier) {
            publication.revoke();
            normal = null;
            fence.invalidate();
        }
    }
}
