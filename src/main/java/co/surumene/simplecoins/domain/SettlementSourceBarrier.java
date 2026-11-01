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

    public SettlementSourceBarrier(Set<String> requiredSources) {
        if (requiredSources == null || requiredSources.isEmpty() ||
                requiredSources.stream().anyMatch(source -> source == null || source.isBlank()))
            throw new IllegalArgumentException("Authoritative source set must be nonempty");
        this.requiredSources = Set.copyOf(requiredSources);
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

    public synchronized void failSource(String source) {
        requireStaging();
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
    public synchronized void openAfterDurableReadback(
            SettlementSnapshotStore store,
            SettlementGenerationPublisher.Published expected) throws IOException {
        requireStaging();
        if (!completedSources.equals(requiredSources))
            throw new IllegalStateException("Authoritative settlement sources remain incomplete");
        try {
            Objects.requireNonNull(store, "store");
            Objects.requireNonNull(expected, "expected");
            var actual = store.load().orElseThrow(
                    () -> new IOException("Durable settlement generation is missing"));
            if (!actual.equals(expected))
                throw new IOException("Durable settlement generation differs from staged result");
            state = State.OPEN;
        } catch (IOException | RuntimeException failure) {
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
        Objects.requireNonNull(admission, "admission").run();
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
