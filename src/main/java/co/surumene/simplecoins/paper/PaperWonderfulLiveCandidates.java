package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WclOwnerEvidence;
import co.surumene.simplecoins.domain.WclPhysicalSlotIdentity;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * One main-thread WCL read produces monetary slots, physical alias identities,
 * and local owner evidence for a caller-selected LIVE Entity.
 *
 * These are UNPUBLISHED candidate values. This does not discover all loaded
 * entities or prove the G0 writer epoch, LIVE/SAVED selection, global physical
 * deduplication, BAN/playerdata, atomic settlement, G3 or admission.
 */
public final class PaperWonderfulLiveCandidates {
    private PaperWonderfulLiveCandidates() {}

    public record Candidate(UUID entityId, UUID worldId, String worldName,
                            String providerId, Map<String, UUID> relationships,
                            WclOwnerEvidence.Decision ownerEvidence,
                            Map<String, BigInteger> slotValues,
                            Map<String, String> physicalSlotKeys,
                            Map<String, PaperWonderfulLoadedEvidence.StoredItem> sourceItems) {
        /** Old test-only/local snapshot shape cannot prove item identity. */
        public Candidate(UUID entityId, UUID worldId, String worldName, String providerId,
                         Map<String, UUID> relationships,
                         WclOwnerEvidence.Decision ownerEvidence,
                         Map<String, BigInteger> slotValues,
                         Map<String, String> physicalSlotKeys) {
            this(entityId, worldId, worldName, providerId, relationships,
                    ownerEvidence, slotValues, physicalSlotKeys, Map.of());
        }

        public Candidate {
            Objects.requireNonNull(entityId, "verified Entity UUID");
            Objects.requireNonNull(worldId, "verified World UUID");
            if (worldName == null || worldName.isBlank()
                    || providerId == null || providerId.isBlank())
                throw new IllegalArgumentException("Missing live WCL source identity");
            Objects.requireNonNull(ownerEvidence, "local owner verdict");
            relationships = Map.copyOf(Objects.requireNonNull(relationships));
            slotValues = Map.copyOf(Objects.requireNonNull(slotValues));
            physicalSlotKeys = Map.copyOf(Objects.requireNonNull(physicalSlotKeys));
            sourceItems = Map.copyOf(Objects.requireNonNull(sourceItems, "same-read WCL items"));
            if (!sourceItems.isEmpty() && !slotValues.keySet().equals(sourceItems.keySet()))
                throw new IllegalArgumentException("WCL same-read items differ from valued slots");
            for (var item : sourceItems.entrySet())
                if (!item.getKey().equals(item.getValue().slot()))
                    throw new IllegalArgumentException("WCL source ItemStack slot mismatch");
            if (!slotValues.keySet().equals(physicalSlotKeys.keySet()))
                throw new IllegalArgumentException("WCL source slots and aliases disagree");
            for (BigInteger value : slotValues.values()) {
                if (value.signum() < 0)
                    throw new IllegalArgumentException("Negative WCL source value");
            }
        }

        public BigInteger total() {
            BigInteger sum = BigInteger.ZERO;
            for (BigInteger value : slotValues.values()) sum = sum.add(value);
            return sum;
        }
    }

    /**
     * Caller must already have selected this authoritative loaded Entity.
     * Only the real registered WCL readLoaded Success is used. Owner, monetary
     * values and slot aliases all derive from that SAME provider snapshot.
     *
     * seenWalletIds MUST be the exact shared mutable identity set of the
     * candidate generation (not a new set per Entity). Any failure requires
     * discarding that entire generation and its now-mutated identity set.
     */
    public static Candidate collect(
            Plugin consumer, Entity entity, String providerId,
            Set<UUID> seenWalletIds, int maxSerializedBytes,
            int maxInflatedBytes) throws IOException {
        return collect(consumer, entity, providerId, seenWalletIds,
                maxSerializedBytes, maxInflatedBytes, false);
    }

    static Candidate collectForHybrid(Plugin consumer, Entity entity, String providerId,
            Set<UUID> seenWalletIds, int maxSerializedBytes, int maxInflatedBytes) throws IOException {
        return collect(consumer, entity, providerId, seenWalletIds,
                maxSerializedBytes, maxInflatedBytes, true);
    }

    private static Candidate collect(Plugin consumer, Entity entity, String providerId,
            Set<UUID> seenWalletIds, int maxSerializedBytes, int maxInflatedBytes,
            boolean nativeMirrors) throws IOException {
        Objects.requireNonNull(consumer, "consumer plugin");
        Objects.requireNonNull(entity, "selected LIVE Entity");
        Objects.requireNonNull(providerId, "expected WCL provider");
        Objects.requireNonNull(seenWalletIds, "shared physical wallet identities");
        if (maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Positive WCL ItemStack budgets required");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("WCL LIVE capture requires Paper main thread");

        PaperWonderfulLoadedEvidence.Result observed =
                PaperWonderfulLoadedEvidence.readLoaded(consumer, entity, providerId);
        if (observed.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                || observed.snapshot().isEmpty())
            throw new IOException("WCL LIVE source not usable: "
                    + providerId + "/" + entity.getUniqueId()
                    + " status=" + observed.status() + " reason=" + observed.reason());
        var snapshot = observed.snapshot().orElseThrow();
        // This reuses the successful read. Calling the public readLoaded owner
        // adapter here would perform a second provider read and risk drift.
        WclOwnerEvidence.Decision owner =
                PaperWonderfulLiveOwnerEvidence.fromVerifiedLoadedSnapshot(
                        entity, providerId, snapshot);

        Map<String, BigInteger> values = nativeMirrors
                ? PaperWonderfulSnapshotAssets.slotValuesForHybrid(
                        snapshot, maxSerializedBytes, maxInflatedBytes, seenWalletIds)
                : PaperWonderfulSnapshotAssets.slotValues(
                        snapshot, maxSerializedBytes, maxInflatedBytes, seenWalletIds);
        Map<String, String> aliases = WclPhysicalSlotIdentity.keys(
                snapshot.entityId(), providerId, values.keySet());

        UUID entityId = entity.getUniqueId();
        if (!entity.isValid() || Bukkit.getEntity(entityId) != entity
                || !entityId.equals(snapshot.entityId())
                || !entity.getWorld().getUID().equals(snapshot.worldId())
                || !providerId.equals(snapshot.providerId()))
            throw new IOException("WCL LIVE Entity authority changed during candidate read");

        Map<String, PaperWonderfulLoadedEvidence.StoredItem> sameRead =
                new java.util.LinkedHashMap<>();
        for (var item : snapshot.items())
            if (sameRead.putIfAbsent(item.slot(), item) != null)
                throw new IOException("Duplicate WCL same-read ItemStack slot");
        return new Candidate(snapshot.entityId(), snapshot.worldId(),
                entity.getWorld().getName(), providerId, snapshot.relationships(),
                owner, values, aliases, sameRead);
    }
}
