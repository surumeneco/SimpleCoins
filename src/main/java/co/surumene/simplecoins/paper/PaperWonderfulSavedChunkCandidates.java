package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WclPhysicalSlotIdentity;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import co.surumene.simplecoins.domain.SavedEntityOwner;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Read-only bridge from a caller-selected saved ENTITY Anvil chunk into
 * unpublished WCL physical-slot candidates. This discovers only the
 * WWC/WWW marker scheme of the pinned distributed storage providers.
 *
 * The caller MUST independently prove all world/region discovery,
 * source-file provenance and saved-content witnesses, select SAVED rather
 * than a shadowing LIVE chunk, and maintain the same seenEntityIds set for
 * the whole selected saved-source traversal. The exact same shared wallet
 * identity set must be used for other selected saved/live monetary sources.
 * This method does not establish
 * any such proof, freeze writers, resolve ownership/BAN, close global wallet
 * or item identities, stage an accounting source or publish/admit anyone.
 */
public final class PaperWonderfulSavedChunkCandidates {
    private static final String WWC_NAMESPACE = "whatawonderfulchicken:";
    private static final String WWW_NAMESPACE = "whatawonderfulwolf:";

    private PaperWonderfulSavedChunkCandidates() {}

    /** One independently identified WCL holder; amounts include explicit 0C slots. */
    public record Candidate(
            WorldStorageDiscovery.SourceFile source, UUID worldId,
            int chunkX, int chunkZ, UUID entityId, String providerId,
            Map<String, UUID> relationships, Map<String, BigInteger> slotValues,
            Map<String, PaperWonderfulLoadedEvidence.StoredItem> sourceItems) {
        public Candidate {
            Objects.requireNonNull(source, "saved source");
            Objects.requireNonNull(worldId, "world UUID");
            Objects.requireNonNull(entityId, "Entity UUID");
            Objects.requireNonNull(providerId, "storage provider");
            relationships = Map.copyOf(Objects.requireNonNull(relationships));
            slotValues = Map.copyOf(Objects.requireNonNull(slotValues));
            sourceItems = Map.copyOf(Objects.requireNonNull(sourceItems));
            if (!sourceItems.isEmpty() && !sourceItems.keySet().equals(slotValues.keySet()))
                throw new IllegalArgumentException("WCL same-read item bytes/values disagree");
            for (var item : sourceItems.entrySet()) {
                if (!item.getKey().equals(item.getValue().slot()))
                    throw new IllegalArgumentException("WCL slot item identity mismatch");
            }
        }

        /** Source-less fixture compatibility, never sufficient for mirror proof. */
        public Candidate(WorldStorageDiscovery.SourceFile source, UUID worldId,
                         int chunkX, int chunkZ, UUID entityId, String providerId,
                         Map<String, UUID> relationships, Map<String, BigInteger> slotValues) {
            this(source, worldId, chunkX, chunkZ, entityId, providerId,
                    relationships, slotValues, Map.of());
        }

        /**
         * Names used by the normal Entity equipment staging path for the same
         * physical observation. A collision means HOLD until the two views are
         * independently proven equivalent; it must never double the amount.
         */
        public Map<String, String> physicalSlotKeys() throws IOException {
            return WclPhysicalSlotIdentity.keys(entityId, providerId, slotValues.keySet());
        }

        /**
         * Resolve only local relationship priority. The caller separately
         * proves the complete WCL provider read and vanilla/tame owner evidence;
         * this never decides BAN/playerdata, source generation or publication.
         */
        public WclOwnerEvidence.Decision ownerEvidence(
                boolean providerReadVerified, WclOwnerEvidence.TameOwner tame)
                throws IOException {
            return WclOwnerEvidence.decide(providerReadVerified, providerId, relationships, tame);
        }

        /** Candidate-owned conversion from the independently selected SAVED chunk. */
        public WclOwnerEvidence.Decision savedOwnerEvidence(
                boolean providerReadVerified, AnvilRegionReader.Chunk savedChunk)
                throws IOException {
            Objects.requireNonNull(savedChunk, "selected saved Entity chunk");
            if (savedChunk.x() != chunkX || savedChunk.z() != chunkZ)
                throw new IOException("Saved WCL owner evidence chunk mismatch");
            return ownerEvidence(providerReadVerified,
                    SavedEntityOwner.wclTameObservation(savedChunk.nbt(), entityId, providerId));
        }
    }

