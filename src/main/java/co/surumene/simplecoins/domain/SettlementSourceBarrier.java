package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Fail-closed readiness gate for one complete settlement generation.
 *
 * This gate does not freeze Minecraft sources or intercept joins by itself.
 * The Paper adapter must prevent concurrent physical mutations and
 * coordinate join actions with source-mutation callbacks. A standalone
 * readiness check is only safe on an already serialized Paper main thread.
 */
public final class SettlementSourceBarrier {
    public enum State { BLOCKED, STAGING, OPEN, FAILED }

    private final Set<String> requiredSources;
    private final Set<String> completedSources = new HashSet<>();
    private State state = State.BLOCKED;
    private boolean admissionInProgress;

    public SettlementSourceBarrier(Set<String> requiredSources) {
        if (requiredSources == null || requiredSources.isEmpty() ||
                requiredSources.stream().anyMatch(source -> source == null || source.isBlank()))
            throw new IllegalArgumentException("Authoritative source set must be nonempty");
        this.requiredSources = Set.copyOf(requiredSources);
    }

    /** Derive the exact canonical closure from the verified discovery manifest. */
    public static SettlementSourceBarrier fromManifest(WorldStorageDiscovery.Manifest manifest)
            throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        return new SettlementSourceBarrier(manifest.requiredSourceIds());
    }

    public synchronized State state() { return state; }

    public synchronized void beginStaging() {
        if (state != State.BLOCKED)
            throw new IllegalStateException("Settlement staging cannot restart on an existing barrier");
        state = State.STAGING;
    }

    /** Call only after the entire authoritative source has been read and validated. */
    public synchronized void completeSource(String source) {
        requireStaging();
        if (!requiredSources.contains(source) || !completedSources.add(source)) {
            state = State.FAILED;
            throw new IllegalStateException("Unknown or duplicate settlement source completion");
        }
    }

    @FunctionalInterface public interface SourceReader { void read() throws IOException; }

    /** A source is complete only after its entire read succeeds. */
    public synchronized void readAndCompleteSource(String source, SourceReader reader)
            throws IOException {
        requireStaging();
        if (!requiredSources.contains(source) || completedSources.contains(source)) {
            state = State.FAILED;
            throw new IllegalStateException("Unknown or duplicate source read");
        }
        try {
            Objects.requireNonNull(reader, "reader").read();
            completeSource(source);
        } catch (IOException | RuntimeException | Error failure) {
            state = State.FAILED;
            throw failure;
        }
    }

    public synchronized void failSource(String source) {
        if (!requiredSources.contains(source)) {
            state = State.FAILED;
            throw new IllegalArgumentException("Unknown failed settlement source");
        }
        state = State.FAILED;
    }

    /** Must be called whenever a tracked source mutates during/after staging. */
    public synchronized void sourceMutated(String source) {
        if (!requiredSources.contains(source)) {
            state = State.FAILED;
            throw new IllegalArgumentException("Mutation of an untracked settlement source");
        }
        if (state == State.STAGING || state == State.OPEN) state = State.FAILED;
    }

    /**
     * Open only after exact durable readback of the completed generation.
     * This verifies the persisted result, not world-wide source quiescence.
     */
    @FunctionalInterface
    interface DurableReadback {
        java.util.Optional<SettlementGenerationPublisher.Published> load() throws IOException;
    }

    public synchronized void openAfterDurableReadback(
            SettlementSnapshotStore store,
            SettlementGenerationPublisher.Published expected) throws IOException {
        openAfterDurableReadback(() -> Objects.requireNonNull(store, "store").load(), expected);
    }

    /** Testable readback boundary; production always delegates to the durable store. */
    synchronized void openAfterDurableReadback(
            DurableReadback readback,
            SettlementGenerationPublisher.Published expected) throws IOException {
        requireStaging();
        if (!completedSources.equals(requiredSources))
            throw new IllegalStateException("Authoritative settlement sources remain incomplete");
        try {
            Objects.requireNonNull(readback, "readback");
            Objects.requireNonNull(expected, "expected");
            var actual = readback.load().orElseThrow(
                    () -> new IOException("Durable settlement generation is missing"));
            if (!actual.equals(expected))
                throw new IOException("Durable settlement generation differs from staged result");
            state = State.OPEN;
        } catch (IOException | RuntimeException | Error failure) {
            // A fatal readback failure leaves durable readiness unverified.
            state = State.FAILED;
            throw failure;
        }
    }

    /**
     * Run a join admission action under the same monitor as mutation reports.
     * All cooperating mutation paths must honor this monitor; this alone does
     * not freeze arbitrary world mutations outside the barrier.
     */
    public synchronized void withJoinPermit(Runnable admission) {
        requireJoinReady();
        if (admissionInProgress) {
            state = State.FAILED;
            throw new IllegalStateException("Nested settlement admission is not allowed");
        }
        Objects.requireNonNull(admission, "admission");
        admissionInProgress = true;
        try {
            admission.run();
            if (state != State.OPEN)
                throw new IllegalStateException("Admission invalidated settlement readiness");
        } catch (RuntimeException | Error uncertain) {
            // A partially applied admission may have changed world state.
            state = State.FAILED;
            throw uncertain;
        } finally {
            admissionInProgress = false;
        }
    }

    /** A readiness check only; prefer withJoinPermit for concurrent adapters. */
    public synchronized void requireJoinReady() {
        if (state != State.OPEN)
            throw new IllegalStateException("Settlement is not verified for joining");
    }

    private void requireStaging() {
        if (state != State.STAGING)
            throw new IllegalStateException("Settlement barrier is not staging");
    }
}
