package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.BanSourceCoverage;
import co.surumene.simplecoins.domain.PaperSavedWorldMetadataWitness;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.SettlementScanRunner;
import co.surumene.simplecoins.domain.WclSavedSlotSupplement;
import co.surumene.simplecoins.domain.WclSavedEquipmentMirrorProof;
import co.surumene.simplecoins.domain.WclOwnerEvidence;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Source-local SAVED WCL extension to SettlementScanRunner. The caller must
 * independently establish the real world UUIDs for every saved-world source,
 * the matching LIVE/SAVED partition, and the source/writer epoch.
 * Never publish/ack/admit based on this local adapter.
 */
public final class PaperWonderfulSavedSettlementSource
        implements SettlementScanRunner.SavedEntitySupplement {
    private final Plugin consumer;
    private final Map<String, UUID> verifiedWorldIds;
    private final PaperSavedWorldMetadataWitness.Witness worldWitness;
    private final WorldStorageDiscovery.Manifest manifest;
    private final Set<UUID> banned;
    private final BanSourceCoverage banSources;
    private final Set<UUID> seenEntityIds;
    private final int maxSerialized;
    private final int maxInflated;

    public PaperWonderfulSavedSettlementSource(
            Plugin consumer, Map<String, UUID> verifiedWorldIds,
            WorldStorageDiscovery.Manifest manifest,
            Set<UUID> banned, BanSourceCoverage banSources,
            Set<UUID> alreadySelectedLiveWclIds,
            int maxSerialized, int maxInflated) throws IOException {
        this.consumer = Objects.requireNonNull(consumer);
        this.verifiedWorldIds = Map.copyOf(Objects.requireNonNull(verifiedWorldIds));
        this.manifest = Objects.requireNonNull(manifest);
        if (!Bukkit.isPrimaryThread())
            throw new IOException("SAVED World metadata capture requires Paper main thread");
        this.worldWitness = PaperSavedWorldMetadataWitness.capture(
                this.manifest, this.verifiedWorldIds);
        // Compare any actually loaded Bukkit World with persisted Paper
        // metadata and the WorldFolder used for saved-source discovery.
        for (var entry : worldWitness.worldIds().entrySet()) {
            World live = Bukkit.getWorld(entry.getKey());
            World byId = Bukkit.getWorld(entry.getValue());
            if (live == null) {
                if (byId != null)
                    throw new IOException("Persisted SAVED World UUID already loaded under a different name");
            } else if (live.getUID().equals(entry.getValue())
                    && byId == live
                    && manifest.origin().worldFolders().get(entry.getKey())
                            .toAbsolutePath().normalize().equals(
                                live.getWorldFolder().toPath().toAbsolutePath().normalize())) {
                // Verified same-world input, not an all-writer freeze.
            } else {
                throw new IOException("Bukkit/Paper saved World metadata identity mismatch: "
                        + entry.getKey());
            }
        }
        if ((banned == null) == (banSources == null))
            throw new IllegalArgumentException("Exactly one BAN evidence mode is required");
        this.banned = banned == null ? null : Set.copyOf(banned);
        this.banSources = banSources;
        this.seenEntityIds = new HashSet<>(Objects.requireNonNull(alreadySelectedLiveWclIds));
        if (maxSerialized < 1 || maxInflated < 1)
            throw new IllegalArgumentException("Positive saved WCL ItemStack budgets required");
        this.maxSerialized = maxSerialized;
        this.maxInflated = maxInflated;
    }

    @Override
    public void stage(WorldStorageDiscovery.SourceFile source, AnvilRegionReader.Chunk chunk,
                      SettlementAccumulator pending, Set<UUID> seenWalletIds)
            throws IOException {
        Objects.requireNonNull(source);
        Objects.requireNonNull(chunk);
        Objects.requireNonNull(pending);
        Objects.requireNonNull(seenWalletIds);
        worldWitness.requireUnchanged();
        if (!Bukkit.isPrimaryThread())
            throw new IOException("SAVED WCL readStored requires Paper main thread");
        if (source.kind() != WorldStorageDiscovery.Kind.ENTITY
                || !manifest.worldFiles().contains(source))
            throw new IOException("SAVED WCL supplement source not selected by manifest");
        UUID worldId = verifiedWorldIds.get(source.world());
        if (worldId == null)
            throw new IOException("Missing verified saved WCL World UUID: " + source.world());
        World live = Bukkit.getWorld(source.world());
        if (live != null && !worldId.equals(live.getUID()))
            throw new IOException("SAVED WCL source World UUID conflicts with loaded World");

        var candidates = PaperWonderfulSavedChunkCandidates.collect(
                consumer, source, worldId, chunk, seenEntityIds, seenWalletIds,
                maxSerialized, maxInflated);
        String sourceId = source.path().toAbsolutePath().normalize().toString();
        for (var candidate : candidates) {
            if (!source.equals(candidate.source())
                    || !worldId.equals(candidate.worldId())
                    || candidate.chunkX() != chunk.x() || candidate.chunkZ() != chunk.z())
                throw new IOException("SAVED WCL candidate provenance differs from source");
            WclOwnerEvidence.Decision owner = candidate.savedOwnerEvidence(true, chunk);
            if (owner.kind() == WclOwnerEvidence.Kind.HOLD)
                throw new IOException("Unresolved SAVED WCL owner: " + candidate.entityId());
            UUID uuid = owner.owner();
            Set<UUID> ownerBanned = uuid != null &&
                    (banSources != null
                            ? banSources.currentlyBanned(uuid)
                            : banned.contains(uuid)) ? Set.of(uuid) : Set.of();
            // Verified provider StoredItem and its monetary value came from
            // the SAME readStored Success. A positive equipment mirror can
            // only reuse an already staged matching vanilla Entity item.
            // Unknown/altered content, missing equipment or missing native
            // accounting Asset is HOLD, never a new second currency source.
            Set<String> provenEquipmentMirrors = new HashSet<>();
            for (var slot : candidate.slotValues().entrySet()) {
                boolean mirror = ("www".equals(candidate.providerId())
                        && "weapon".equals(slot.getKey()))
                        || ("wwc".equals(candidate.providerId())
                        && "head".equals(slot.getKey()));
                if (!mirror || slot.getValue().signum() == 0)
                    continue;
                var stored = candidate.sourceItems().get(slot.getKey());
                if (stored == null)
                    throw new IOException("No same-read WCL equipment bytes for mirror");
                WclSavedEquipmentMirrorProof.prove(
                        chunk.nbt(), candidate.entityId(), candidate.providerId(),
                        slot.getKey(), stored.bytes(), maxSerialized, maxInflated,
                        Bukkit.getUnsafe().getDataVersion());
                provenEquipmentMirrors.add(slot.getKey());
            }
            WclSavedSlotSupplement.stage(
                    pending, sourceId, source.world(), candidate.entityId(),
                    candidate.providerId(), candidate.slotValues(), owner,
                    ownerBanned, manifest.playerDataFiles().keySet(),
                    provenEquipmentMirrors);
        }
        worldWitness.requireUnchanged();
    }

    /**
     * The owning one-shot Hybrid scan calls this again after all saved and
     * LIVE ownership callbacks. It does not replace global writer exclusion.
     */
    public void requireWorldIdentityUnchanged() throws IOException {
        worldWitness.requireUnchanged();
    }
}
