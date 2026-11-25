package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import co.surumene.simplecoins.domain.WclLiveMarker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

/**
 * Bounded, read-only enumeration of all WWC/WWW PDC-marked Entities in the
 * exact loaded Chunk set previously selected by PaperLiveUniverseAssets.
 * This does not load or generate any Chunk. It does NOT prove that other
 * plugins or Paper writers were stopped, or that any unmarked custom Entity
 * can safely be treated as an empty WCL source.
 */
public final class PaperWonderfulLiveEntityDiscovery {
    private static final NamespacedKey WWC_TYPE =
            new NamespacedKey("whatawonderfulchicken", "type");
    private static final NamespacedKey WWW_TYPE =
            new NamespacedKey("whatawonderfulwolf", "type");

    private static final NamespacedKey WWC_PENDING =
            new NamespacedKey("whatawonderfulchicken", "pending_natural_founder_seed");

    private PaperWonderfulLiveEntityDiscovery() {}

    public record Identity(UUID entityId, String providerId,
                           SavedLiveSourcePartition.Chunk chunk) {
        public Identity {
            Objects.requireNonNull(entityId);
            Objects.requireNonNull(providerId);
            Objects.requireNonNull(chunk);
        }
    }

    public record Holder(Entity entity, String providerId,
                         SavedLiveSourcePartition.Chunk chunk) {
        public Holder {
            Objects.requireNonNull(entity);
            Objects.requireNonNull(providerId);
            Objects.requireNonNull(chunk);
        }
        public Identity identity() {
            return new Identity(entity.getUniqueId(), providerId, chunk);
        }
    }

    public record Report(List<Holder> holders, int scannedChunks, int scannedEntities,
                         Set<UUID> observedEntityIds) {
        public Report {
            holders = List.copyOf(Objects.requireNonNull(holders));
            observedEntityIds = Set.copyOf(Objects.requireNonNull(observedEntityIds));
            if (scannedChunks < 0 || scannedEntities < 0
                    || scannedEntities != observedEntityIds.size())
                throw new IllegalArgumentException("Invalid LIVE enumeration counters");
        }
        public boolean isCompleteSettlementSource() { return false; }
        public Set<Identity> identities() {
            Set<Identity> ids = new HashSet<>();
            for (Holder holder : holders) {
                if (!ids.add(holder.identity()))
                    throw new IllegalStateException("Duplicate WCL holder identity in report");
            }
            return Set.copyOf(ids);
        }
    }

