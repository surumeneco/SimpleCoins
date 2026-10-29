package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Staging transaction for a settlement, not an inventory/region scanner.
 *
 * A caller must discover ALL required sources, provide stable physical slot keys,
 * report source completion, and resolve ownership/BAN state from authoritative data.
 * This class refuses to publish partial or failed source sets.
 */
public final class SettlementAccumulator {
    public record Asset(String source, String physicalKey, String world, UUID owner,
                        boolean everyoneProtected, boolean currentlyBanned,
                        boolean hasPlayerdata, BigInteger value, String location) {
        public Asset {
            if (source == null || source.isBlank() || physicalKey == null || physicalKey.isBlank()
                    || location == null || location.isBlank() || value == null || value.signum() < 0) {
                throw new IllegalArgumentException("Invalid source, slot, location or value");
            }
        }
    }

    public record DeadLocation(UUID owner, AssetOwner.DeadReason reason,
                               BigInteger value, String location) {}

    public record Result(AssetStatistics.Snapshot statistics,
                         Map<UUID, BigInteger> personalBalances,
                         BigInteger free, BigInteger dead, List<DeadLocation> deadLocations) {}

    private final WorldScope worldScope;
    private final Set<String> required;
    private final Set<String> completed = new HashSet<>();
    private final Map<String, String> failures = new HashMap<>();
    private final Set<String> occupiedSlots = new HashSet<>();
    private final Map<UUID, BigInteger> personal = new HashMap<>();
    private final List<DeadLocation> deadLocations = new ArrayList<>();
    private BigInteger free = BigInteger.ZERO;
    private BigInteger dead = BigInteger.ZERO;
    private boolean committed;
    private boolean rejectedInput;

    public SettlementAccumulator(WorldScope worldScope, Set<String> requiredSources) {
        this.worldScope = Objects.requireNonNull(worldScope);
        if (requiredSources == null || requiredSources.isEmpty() ||
                requiredSources.stream().anyMatch(s -> s == null || s.isBlank())) {
            throw new IllegalArgumentException("Required source set must be non-empty");
        }
        this.required = Set.copyOf(requiredSources);
    }

    public void accept(Asset asset) {
        checkOpen();
        try {
            Objects.requireNonNull(asset);
            verifySource(asset.source());
            if (completed.contains(asset.source()) || failures.containsKey(asset.source()))
                throw new IllegalStateException("Source is no longer accepting assets: " + asset.source());
            // Reject the same physical slot even when exposed by multiple memory/disk adapters.
            if (!occupiedSlots.add(asset.physicalKey()))
                throw new IllegalArgumentException("Duplicate physical asset slot: " + asset.physicalKey());
            if (!worldScope.includes(asset.world())) return;

            AssetOwner.Result owner = AssetOwner.resolve(asset.owner(), asset.everyoneProtected(),
                    asset.currentlyBanned(), asset.hasPlayerdata());
            switch (owner.kind()) {
                case PERSONAL -> personal.merge(owner.player(), asset.value(), BigInteger::add);
                case FREE -> free = free.add(asset.value());
                case DEAD -> {
                    dead = dead.add(asset.value());
                    if (asset.value().signum() > 0)
                        deadLocations.add(new DeadLocation(owner.player(), owner.reason(),
                                asset.value(), asset.location()));
                }
            }
        } catch (RuntimeException failure) {
            rejectedInput = true;
            throw failure;
        }
    }

    /**
     * Only previously established registered users with present playerdata and no
     * current BAN can be carried forward with a zero balance.
     */
    public void preservePreviouslyRegistered(UUID uuid, boolean currentlyBanned, boolean hasPlayerdata) {
        checkOpen();
        if (uuid == null) {
            rejectedInput = true;
            throw new IllegalArgumentException("Missing registered UUID");
        }
        if (!currentlyBanned && hasPlayerdata) personal.putIfAbsent(uuid, BigInteger.ZERO);
    }

    public void completeSource(String source) {
        checkOpen();
        verifySource(source);
        if (failures.containsKey(source) || !completed.add(source)) {
            rejectedInput = true;
            throw new IllegalStateException("Source failed or completed twice: " + source);
        }
    }

    public void failSource(String source, String reason) {
        checkOpen();
        verifySource(source);
        if (completed.contains(source)) {
            rejectedInput = true;
            throw new IllegalStateException("Completed source cannot fail");
        }
        if (reason == null || reason.isBlank()) {
            rejectedInput = true;
            throw new IllegalArgumentException("Missing failure reason");
        }
        failures.put(source, reason);
    }

    /** No partially built totals are visible; failure never yields a Result. */
    public Result commit() {
        checkOpen();
        if (rejectedInput) throw new IllegalStateException("Settlement rejected an input; discard the transaction");
        if (!failures.isEmpty()) throw new IllegalStateException("Unreliable settlement sources: " + failures);
        if (!completed.containsAll(required))
            throw new IllegalStateException("Incomplete sources: " + missingSources());
        committed = true;
        Map<UUID, BigInteger> balances = Map.copyOf(personal);
        return new Result(AssetStatistics.calculate(balances, free, dead),
                balances, free, dead, List.copyOf(deadLocations));
    }

    private Set<String> missingSources() {
        Set<String> missing = new HashSet<>(required);
        missing.removeAll(completed);
        return missing;
    }

    private void verifySource(String source) {
        if (!required.contains(source)) {
            rejectedInput = true;
            throw new IllegalArgumentException("Unknown settlement source: " + source);
        }
    }

    private void checkOpen() {
        if (committed) throw new IllegalStateException("Settlement already committed");
    }
}
