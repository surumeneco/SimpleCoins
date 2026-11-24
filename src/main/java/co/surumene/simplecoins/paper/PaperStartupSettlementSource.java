package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.*;
import java.io.IOException;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.Plugin;

/** Product source-stage adapter for the managed zero-player startup window.
 * Capture live state and provider settings on primary, traverse saved sources
 * on the settlement worker. It is intentionally not the admission owner. */
public final class PaperStartupSettlementSource implements SettlementRuntime.SourceStage {
    private final Plugin plugin;
    private final WorldStorageDiscovery.Manifest manifest;
    private final WorldScope scope;
    private final BanSourceCoverage bans;
    private final PaperLiveUniverseAssets.Budget budget;
    private final int terrainChunks;
    private final Map<String, World> worlds;
    private final Map<String, UUID> worldIds;
    private final PaperBlockLockerSettings settings;
    private final PaperCapturedHybridSettlement live;

    private PaperStartupSettlementSource(Plugin plugin, Collection<World> registered,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope, BanSourceCoverage bans,
            PaperLiveUniverseAssets.Budget budget, int terrainChunks) throws IOException {
        this.plugin = Objects.requireNonNull(plugin);
        this.manifest = Objects.requireNonNull(manifest);
        this.scope = Objects.requireNonNull(scope);
        this.bans = Objects.requireNonNull(bans);
        this.budget = Objects.requireNonNull(budget);
        this.terrainChunks = terrainChunks;
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Startup source requires primary and no online players");
        if (manifest.origin() == null || !scope.equals(manifest.origin().scope()))
            throw new IOException("Startup source requires scope-matched saved discovery");
        Map<String, World> selected = new LinkedHashMap<>();
        Map<String, UUID> ids = new LinkedHashMap<>();
        for (World world : registered) {
            if (Bukkit.getWorld(world.getUID()) != world
                    || Bukkit.getWorld(world.getName()) != world
                    || ids.putIfAbsent(world.getName(), world.getUID()) != null)
                throw new IOException("Invalid registered world identity");
            if (scope.includes(world.getName())) selected.put(world.getName(), world);
        }
        if (!ids.keySet().equals(manifest.origin().worldFolders().keySet()))
            throw new IOException("Startup live/saved world scope mismatch");
        this.worlds = Map.copyOf(selected);
        this.worldIds = Map.copyOf(ids);
        this.settings = PaperBlockLockerSettings.capture();
        if (!PaperWonderfulReaderRegistry.inspect(plugin).registrationsPresent())
            throw new IOException("Required WCL provider registration unavailable");
        this.live = PaperCapturedHybridSettlement.capture(worlds.values(), budget);
        live.includeDiscoveredLoadedWcl(plugin);
    }

    public static PaperStartupSettlementSource capturePrimary(Plugin plugin, Collection<World> registered,
            WorldStorageDiscovery.Manifest manifest, WorldScope scope, BanSourceCoverage bans,
            PaperLiveUniverseAssets.Budget budget, int terrainChunks) throws IOException {
        return new PaperStartupSettlementSource(plugin, registered, manifest, scope, bans, budget, terrainChunks);
    }

    @Override public SettlementRuntime.Captured capture() throws IOException {
        if (Bukkit.isPrimaryThread()) throw new IOException("Startup saved traversal on primary");
        var terrain = settings.captureSaved(manifest, scope, budget.maxSerializedBytes(),
                budget.maxInflatedBytes(), terrainChunks);
        // The canonical terrain capture read the actual sign neighborhoods,
        // validated physical identity and retained its complete source witness.
        // assembleOwnerIndex checks provenance/dedup and rereads that witness.
        // These local proofs do not assert global writer quiescence.
        var staged = live.stageCombinedOffMain(plugin, worldIds, manifest, scope, bans, terrain,
                observed -> {
                    var signs = SavedBlockLockerSignSummary.summarize(observed.observation());
                    return new SavedBlockLockerOwnerCandidate.ExternalProof(
                            true, true, true, true, !signs.protectionSignObserved());
                },
                (source, chunk, position) -> new BlockEntitySettlementImport.Ownership(null, false, false, false),
                this::savedOwners, budget.maxSerializedBytes(), budget.maxInflatedBytes(),
                this::classifyLive, 120000);
        return new SettlementRuntime.Captured(staged, manifest);
    }