    /**
     * Inspect every saved Entity compound in the selected chunk, including
     * untagged vanilla animals. Unknown or damaged WWC/WWW PDC may not be
     * silently classified as non-target or as an empty inventory.
     *
     * Source and world identity originate with the caller; use only in a
     * previously verified traversal and discard the entire candidate
     * generation on IOException. This is a main-thread Paper/WCL operation.
     */
    public static List<Candidate> collect(
            Plugin consumer, WorldStorageDiscovery.SourceFile source, UUID worldId,
            AnvilRegionReader.Chunk chunk, Set<UUID> seenEntityIds,
            Set<UUID> seenWalletIds, int maxSerializedBytes, int maxInflatedBytes) throws IOException {
        Objects.requireNonNull(consumer, "plugin");
        Objects.requireNonNull(source, "saved source");
        Objects.requireNonNull(worldId, "world UUID");
        Objects.requireNonNull(chunk, "saved Entity chunk");
        Objects.requireNonNull(seenEntityIds, "cross-source saved Entity identities");
        Objects.requireNonNull(seenWalletIds, "shared physical wallet UUID identities");
        if (source.kind() != WorldStorageDiscovery.Kind.ENTITY)
            throw new IOException("WCL candidate scan requires saved ENTITY Anvil source");
        if (!Bukkit.isPrimaryThread())
            throw new IOException("WCL candidate collection requires Paper primary thread");
        if (maxSerializedBytes < 1 || maxInflatedBytes < 1)
            throw new IllegalArgumentException("Positive serialized ItemStack budgets required");
        if (!(chunk.nbt().get("Entities") instanceof List<?> entities))
            throw new IOException("Saved Entity chunk has no valid Entities list");

        List<Candidate> candidates = new ArrayList<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity))
                throw new IOException("Malformed saved Entity compound");
            if (!(entity.get("id") instanceof String entityType) || entityType.isBlank())
                throw new IOException("Saved Entity has no canonical species identifier");
            Object maybePdc = entity.get("BukkitValues");
            if (maybePdc == null) continue;
            if (!(maybePdc instanceof Map<?, ?> pdc))
                throw new IOException("Saved Entity has malformed BukkitValues");
            boolean wwc = false, www = false;
            for (Object key : pdc.keySet()) {
                if (!(key instanceof String name))
                    throw new IOException("Saved Entity BukkitValues has non-string key");
                wwc |= name.startsWith(WWC_NAMESPACE);
                www |= name.startsWith(WWW_NAMESPACE);
            }
            if (!wwc && !www) continue;
            if (wwc && www)
                throw new IOException("Saved Entity contains contradictory WWC/WWW identities");
            String provider = wwc ? "wwc" : "www";
            String expectedSpecies = wwc ? "minecraft:chicken" : "minecraft:wolf";
            String expectedMarker = wwc ? "wonderful_chicken" : "wonderful_wolf";
            String markerKey = (wwc ? WWC_NAMESPACE : WWW_NAMESPACE) + "type";
            if (!expectedSpecies.equals(entityType)
                    || !expectedMarker.equals(pdc.get(markerKey)))
                throw new IOException("Saved WCL marker/species is incomplete or contradictory");

            UUID entityId = readUuid(entity.get("UUID"));
            if (!seenEntityIds.add(entityId))
                throw new IOException("Duplicate WCL saved Entity UUID across selected chunks: "
                        + entityId);
            var restored = PaperWonderfulSavedPdc.reconstruct(
                    entityId, provider, entity, maxSerializedBytes);
            var result = PaperWonderfulLoadedEvidence.readStored(
                    consumer, entityId, worldId, provider, restored);
            if (result.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                    || result.snapshot().isEmpty())
                throw new IOException("Selected saved WCL Entity could not be read: "
                        + provider + "/" + entityId + " status=" + result.status()
                        + " reason=" + result.reason());
            var snapshot = result.snapshot().orElseThrow();
            Map<String, BigInteger> values = PaperWonderfulSnapshotAssets.slotValues(
                    snapshot, maxSerializedBytes, maxInflatedBytes, seenWalletIds);
            Map<String, PaperWonderfulLoadedEvidence.StoredItem> sameRead =
                    new java.util.LinkedHashMap<>();
            for (var item : snapshot.items()) {
                if (sameRead.putIfAbsent(item.slot(), item) != null)
                    throw new IOException("Repeated WCL stored ItemStack identity");
            }
            var candidate = new Candidate(source, worldId, chunk.x(), chunk.z(),
                    entityId, provider, snapshot.relationships(), values, sameRead);
            // Unknown provider slot identities may not silently become another
            // minted physical source. This remains an unpublished candidate.
            candidate.physicalSlotKeys();
            candidates.add(candidate);
        }
        return List.copyOf(candidates);
    }

    private static UUID readUuid(Object raw) throws IOException {
        if (!(raw instanceof int[] parts) || parts.length != 4)
            throw new IOException("Saved WCL candidate has no four-int Entity UUID");
        return new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
    }
}
