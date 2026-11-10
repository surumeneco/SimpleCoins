package co.surumene.simplecoins.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Pure selection policy for the optional wandering-trader currency drop.
 *
 * This class never mints ItemStacks or records a ledger transaction. The Paper
 * adapter must commit a unique issuance receipt before exposing any new coin.
 * A draw by itself is not an issuance event.
 */
public final class WanderingTraderDropPolicy {
    public record Drop(CoinDenomination denomination, int count) {
        public Drop {
            Objects.requireNonNull(denomination, "denomination");
            if (count < 1)
                throw new IllegalArgumentException("Drop count must be positive");
        }

        public long value() {
            return (long) denomination.value() * count;
        }

        /** Number of 64-coin ItemStacks a later issuance adapter must prepare. */
        public long requiredStacks() {
            return 1L + (count - 1L) / 64L;
        }
    }

    private final boolean enabled;
    private final double probability;
    private final int minimumCount;
    private final int maximumCount;
    private final Map<CoinDenomination, Integer> weights;
    private final long totalWeight;

    public WanderingTraderDropPolicy(boolean enabled, double probability, int minimumCount,
                                     int maximumCount, Map<CoinDenomination, Integer> configuredWeights) {
        if (!Double.isFinite(probability) || probability < 0 || probability > 1)
            throw new IllegalArgumentException("Drop probability must be in [0, 1]");
        if (minimumCount < 1 || maximumCount < minimumCount)
            throw new IllegalArgumentException("Drop count range must be positive and ordered");
        Objects.requireNonNull(configuredWeights, "configuredWeights");
        EnumMap<CoinDenomination, Integer> accepted = new EnumMap<>(CoinDenomination.class);
        long sum = 0;
        for (Map.Entry<CoinDenomination, Integer> item : configuredWeights.entrySet()) {
            CoinDenomination denomination = Objects.requireNonNull(item.getKey(), "denomination");
            int weight = Objects.requireNonNull(item.getValue(), "weight");
            if (weight < 0) throw new IllegalArgumentException("Negative denomination weight");
            if (weight > 0) {
                accepted.put(denomination, weight);
                sum += weight;
            }
        }
        if (enabled && sum == 0)
            throw new IllegalArgumentException("An enabled drop needs a positive denomination weight");
        this.enabled = enabled;
        this.probability = probability;
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
        this.weights = Map.copyOf(accepted);
        this.totalWeight = sum;
    }

    /** Draw exactly one denomination/count, without side effects or issuance. */
    public Optional<Drop> draw(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        if (!enabled || probability == 0) return Optional.empty();
        if (probability < 1 && random.nextDouble() >= probability) return Optional.empty();

        long ticket = random.nextLong(totalWeight);
        // Enum iteration makes outcomes independent of input map iteration order.
        for (CoinDenomination denomination : CoinDenomination.values()) {
            int weight = weights.getOrDefault(denomination, 0);
            if (ticket < weight) {
                // Long upper bound remains valid when max == Integer.MAX_VALUE.
                int count = minimumCount == maximumCount
                        ? minimumCount : (int) random.nextLong(minimumCount, (long) maximumCount + 1L);
                return Optional.of(new Drop(denomination, count));
            }
            ticket -= weight;
        }
        throw new IllegalStateException("Weighted selection exhausted unexpectedly");
    }
}
