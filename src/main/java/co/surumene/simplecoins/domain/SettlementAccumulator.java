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
    private final Map<String, Asset> occupiedSlots = new HashMap<>();
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

    /**
     * Paper LIVE Entity equipment uses a world-qualified physical key while
     * saved Entity equipment and known WWC head / WWW weapon WCL mirrors use
     * their Entity UUID's unqualified key. Normalize only proven collision
     * identity for these mirrored slots;
     * source/provenance keys and the original Asset remain unchanged.
     *
     * This does not prove WCL and LIVE values are equal or from one writer
     * epoch. Any duplicated candidate must still invalidate the transaction.
     */
    private static String collisionKey(Asset asset) {
        String key = asset.physicalKey();
        if (asset.world() == null) return key;
        String prefix = "world/" + asset.world() + "/entity/";
        String suffix = key.endsWith("/equipment/mainhand")
                ? "/equipment/mainhand" : key.endsWith("/equipment/head")
                ? "/equipment/head" : key.endsWith("/wcl/wwc/shulker")
                ? "/wcl/wwc/shulker" : key.endsWith("/wcl/wwc/carpet")
                ? "/wcl/wwc/carpet" : null;
        if (suffix == null || !key.startsWith(prefix)) return key;
        String rawEntityId = key.substring(prefix.length(), key.length() - suffix.length());
        try {
            UUID entityId = UUID.fromString(rawEntityId);
            if (entityId.toString().equals(rawEntityId))
                return "entity/" + entityId + suffix;
        } catch (IllegalArgumentException malformed) {
            // Unverified/noncanonical keys never gain a fabricated alias.
        }
        return key;
    }

    public void accept(Asset asset) {
        checkOpen();
        try {
            Objects.requireNonNull(asset);
            verifySource(asset.source());
            if (completed.contains(asset.source()) || failures.containsKey(asset.source()))
                throw new IllegalStateException("Source is no longer accepting assets: " + asset.source());
            // Reject the same physical slot even when exposed by multiple memory/disk adapters.
            String collisionKey = collisionKey(asset);
            if (occupiedSlots.putIfAbsent(collisionKey, asset) != null)
                throw new IllegalArgumentException("Duplicate physical asset slot: " + collisionKey);
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
     * Require an already accepted asset for a second, fully proven view of
     * the SAME physical item. No balance is added: a confirmed WCL equipment
     * mirror must inherit the native Entity's identical amount, owner/BAN/
     * playerdata classification, location and source. Unproven duplicates
     * must still call accept() and fail rather than using this method.
     *
     * The caller, not this accumulator, must prove serialized ItemStack NBT
     * equality from the same selected SAVED Entity source before invocation.
     */
    public void requireExistingEquivalentAsset(Asset mirrored) {
        checkOpen();
        try {
            Objects.requireNonNull(mirrored, "verified saved WCL mirror");
            verifySource(mirrored.source());
            if (completed.contains(mirrored.source())
                    || failures.containsKey(mirrored.source())
                    || !worldScope.includes(mirrored.world()))
                throw new IllegalStateException("Unstageable WCL mirrored source");
            String key = collisionKey(mirrored);
            Asset existing = occupiedSlots.get(key);
            if (existing == null || !existing.equals(mirrored))
                throw new IllegalArgumentException(
                        "WCL mirrored item differs from native physical asset: " + key);
        } catch (RuntimeException invalid) {
            rejectedInput = true;
            throw invalid;
        }
    }

    /**
     * Only previously established registered users with present playerdata and no
     * current BAN can be carried forward with a zero balance.
     */
    public void preservePreviouslyRegistered(UUID uuid, boolean currentlyBanned, boolean hasPlayerdata) {
        checkOpen();
        if (completed.containsAll(required)) {
            rejectedInput = true;
            throw new IllegalStateException("Cannot add registered users after all sources completed");
        }
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

    /** Only a completed authoritative settlement may seed the locator index.
     * Normal per-location amounts are not retained in that index. */
    public OwnerStorageIndex ownerStorageIndex() {
        if (!committed) throw new IllegalStateException("Settlement is not committed");
        return OwnerStorageIndex.fromAssets(occupiedSlots.values(), worldScope);
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
