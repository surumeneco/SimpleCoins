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
    private final PaperWonderfulLiveEntityDiscovery.Report initialWcl;
    private final Set<UUID> protectedRegistrations;
    private final Map<String, SettlementAccumulator.Asset> initialOwners = new HashMap<>();

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
        initialWcl = live.includeDiscoveredLoadedWcl(plugin);
        protectedRegistrations = protectedOwners(live.observed());
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
                (source, value) -> {
                    var asset = classifyLive(source, value);
                    if (initialOwners.putIfAbsent(value.physicalKey(), asset) != null)
                        throw new IOException("Repeated LIVE attribution key");
                    return asset;
                }, 120000);
        return new SettlementRuntime.Captured(staged, manifest, bans, protectedRegistrations);
    }

    /** Verify the actual selected LIVE values and provider inputs at handoff.
     * SAVED bytes were rechecked by the worker, not by a main-thread disk walk. */
    public void requireCurrentPrimary(BanSourceCoverage currentBans) throws IOException {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !bans.sameCompleteBannedPlayers(currentBans)
                || !settings.equals(PaperBlockLockerSettings.capture())
                || !PaperWonderfulReaderRegistry.inspect(plugin).registrationsPresent())
            throw new IOException("Startup attribution inputs changed");
        Map<String,UUID> currentWorlds = new HashMap<>();
        for (World world : Bukkit.getWorlds()) currentWorlds.put(world.getName(), world.getUID());
        if (!worldIds.equals(currentWorlds)) throw new IOException("World registry changed");
        var now = PaperLiveUniverseAssets.scanWithSources(worlds.values(), budget, new HashSet<>());
        if (!sourceValues(live.observed()).equals(sourceValues(now)))
            throw new IOException("LIVE monetary source changed during settlement");
        var wclNow = PaperWonderfulLiveEntityDiscovery.discover(worlds, now.sources(),
                budget.maxChunks(), budget.maxEntities());
        if (!initialWcl.identities().equals(wclNow.identities())
                || !initialWcl.observedEntityIds().equals(wclNow.observedEntityIds()))
            throw new IOException("LIVE holder population changed during settlement");
        live.requireCurrentWclPrimary(budget.maxSerializedBytes(), budget.maxInflatedBytes());
        if(!protectedRegistrations.equals(protectedOwners(now)))
            throw new IOException("LIVE protected registration changed during settlement");
        Map<String, SettlementAccumulator.Asset> currentOwners = new HashMap<>();
        for (var source : now.sources()) for (var value : source.values()) {
            if (initialOwners.containsKey(value.physicalKey()))
                currentOwners.put(value.physicalKey(), classifyLive(source, value));
        }
        if (!initialOwners.equals(currentOwners))
            throw new IOException("LIVE monetary attribution changed during settlement");
    }

    /** Must be called directly after the successful final readback, before publication. */
    public void seedNormalWcl(PaperEntityAccounting accounting) throws IOException {
        accounting.seedLoadedWcl(initialWcl.holders());
    }

    /** Positive slot values are not the only basis for registration: an
     * existing non-shared protected empty inventory also qualifies. */
    private Set<UUID> protectedOwners(PaperLiveUniverseAssets.Observed observed) throws IOException {
        if(!Bukkit.isPrimaryThread())throw new IOException("Protected registration requires primary");
        var result=new HashSet<UUID>();
        int tiles=0;
        for(var source:observed.sources()) {
            if(!(source instanceof HybridLiveSourceStage.ChunkSource chunk)) continue;
            World world=worlds.get(chunk.chunk().world());
            if(world==null||!world.isChunkLoaded(chunk.chunk().x(),chunk.chunk().z()))
                throw new IOException("Selected LIVE protected registration chunk unavailable");
            for(var tile:world.getChunkAt(chunk.chunk().x(),chunk.chunk().z()).getTileEntities()) {
                if(++tiles>budget.maxTileEntities())throw new IOException("Protected registration tile budget exhausted");
                if(!(tile instanceof org.bukkit.block.Container)
                        || !settings.containers().contains(tile.getType().getKey().toString()))continue;
                var ownership=BlockLockerLiveEvidence.observe(tile.getBlock());
                if(ownership.owner()!=null&&!ownership.everyoneProtected())result.add(ownership.owner());
            }
        }
        return Set.copyOf(result);
    }

    private static Map<String, Set<HybridLiveSourceStage.Value>> sourceValues(
            PaperLiveUniverseAssets.Observed observed) {
        var result = new HashMap<String, Set<HybridLiveSourceStage.Value>>();
        for (var source : observed.sources()) result.put(source.id(), Set.copyOf(source.values()));
        return Map.copyOf(result);
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
