package co.surumene.simplecoins.domain;

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

    public synchronized Optional<Published> current() {
        return Optional.ofNullable(current);
    }

    public synchronized Published publish(long expectedGeneration,
                                          SettlementAccumulator pending) {
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
}
