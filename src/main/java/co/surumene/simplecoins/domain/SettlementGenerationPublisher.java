package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * In-process, generation-checked publication of complete settlement results.
 * Does not persist generations, freeze the world or prove snapshot consistency.
 */
public final class SettlementGenerationPublisher {
    public record Published(long generation, SettlementAccumulator.Result result) {
        public Published {
            if (generation <= 0) throw new IllegalArgumentException("Invalid generation");
            Objects.requireNonNull(result);
        }
    }

    private Published current;
    private boolean durableUncertain;
    private boolean durableMode;

    public synchronized Optional<Published> current() {
        return Optional.ofNullable(current);
    }

    public synchronized Published publish(long expectedGeneration,
                                          SettlementAccumulator pending) {
        checkDurableKnown();
        if (durableMode)
            throw new IllegalStateException("Durable settlement generation requires persistent publication");
        if (expectedGeneration < 0)
            throw new IllegalArgumentException("Negative expected settlement generation");
        Objects.requireNonNull(pending, "pending");
        long actual = current == null ? 0 : current.generation();
        if (actual != expectedGeneration)
            throw new IllegalStateException("Stale settlement generation");
        long next = Math.addExact(actual, 1);
        SettlementAccumulator.Result complete = pending.commit();
        Published published = new Published(next, complete);
        current = published;
        return published;
    }
    @FunctionalInterface
    public interface DurableWriter {
        /** Return only after the complete candidate is durably committed. */
        void persist(Published candidate) throws IOException;
    }

    /**
     * Publish only after a caller-supplied durable writer succeeds. The writer
     * is responsible for atomic storage and fsync; this method guarantees only
     * in-process visibility ordering, not an on-disk format or restart replay.
     */
    public synchronized Published publishDurably(long expectedGeneration,
                                                 SettlementAccumulator pending,
                                                 DurableWriter writer) throws IOException {
        checkDurableKnown();
        if (expectedGeneration < 0)
            throw new IllegalArgumentException("Negative expected settlement generation");
        Objects.requireNonNull(pending, "pending");
        Objects.requireNonNull(writer, "writer");
        long actual = current == null ? 0 : current.generation();
        if (actual != expectedGeneration)
            throw new IllegalStateException("Stale settlement generation");
        long next = Math.addExact(actual, 1);
        Published candidate = new Published(next, pending.commit());
        try {
            writer.persist(candidate);
        } catch (IOException | RuntimeException | Error uncertain) {
            // The writer may have committed before throwing. Refuse further
            // publication until restart recovery reconciles the durable head.
            durableUncertain = true;
            throw uncertain;
        }
        current = candidate;
        durableMode = true;
        return candidate;
    }

    /** Recover the authoritative generation from verified durable bytes. */
    public static SettlementGenerationPublisher recoverFrom(SettlementSnapshotStore store)
            throws IOException {
        Objects.requireNonNull(store, "store");
        var publisher = new SettlementGenerationPublisher();
        publisher.current = store.load().orElse(null);
        publisher.durableMode = true;
        return publisher;
    }

    private void checkDurableKnown() {
        if (durableUncertain)
            throw new IllegalStateException(
                    "Durable settlement generation is uncertain; reload and reconcile before publishing");
    }

}