    /** Native storage fallback. A selected WCL provider's same-read decision
     * overrides its holder before the native slots are staged. */
    private Map<String, ResolvedEntitySettlementImport.Ownership> savedOwners(
            String world, Map<String, Object> chunk) throws IOException {
        var tame = SavedEntityOwner.extract(chunk);
        Map<String, ResolvedEntitySettlementImport.Ownership> result = new HashMap<>();
        for (UUID entity : SavedEntityUuidIntegrity.identities(chunk)) {
            UUID owner = tame.get(entity.toString());
            result.put(entity.toString(), new ResolvedEntitySettlementImport.Ownership(
                    owner, owner != null && bans.currentlyBanned(owner),
                    owner != null && manifest.playerDataFiles().containsKey(owner)));
        }
        return Map.copyOf(result);
    }

    private SettlementAccumulator.Asset classifyLive(HybridLiveSourceStage.Source source,
            HybridLiveSourceStage.Value value) throws IOException {
        if (!Bukkit.isPrimaryThread() || !(source instanceof HybridLiveSourceStage.ChunkSource chunk))
            throw new IOException("Startup LIVE attribution requires a primary-thread Chunk");
        World world = worlds.get(chunk.chunk().world());
        if (world == null || Bukkit.getWorld(world.getUID()) != world
                || !world.isChunkLoaded(chunk.chunk().x(), chunk.chunk().z()))
            throw new IOException("Captured LIVE chunk unavailable");
        String prefix = "world/" + world.getName() + "/entity/";
        UUID owner = null;
        boolean everyone = false;
        if (value.physicalKey().startsWith(prefix)) {
            String tail = value.physicalKey().substring(prefix.length());
            int separator = tail.indexOf('/');
            UUID id;
            try { id = UUID.fromString(separator < 0 ? tail : tail.substring(0, separator)); }
            catch (IllegalArgumentException invalid) { throw new IOException("Invalid LIVE entity UUID", invalid); }
            Entity entity = Bukkit.getEntity(id);
            if (entity == null || !entity.isValid() || entity.getWorld() != world
                    || Math.floorDiv(entity.getLocation().getBlockX(), 16) != chunk.chunk().x()
                    || Math.floorDiv(entity.getLocation().getBlockZ(), 16) != chunk.chunk().z())
                throw new IOException("Captured LIVE entity source changed");
            var wcl = live.capturedWclOwner(id);
            if (wcl.isPresent()) {
                if (wcl.get().kind() == WclOwnerEvidence.Kind.HOLD)
                    throw new IOException("Unresolved LIVE WCL owner");
                owner = wcl.get().owner();
            } else if (entity instanceof Tameable tame && tame.getOwner() != null) {
                owner = tame.getOwner().getUniqueId();
            }
        } else {
            var position = LiveContainerPhysicalSlot.parse(source, value);
            if (position.y() < world.getMinHeight() || position.y() >= world.getMaxHeight())
                throw new IOException("Captured LIVE block outside world");
            var block = world.getBlockAt(position.x(), position.y(), position.z());
            if (settings.containers().contains(block.getType().getKey().toString())) {
                var protection = BlockLockerLiveEvidence.observe(block);
                owner = protection.owner();
                everyone = protection.everyoneProtected();
            }
        }
        return new SettlementAccumulator.Asset(source.id(), value.physicalKey(), value.world(),
                owner, everyone, owner != null && bans.currentlyBanned(owner),
                owner != null && manifest.playerDataFiles().containsKey(owner),
                value.amount(), value.physicalKey());
    }
}