    /** A main-thread, bounded observation of selected loaded Chunks only. */
    public static Report discover(
            Map<String, World> capturedWorlds,
            Collection<? extends HybridLiveSourceStage.Source> capturedSources,
            int maxChunks, int maxEntities) throws IOException {
        Objects.requireNonNull(capturedWorlds, "captured Paper World identities");
        Objects.requireNonNull(capturedSources, "captured LIVE sources");
        if (maxChunks < 1 || maxEntities < 1)
            throw new IllegalArgumentException("Positive LIVE discovery budgets required");
        if (!Bukkit.isPrimaryThread())
            throw new IllegalStateException("LIVE WCL enumeration requires main thread");

        Set<SavedLiveSourcePartition.Chunk> expected = new HashSet<>();
        for (var source : capturedSources) {
            if (source instanceof HybridLiveSourceStage.ChunkSource chunk) {
                if (!capturedWorlds.containsKey(chunk.chunk().world())
                        || !expected.add(chunk.chunk()))
                    throw new IOException("Duplicate or unrecognized captured LIVE Chunk");
            } else if (!(source instanceof HybridLiveSourceStage.PlayerSource)) {
                throw new IOException("Unexpected source in LIVE WCL discovery");
            }
        }
        if (expected.size() > maxChunks)
            throw new IOException("Selected LIVE Chunk count exceeds WCL discovery budget");

        Map<SavedLiveSourcePartition.Chunk, Chunk> loaded = new HashMap<>();
        for (var entry : capturedWorlds.entrySet()) {
            World world = entry.getValue();
            if (world == null || !entry.getKey().equals(world.getName())
                    || Bukkit.getWorld(entry.getKey()) != world
                    || Bukkit.getWorld(world.getUID()) != world)
                throw new IOException("Captured LIVE World identity has changed");
            Chunk[] chunks = world.getLoadedChunks();
            if (chunks == null || chunks.length > maxChunks - loaded.size())
                throw new IOException("Loaded Chunk enumeration budget exceeded");
            for (Chunk chunk : chunks) {
                if (chunk == null || chunk.getWorld() != world || !chunk.isLoaded())
                    throw new IOException("Invalid loaded Chunk during WCL enumeration");
                var key = new SavedLiveSourcePartition.Chunk(
                        world.getName(), chunk.getX(), chunk.getZ());
                if (loaded.putIfAbsent(key, chunk) != null)
                    throw new IOException("Duplicate loaded Chunk during WCL enumeration");
            }
        }
        if (!loaded.keySet().equals(expected))
            throw new IOException("Loaded Chunk population differs from captured monetary sources");

        int scanned = 0;
        Set<UUID> seenEntities = new HashSet<>();
        List<Holder> candidates = new ArrayList<>();
        for (var entry : loaded.entrySet()) {
            Chunk chunk = entry.getValue();
            Entity[] entities = chunk.getEntities();
            if (entities == null || entities.length > maxEntities - scanned)
                throw new IOException("LIVE WCL entity enumeration exceeds hard budget");
            scanned += entities.length;
            for (Entity entity : entities) {
                if (entity == null || !entity.isValid() || entity.getWorld() != chunk.getWorld())
                    throw new IOException("Invalid LIVE Entity during WCL enumeration");
                UUID uuid = entity.getUniqueId();
                if (uuid == null || !seenEntities.add(uuid))
                    throw new IOException("Duplicate or invalid LIVE Entity UUID");
                var location = entity.getLocation();
                if (location.getWorld() != chunk.getWorld()
                        || Math.floorDiv(location.getBlockX(), 16) != chunk.getX()
                        || Math.floorDiv(location.getBlockZ(), 16) != chunk.getZ())
                    throw new IOException("LIVE Entity moved outside selected Chunk");
                PersistentDataContainer pdc = entity.getPersistentDataContainer();
                Set<String> keys = new HashSet<>();
                for (NamespacedKey key : pdc.getKeys()) {
                    if (key == null || !keys.add(key.toString()))
                        throw new IOException("Invalid Entity PDC key evidence");
                }
                var provider = WclLiveMarker.provider(
                        entity.getType().getKey().toString(), keys,
                        pdc.get(WWC_TYPE, PersistentDataType.STRING),
                        pdc.get(WWW_TYPE, PersistentDataType.STRING),
                        pdc.has(WWC_PENDING, PersistentDataType.LONG));
                if (provider.isPresent()) {
                    if (Bukkit.getEntity(uuid) != entity)
                        throw new IOException("Selected WCL Entity is not Bukkit-authoritative");
                    candidates.add(new Holder(entity, provider.orElseThrow(), entry.getKey()));
                }
            }
        }
        // Detect world/Chunk churn during traversal. This is NOT a writer
        // freeze and cannot reject an A->B->A mutation between observations.
        for (var world : capturedWorlds.values()) {
            if (Bukkit.getWorld(world.getUID()) != world)
                throw new IOException("LIVE WCL World identity changed after enumeration");
            Set<SavedLiveSourcePartition.Chunk> observed = new HashSet<>();
            for (Chunk chunk : world.getLoadedChunks()) {
                if (chunk == null || chunk.getWorld() != world || !chunk.isLoaded()
                        || !observed.add(new SavedLiveSourcePartition.Chunk(
                                world.getName(), chunk.getX(), chunk.getZ())))
                    throw new IOException("Unreliable loaded Chunk re-observation");
            }
            Set<SavedLiveSourcePartition.Chunk> selected = new HashSet<>();
            for (var chunk : expected)
                if (world.getName().equals(chunk.world())) selected.add(chunk);
            if (!observed.equals(selected))
                throw new IOException("Captured LIVE Chunk set changed during WCL scan");
        }
        return new Report(candidates, loaded.size(), scanned, seenEntities);
    }
}
