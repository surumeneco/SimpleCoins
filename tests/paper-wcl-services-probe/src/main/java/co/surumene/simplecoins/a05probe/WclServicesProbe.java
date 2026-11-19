package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldSnapshotTraversal;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperWonderfulLoadedEvidence;
import co.surumene.simplecoins.paper.PaperWonderfulLiveOwnerEvidence;
import co.surumene.simplecoins.paper.PaperWonderfulLiveCandidates;
import co.surumene.simplecoins.paper.PaperCapturedHybridSettlement;
import co.surumene.simplecoins.paper.PaperLiveUniverseAssets;
import co.surumene.simplecoins.paper.PaperWonderfulSnapshotAssets;
import co.surumene.simplecoins.paper.PaperWonderfulSavedPdc;
import co.surumene.simplecoins.paper.PaperWonderfulSavedChunkCandidates;
import co.surumene.simplecoins.paper.PaperWonderfulSavedSettlementSource;
import co.surumene.simplecoins.paper.PaperLoadedChunkAssets;
import co.surumene.simplecoins.paper.PaperWwcDisplayPhysicalAlias;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.ResolvedEntitySettlementImport;
import co.surumene.simplecoins.domain.PaperSavedWorldMetadataWitness;
import co.surumene.www.WhatAWonderfulWolfPlugin;
import co.surumene.www.spawn.WonderfulWolfFactory;
import co.surumene.www.spawn.WonderfulWolfCreationResult;
import co.surumene.www.genome.WonderfulWolfGenomeProfile;
import co.surumene.www.founder.FounderOrigin;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.individual.ItemStackSnapshot;
import co.surumene.simplecoins.paper.WalletItemCodec;
import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.api.inventory.StorageSnapshot;
import co.surumene.wcl.plugin.inventory.WonderfulStorageReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Probe ONLY. Uses shipped WCL/WWC/WWW/WGL distribution JARs on real Paper.
 * Never reads an actual monetary owner or grants admission.
 */
public final class WclServicesProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("PAPER_A05_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                for (String pluginName : new String[]{
                        "WonderfulGenomeLib", "WonderfulCoreLibrary",
                        "WhatAWonderfulChicken", "WhatAWonderfulWolf", "SimpleCoins"}) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
                    if (plugin == null || !plugin.isEnabled())
                        throw new IllegalStateException("Expected active provider dependency: " + pluginName);
                }
                var registrations = Bukkit.getServicesManager()
                        .getRegistrations(WonderfulStorageReader.class);
                Map<String, WonderfulStorageReader> selected = new HashMap<>();
                for (RegisteredServiceProvider<WonderfulStorageReader> registered : registrations) {
                    Plugin owner = registered.getPlugin();
                    WonderfulStorageReader reader = registered.getProvider();
                    if (!owner.isEnabled()) throw new IllegalStateException("Disabled provider owner");
                    String id = reader.providerId();
                    String expectedOwner = switch (id) {
                        case "wwc" -> "WhatAWonderfulChicken";
                        case "www" -> "WhatAWonderfulWolf";
                        default -> throw new IllegalStateException("Unexpected storage provider: " + id);
                    };
                    if (!owner.getName().equals(expectedOwner))
                        throw new IllegalStateException("Wrong owner " + owner.getName() + " for " + id);
                    if (selected.putIfAbsent(id, reader) != null)
                        throw new IllegalStateException("Duplicate providerId " + id);
                }
                if (!selected.keySet().equals(Set.of("wwc", "www"))
                        || registrations.size() != 2)
                    throw new IllegalStateException(
                            "Both registered storage providers are required, got " + selected.keySet());
                getLogger().info("PAPER_A05_BOTH_PACKAGED_STORAGE_SERVICES_REGISTERED_PASSED"
                        + " providers=" + selected.keySet());

                World world = Bukkit.getWorld("world");
                if (world == null) throw new IllegalStateException("Missing test overworld");
                PersistentDataContainer blank = new ItemStack(Material.PAPER)
                        .getItemMeta().getPersistentDataContainer();
                UUID unknown = UUID.fromString("00000000-0000-0000-0000-00000000a005");
                for (String id : Set.of("wwc", "www")) {
                    WonderfulStorageReader provider = selected.get(id);
                    if (!(provider.readLoaded(unknown) instanceof StorageReadResult.NotFound))
                        throw new IllegalStateException("Untracked loaded entity not NotFound: " + id);
                    if (!(provider.readStored(unknown, world.getUID(), Optional.empty(), blank)
                            instanceof StorageReadResult.NotFound))
                        throw new IllegalStateException("Untyped stored entity not NotFound: " + id);
                }
                getLogger().info("PAPER_A05_BOTH_PACKAGED_STORAGE_READERS_EMPTY_ENTITY_PASSED");
                verifyPhysicalStorage(selected, world);
                verifyShippedWwcThreeSlotReadback(selected, world);
                verifyWwcDisplayProxyCollision(selected, world);
                verifySerializedWclValuationBoundaries(world);
                verifyCrossProviderWalletIdentity(world);
                String phase = System.getProperty("a05.probe.phase", "baseline");
                switch (phase) {
                    case "baseline" -> { }
                    case "persist" -> {
                        // Generating far terrain plus multiple plugin-backed
                        // entities and saving all worlds in ONE server tick
                        // tripped Paper's watchdog on the constrained CI host.
                        // Pre-generate the disposable far chunk asynchronously,
                        // then persist on a later main-thread callback. Do not
                        // relax watchdog limits or mutate any production world.
                        world.getChunkAtAsync(FAR_CHUNK, FAR_CHUNK, true)
                                .whenComplete((preparedChunk, asyncFailure) ->
                                        Bukkit.getScheduler().runTask(this, () -> {
                                            try {
                                                if (asyncFailure != null) {
                                                    throw new IllegalStateException(
                                                            "Async far fixture chunk generation failed",
                                                            asyncFailure);
                                                }
                                                if (preparedChunk == null || !preparedChunk.isLoaded())
                                                    throw new IllegalStateException(
                                                            "Async far fixture chunk not ready");
                                                persistChickenEntity(world, selected);
                                            } catch (Throwable failure) {
                                                getLogger().log(Level.SEVERE,
                                                        "PAPER_A05_SERVICES_PROBE_FAILED", failure);
                                            } finally {
                                                Bukkit.shutdown();
                                            }
                                        }));
                        return;
                    }
                    case "reload" -> {
                        verifyFarUnloadedStoredItems(world, selected);
                        reloadChickenEntity(world, selected);
                    }
                    default -> throw new IllegalArgumentException("Unknown A05 probe phase: " + phase);
                }
                Bukkit.shutdown();
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "PAPER_A05_SERVICES_PROBE_FAILED", failure);
                Bukkit.shutdown();
            }
        }, 50L);
    }

    /**
     * The value adapter must preserve an observed non-money zero but never
     * reinterpret malformed or duplicate physical WCL slots as zero.
     * These are detached snapshots, not a claim of source/generation proof.
     */
    private void verifySerializedWclValuationBoundaries(World world) throws IOException {
        UUID entity = UUID.fromString("00000000-0000-0000-0000-00000000a05a");
        var nonMoney = new PaperWonderfulLoadedEvidence.StoredItem(
                "non-money", new ItemStack(Material.STONE).serializeAsBytes());
        var base = new PaperWonderfulLoadedEvidence.Snapshot(
                entity, world.getUID(), "wwc", Map.of(), List.of(nonMoney));
        Map<String, BigInteger> observed = PaperWonderfulSnapshotAssets.slotValues(
                base, 2 * 1024 * 1024, 16 * 1024 * 1024);
        if (observed.size() != 1 || !BigInteger.ZERO.equals(observed.get("non-money")))
            throw new IOException("Non-money WCL slot is not positively represented as zero");

        var duplicate = new PaperWonderfulLoadedEvidence.Snapshot(
                entity, world.getUID(), "wwc", Map.of(), List.of(nonMoney, nonMoney));
        try {
            PaperWonderfulSnapshotAssets.slotValues(
                    duplicate, 2 * 1024 * 1024, 16 * 1024 * 1024);
            throw new IOException("Duplicate physical WCL slot was accepted");
        } catch (IOException expected) {
            if (!"Invalid or duplicate WCL source slot".equals(expected.getMessage()))
                throw expected;
        }

        var bad = new PaperWonderfulLoadedEvidence.Snapshot(
                entity, world.getUID(), "www", Map.of(),
                List.of(new PaperWonderfulLoadedEvidence.StoredItem(
                        "damaged", new byte[]{0, 0})));
        try {
            PaperWonderfulSnapshotAssets.slotValues(
                    bad, 2 * 1024 * 1024, 16 * 1024 * 1024);
            throw new IOException("Malformed serialized WCL item was accepted");
        } catch (IOException expected) {
            if ("Malformed serialized WCL item was accepted".equals(expected.getMessage()))
                throw expected;
        }
        getLogger().info("PAPER_A05_PRODUCT_WCL_VALUE_ZERO_AND_MALFORMED_REJECTION_PASSED");
    }

    /**
     * Synthetic storage snapshots containing REAL Paper wallet bytes test
     * the cross-WCL identity context. This is not a naturally persisted
     * WWC/WWW wallet and not complete global source deduplication.
     */
    private void verifyCrossProviderWalletIdentity(World world) throws IOException {
        UUID walletId = UUID.fromString("00000000-0000-0000-0000-00000000a065");
        var codec = new WalletItemCodec(() -> walletId, "C");
        ItemStack wallet = codec.withBalance(codec.create(), 7000);
        byte[] bytes = wallet.serializeAsBytes();
        var wwc = new PaperWonderfulLoadedEvidence.Snapshot(
                UUID.fromString("00000000-0000-0000-0000-00000000a066"),
                world.getUID(), "wwc", Map.of(),
                List.of(new PaperWonderfulLoadedEvidence.StoredItem("head", bytes)));
        var www = new PaperWonderfulLoadedEvidence.Snapshot(
                UUID.fromString("00000000-0000-0000-0000-00000000a067"),
                world.getUID(), "www", Map.of(),
                List.of(new PaperWonderfulLoadedEvidence.StoredItem("weapon", bytes)));

        java.util.Set<UUID> shared = new java.util.HashSet<>();
        Map<String, BigInteger> first = PaperWonderfulSnapshotAssets.slotValues(
                wwc, 2 * 1024 * 1024, 16 * 1024 * 1024, shared);
        if (!BigInteger.valueOf(7000).equals(first.get("head"))
                || !shared.equals(Set.of(walletId)))
            throw new IOException("First WCL physical wallet was not valued 7000C");
        try {
            PaperWonderfulSnapshotAssets.slotValues(
                    www, 2 * 1024 * 1024, 16 * 1024 * 1024, shared);
            throw new IOException("Cross-provider cloned wallet UUID was counted twice");
        } catch (IOException duplicated) {
            if (!duplicated.getMessage().startsWith("Duplicate physical wallet UUID: "))
                throw duplicated;
        }

        UUID distinctId = UUID.fromString("00000000-0000-0000-0000-00000000a068");
        var otherCodec = new WalletItemCodec(() -> distinctId, "C");
        byte[] distinct = otherCodec.withBalance(otherCodec.create(), 3000)
                .serializeAsBytes();
        var other = new PaperWonderfulLoadedEvidence.Snapshot(
                www.entityId(), world.getUID(), "www", Map.of(),
                List.of(new PaperWonderfulLoadedEvidence.StoredItem("weapon", distinct)));
        Map<String, BigInteger> second = PaperWonderfulSnapshotAssets.slotValues(
                other, 2 * 1024 * 1024, 16 * 1024 * 1024, shared);
        if (!BigInteger.valueOf(3000).equals(second.get("weapon"))
                || !shared.equals(Set.of(walletId, distinctId)))
            throw new IOException("Independent WCL wallet identity was not preserved");
        getLogger().info("PAPER_A05_PRODUCT_CROSS_WCL_CLONED_WALLET_REJECTED_PASSED");
    }

    /**
     * Actual loaded Paper Wolf: the same 10000C is present in native
     * equipment.mainhand and one WCL weapon read, while 5000C is independent
     * WCL cargo. The source is physically captured before the provider read.
     * It is an UNPUBLISHED staged candidate, not an all-writer G0 guarantee.
     */
    private void verifyRealLiveWolfEquipmentMirror(World world) throws Exception {
        var coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        UUID owner = UUID.fromString("00000000-0000-0000-0000-00000000a052");
        UUID commander = UUID.fromString("00000000-0000-0000-0000-00000000a053");
        var www = (WhatAWonderfulWolfPlugin) Bukkit.getPluginManager().getPlugin("WhatAWonderfulWolf");
        if (www == null || !www.isEnabled())
            throw new IOException("Shipped WWW runtime plugin not enabled");
        // WWW's public WCL readLoaded only observes registered, genotype-backed
        // WonderfulWolfIndividuals. Seeding a vanilla Wolf's PDC alone is NOT
        // a valid LIVE source (readLoaded would correctly return NotFound).
        // Create an actual initialized founder with the shipped WWW/WGL engine.
        var factory = new WonderfulWolfFactory(www.genomeLib().engine(),
                () -> new WonderfulWolfGenomeProfile(
                        www.currentConfig(), www.genomeLib().engine().geneSequenceCodec()));
        var creation = factory.createFounder(FounderOrigin.NATURAL,
                Optional.of(owner), 0L, 0xA05A05L);
        if (!(creation instanceof WonderfulWolfCreationResult.Success made))
            throw new IOException("Actual WWW/WGL founder synthesis failed: " + creation);
        ItemStack nativeMoney = coins.create(CoinDenomination.TEN_THOUSAND, 1);
        // The SHIPPED WWW weapon renderer sets the physical main-hand item
        // unbreakable. Make the test source item already carry that same
        // native item metadata rather than weakening the NBT equality rule.
        var meta = nativeMoney.getItemMeta();
        meta.setUnbreakable(true);
        nativeMoney.setItemMeta(meta);
        ItemStack cargo = coins.create(CoinDenomination.THOUSAND, 5);
        Wolf wolf = world.spawn(new Location(world, 35.5, 120, 35.5), Wolf.class);
        try {
            wolf.setOwner(Bukkit.getOfflinePlayer(owner));
            var individual = made.individual()
                    .withTamedOwner(owner)
                    .withCommandState(Mode.FOLLOW, Optional.of(commander),
                            ActionDistance.NORMAL, Optional.empty())
                    .withStorage(
                            Optional.of(new ItemStackSnapshot(nativeMoney.serializeAsBytes())),
                            Map.of(7, new ItemStackSnapshot(cargo.serializeAsBytes())));
            www.loadedIndividuals().saveAndRegister(wolf, individual);
            if (!www.loadedIndividuals().entity(wolf.getUniqueId()).filter(w -> w == wolf).isPresent())
                throw new IOException("WWW real loaded registry does not contain created founder");
            if (!wolf.isValid() || Bukkit.getEntity(wolf.getUniqueId()) != wolf)
                throw new IOException("Managed WWW Wolf is not the authoritative loaded Paper Entity");

            var candidate = PaperWonderfulLiveCandidates.collect(
                    this, wolf, "www", new java.util.HashSet<>(),
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            if (!commander.equals(candidate.ownerEvidence().owner())
                    || !BigInteger.valueOf(10000).equals(candidate.slotValues().get("weapon"))
                    || !BigInteger.valueOf(5000).equals(candidate.slotValues().get("cargo:7")))
                throw new IOException("Product same-read WWW LIVE owner/coin source mismatch");

            var capture = PaperCapturedHybridSettlement.capture(
                    List.of(world),
                    new PaperLiveUniverseAssets.Budget(
                            1, 4096, 4096, 4096, 131072, 1,
                            2 * 1024 * 1024, 16 * 1024 * 1024));
            capture.includeLoadedWcl(this, wolf, "www",
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            var staged = capture.stageCandidate(
                    new WorldStorageDiscovery.Manifest(List.of(), Map.of()),
                    new WorldScope(true, Set.of(), Set.of()), Set.of(),
                    (sourceWorld, position) -> null,
                    (sourceWorld, savedChunk) -> Map.of(),
                    2 * 1024 * 1024, 16 * 1024 * 1024,
                    (source, value) -> new SettlementAccumulator.Asset(
                            source.id(), value.physicalKey(), value.world(),
                            value.physicalKey().equals("world/" + world.getName()
                                    + "/entity/" + wolf.getUniqueId() + "/equipment/mainhand")
                                    ? commander : null,
                            false, false, false,
                            value.amount(), value.physicalKey()));
            var result = staged.pending().commit();
            if (!BigInteger.valueOf(15000).equals(result.dead())
                    || !BigInteger.ZERO.equals(result.statistics().worldTotal())
                    || result.deadLocations().stream().noneMatch(location ->
                            commander.equals(location.owner())
                            && BigInteger.valueOf(10000).equals(location.value())))
                throw new IOException("LIVE WCL/native WWW equipment counted more than once or lost"
                        + " DEAD=" + result.dead()
                        + " worldTotal=" + result.statistics().worldTotal());
            getLogger().info("PAPER_A05_LIVE_NATIVE_WWW_MIRROR_ONCE_15000_PASSED");
        } finally {
            www.loadedIndividuals().unregister(wolf);
            wolf.remove();
        }
    }

    /**
     * Actual serialized ItemStacks and live Bukkit PDC; the World is disposable.
     * This is a provider-read fixture, not a real economy snapshot.
     */
    private void verifyPhysicalStorage(Map<String, WonderfulStorageReader> providers,
                                       World world) throws Exception {
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack money = coins.create(CoinDenomination.FIVE_THOUSAND, 2);
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        if (!(shulker.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox box))
            throw new IllegalStateException("Paper ShulkerBox BlockStateMeta missing");
        box.getInventory().setItem(0, money);
        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        byte[] storedShulker = shulker.serializeAsBytes();
        UUID chickenCommander = UUID.fromString("00000000-0000-0000-0000-00000000a051");
        // The TEST fixture may load its own disposable chunk before spawning.
        // A production reader must not cause chunk generation or synchronous loads.
        world.getChunkAt(2, 2);
        if (!world.isChunkLoaded(2, 2))
            throw new IllegalStateException("Fixture chunk failed to load");
        Chicken chicken = world.spawn(new Location(world, 32.5, 120, 32.5), Chicken.class);
        try {
            seedChickenStorage(chicken.getPersistentDataContainer(), chickenCommander, storedShulker);
            WonderfulStorageReader wwc = providers.get("wwc");
            // Distinguish actual Bukkit entity indexing from PDC decoder behavior.
            var loadedEntity = Bukkit.getEntity(chicken.getUniqueId());
            var rawType = chicken.getPersistentDataContainer().get(
                    new NamespacedKey("whatawonderfulchicken", "type"), PersistentDataType.STRING);
            boolean indexed = loadedEntity != null;
            boolean valid = chicken.isValid();
            boolean loadedChunk = world.isChunkLoaded(2, 2);
            getLogger().info("PAPER_A05_WWC_LOADED_DIAGNOSTIC"
                    + " indexed=" + indexed + " isValid=" + valid
                    + " chunkLoaded=" + loadedChunk + " type=" + rawType);
            if (!indexed || !valid || !loadedChunk || !"wonderful_chicken".equals(rawType))
                throw new IllegalStateException("Fixture must be registered before testing WWC LIVE");
            StorageSnapshot saved = success(wwc.readStored(chicken.getUniqueId(), world.getUID(),
                    Optional.of(new StorageSnapshot.Position(
                            chicken.getLocation().getX(), chicken.getLocation().getY(), chicken.getLocation().getZ())),
                    chicken.getPersistentDataContainer()), "WWC stored");
            StorageSnapshot live = success(wwc.readLoaded(chicken.getUniqueId()), "WWC loaded");
            assertChickenStorage(live, chicken.getUniqueId(), world.getUID(),
                    chickenCommander, storedShulker, coins);
            assertChickenStorage(saved, chicken.getUniqueId(), world.getUID(),
                    chickenCommander, storedShulker, coins);
            if (!live.items().equals(saved.items()) || !live.relationships().equals(saved.relationships()))
                throw new IllegalStateException("WWC LIVE and stored reader disagree for same entity");
            // Product collector, not this independent observer, must preserve
            // the real loaded WWC source with exact identities and serialized bytes.
            var product = PaperWonderfulLoadedEvidence.readLoaded(this, chicken, "wwc");
            if (product.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                    || product.snapshot().isEmpty())
                throw new IllegalStateException("Product WCL loaded collector failed: " + product);
            var candidate = product.snapshot().orElseThrow();
            if (!candidate.entityId().equals(chicken.getUniqueId())
                    || !candidate.worldId().equals(world.getUID())
                    || !"wwc".equals(candidate.providerId())
                    || !candidate.relationships().equals(live.relationships())
                    || candidate.items().size() != live.items().size()
                    || candidate.items().stream().noneMatch(item ->
                            "shulker".equals(item.slot())
                            && Arrays.equals(storedShulker, item.bytes())))
                throw new IllegalStateException("Product WCL candidate source/evidence mismatch");
            var moneyBySlot = PaperWonderfulSnapshotAssets.slotValues(
                    candidate, 2 * 1024 * 1024, 16 * 1024 * 1024);
            if (moneyBySlot.size() != 1
                    || !BigInteger.valueOf(10000).equals(moneyBySlot.get("shulker")))
                throw new IllegalStateException("Product WWC nested shulker money mismatch: "
                        + moneyBySlot);
            getLogger().info("PAPER_A05_PRODUCT_LOADED_WWC_SHULKER_VALUE_10000_PASSED");
            // Actual same-Entity LIVE ownership evidence, not a synthetic map.
            // This regular Chicken has no vanilla tame owner, and the real
            // WCL follow_target must therefore take ownership priority.
            var localOwner = PaperWonderfulLiveOwnerEvidence.readLoaded(this, chicken, "wwc");
            if (localOwner.kind() != co.surumene.simplecoins.domain.WclOwnerEvidence.Kind.OWNER
                    || !chickenCommander.equals(localOwner.owner()))
                throw new IllegalStateException("LIVE WCL Chicken owner candidate mismatch");
            var nonTargetOwner = PaperWonderfulLiveOwnerEvidence.readLoaded(this, chicken, "www");
            if (nonTargetOwner.kind() != co.surumene.simplecoins.domain.WclOwnerEvidence.Kind.HOLD)
                throw new IllegalStateException("Wrong WCL provider was adopted for LIVE owner");
            getLogger().info("PAPER_A05_PRODUCT_LIVE_OWNER_EVIDENCE_PASSED");
            // The new product bridge must derive value, physical alias, and
            // owner from its one real WCL read, not from separate snapshots.
            var joined = PaperWonderfulLiveCandidates.collect(
                    this, chicken, "wwc", new java.util.HashSet<>(),
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            if (!joined.entityId().equals(chicken.getUniqueId())
                    || !joined.worldId().equals(world.getUID())
                    || !"world".equals(joined.worldName())
                    || !joined.relationships().equals(live.relationships())
                    || joined.ownerEvidence().kind()
                            != co.surumene.simplecoins.domain.WclOwnerEvidence.Kind.OWNER
                    || !chickenCommander.equals(joined.ownerEvidence().owner())
                    || !BigInteger.valueOf(10000).equals(joined.total())
                    || !BigInteger.valueOf(10000).equals(joined.slotValues().get("shulker"))
                    || !("entity/" + chicken.getUniqueId() + "/wcl/wwc/shulker")
                            .equals(joined.physicalSlotKeys().get("shulker")))
                throw new IllegalStateException("Product LIVE WCL joined candidate mismatch");
            try {
                PaperWonderfulLiveCandidates.collect(
                        this, chicken, "www", new java.util.HashSet<>(),
                        2 * 1024 * 1024, 16 * 1024 * 1024);
                throw new IllegalStateException("Wrong provider became a WCL asset");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("WCL LIVE source not usable: "))
                    throw expected;
            }
            getLogger().info("PAPER_A05_PRODUCT_SINGLE_READ_LIVE_OWNER_VALUE_ALIAS_PASSED");
            // Test the product's actual loaded-Chunk capture and its shared
            // WCL wallet generation. The result is *not* a publishable G0
            // settlement or proof that every WWC/WWW Entity was discovered.
            var capturedMoney = PaperCapturedHybridSettlement.capture(
                    List.of(world),
                    new PaperLiveUniverseAssets.Budget(
                            1, 4096, 4096, 4096, 131072, 1,
                            2 * 1024 * 1024, 16 * 1024 * 1024));
            capturedMoney.includeLoadedWcl(
                    this, chicken, "wwc", 2 * 1024 * 1024, 16 * 1024 * 1024);
            var localStage = capturedMoney.stageCandidate(
                    new WorldStorageDiscovery.Manifest(List.of(), Map.of()),
                    new WorldScope(true, Set.of(), Set.of()), Set.of(),
                    (sourceWorld, position) -> null,
                    (sourceWorld, savedChunk) -> Map.of(),
                    2 * 1024 * 1024, 16 * 1024 * 1024,
                    (source, value) -> new SettlementAccumulator.Asset(
                            source.id(), value.physicalKey(), value.world(), null,
                            false, false, false, value.amount(), value.physicalKey()));
            var stagedMoney = localStage.pending().commit();
            if (!stagedMoney.dead().equals(BigInteger.valueOf(10000))
                    || !stagedMoney.deadLocations().stream().anyMatch(location ->
                            location.owner().equals(chickenCommander)
                            && location.value().equals(BigInteger.valueOf(10000))))
                throw new IllegalStateException("Paper hybrid WCL 10000C candidate was not staged");
            getLogger().info("PAPER_A05_PRODUCT_WCL_HYBRID_UNPUBLISHED_STAGE_PASSED");
            // Enumerate the ACTUAL loaded Chunk Entity population rather than
            // handing the Chicken to the capture. This remains only a local
            // unpublished LIVE-source candidate, not a frozen G0 generation.
            var autoMoney = PaperCapturedHybridSettlement.capture(
                    List.of(world),
                    new PaperLiveUniverseAssets.Budget(
                            1, 4096, 4096, 4096, 131072, 1,
                            2 * 1024 * 1024, 16 * 1024 * 1024));
            var found = autoMoney.includeDiscoveredLoadedWcl(this);
            if (found.scannedChunks() < 1
                    || found.scannedEntities() < 1
                    || found.holders().stream().noneMatch(h ->
                            h.entity().getUniqueId().equals(chicken.getUniqueId())
                            && "wwc".equals(h.providerId()))
                    || found.isCompleteSettlementSource())
                throw new IllegalStateException("Automatically enumerated WCL holder missing");
            var autoStaged = autoMoney.stageCandidate(
                    new WorldStorageDiscovery.Manifest(List.of(), Map.of()),
                    new WorldScope(true, Set.of(), Set.of()), Set.of(),
                    (sourceWorld, position) -> null,
                    (sourceWorld, savedChunk) -> Map.of(),
                    2 * 1024 * 1024, 16 * 1024 * 1024,
                    (source, value) -> new SettlementAccumulator.Asset(
                            source.id(), value.physicalKey(), value.world(), null,
                            false, false, false, value.amount(), value.physicalKey()));
            var autoResult = autoStaged.pending().commit();
            if (!autoResult.deadLocations().stream().anyMatch(location ->
                    location.owner().equals(chickenCommander)
                    && location.value().equals(BigInteger.valueOf(10000))))
                throw new IllegalStateException("Automatic WCL holder not handed to Hybrid staging");
            getLogger().info("PAPER_A05_AUTO_DISCOVERED_LOADED_WCL_STAGE_PASSED");
            // A non-target service returning NotFound cannot be treated as
            // proof of zero money in any other world/provider.
            var wrongProvider = PaperWonderfulLoadedEvidence.readLoaded(this, chicken, "www");
            if (wrongProvider.status() != PaperWonderfulLoadedEvidence.Status.NOT_FOUND
                    || wrongProvider.snapshot().isPresent())
                throw new IllegalStateException("Non-target WCL result must not be adopted");
            getLogger().info("PAPER_A05_PRODUCT_LOADED_WWC_SOURCE_PROOF_PASSED");
            getLogger().info("PAPER_A05_WWC_LIVE_STORED_SHULKER_COIN_PASSED");
        } finally {
            chicken.remove();
        }
        // The isolated provider test also exercises the product's complete
        // LIVE capture → readLoaded → overlay → source staging path.
        verifyRealLiveWolfEquipmentMirror(world);

        UUID wolfOwner = UUID.fromString("00000000-0000-0000-0000-00000000a052");
        UUID wolfCommander = UUID.fromString("00000000-0000-0000-0000-00000000a053");
        UUID wolfEntity = UUID.fromString("00000000-0000-0000-0000-00000000a054");
        ItemStack weapon = coins.create(CoinDenomination.TEN_THOUSAND, 1);
        ItemStack cargo = coins.create(CoinDenomination.THOUSAND, 5);
        PersistentDataContainer wolfPdc = new ItemStack(Material.PAPER)
                .getItemMeta().getPersistentDataContainer();
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "type"),
                PersistentDataType.STRING, "wonderful_wolf");
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                PersistentDataType.INTEGER, 1);
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "runtime_state"),
                PersistentDataType.BYTE_ARRAY,
                wolfRuntimeFixture(wolfOwner, wolfCommander,
                        weapon.serializeAsBytes(), cargo.serializeAsBytes()));
        WonderfulStorageReader www = providers.get("www");
        StorageSnapshot storedWolf = success(www.readStored(
                wolfEntity, world.getUID(), Optional.empty(), wolfPdc), "WWW stored");
        if (!wolfEntity.equals(storedWolf.entityId())
                || !world.getUID().equals(storedWolf.worldId())
                || !wolfCommander.equals(storedWolf.relationships().get("commander"))
                || !wolfOwner.equals(storedWolf.relationships().get("owner"))
                || storedWolf.items().size() != 2)
            throw new IllegalStateException("WWW relationship/slot identity mismatch " + storedWolf);
        for (var storedItem : storedWolf.items()) {
            if ("weapon".equals(storedItem.slot())) {
                assertCoin(storedItem.bytes(), coins, CoinDenomination.TEN_THOUSAND, 1);
            } else if ("cargo:7".equals(storedItem.slot())) {
                assertCoin(storedItem.bytes(), coins, CoinDenomination.THOUSAND, 5);
            } else {
                throw new IllegalStateException("Unexpected WWW storage slot " + storedItem.slot());
            }
        }
        getLogger().info("PAPER_A05_WWW_STORED_OWNER_COMMANDER_CURRENCY_PASSED");

        PersistentDataContainer invalidChicken = new ItemStack(Material.PAPER)
                .getItemMeta().getPersistentDataContainer();
        seedChickenStorage(invalidChicken, chickenCommander, storedShulker);
        invalidChicken.set(new NamespacedKey("whatawonderfulchicken", "data_version"),
                PersistentDataType.INTEGER, 98);
        requireUnsupported(providers.get("wwc").readStored(
                wolfEntity, world.getUID(), Optional.empty(), invalidChicken), "WWC");
        var brokenChicken = PaperWonderfulLoadedEvidence.readStored(
                this, wolfEntity, world.getUID(), "wwc", invalidChicken);
        if (brokenChicken.status() != PaperWonderfulLoadedEvidence.Status.FAILURE
                || brokenChicken.snapshot().isPresent())
            throw new IllegalStateException("Product WCL must not adopt unsupported WWC schema");

        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                PersistentDataType.INTEGER, 98);
        requireUnsupported(www.readStored(
                wolfEntity, world.getUID(), Optional.empty(), wolfPdc), "WWW");
        var brokenWolf = PaperWonderfulLoadedEvidence.readStored(
                this, wolfEntity, world.getUID(), "www", wolfPdc);
        if (brokenWolf.status() != PaperWonderfulLoadedEvidence.Status.FAILURE
                || brokenWolf.snapshot().isPresent())
            throw new IllegalStateException("Product WCL must not adopt unsupported WWW schema");
        getLogger().info("PAPER_A05_PRODUCT_STORED_FAILURES_NOT_ADOPTED_PASSED");
        getLogger().info("PAPER_A05_BOTH_STORED_SCHEMA_FAILURES_FAIL_CLOSED_PASSED");
    }

    /**
     * Shipped WWC exposes three PDC-backed items, not only the previously
     * tested shulker. Detached, explicitly synthetic Paper PDC: read-only
     * provider contract check, not a game-managed WWC lifecycle guarantee.
     */
    private void verifyShippedWwcThreeSlotReadback(
            Map<String, WonderfulStorageReader> providers, World world) throws IOException {
        UUID chicken = UUID.fromString("00000000-0000-0000-0000-00000000a06c");
        var coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        var pdc = new ItemStack(Material.PAPER).getItemMeta().getPersistentDataContainer();
        seedChickenStorage(pdc, PERSISTENCE_FOLLOW, createMoneyShulker());
        pdc.set(new NamespacedKey("whatawonderfulchicken", "equipment_carpet"),
                PersistentDataType.BYTE_ARRAY,
                new ItemStack(Material.RED_CARPET).serializeAsBytes());
        pdc.set(new NamespacedKey("whatawonderfulchicken", "equipment_head"),
                PersistentDataType.BYTE_ARRAY,
                coins.create(CoinDenomination.FIVE_THOUSAND, 1).serializeAsBytes());
        StorageSnapshot direct = success(
                providers.get("wwc").readStored(chicken, world.getUID(), Optional.empty(), pdc),
                "Shipped WWC all three slots");
        if (!chicken.equals(direct.entityId()) || direct.items().size() != 3
                || !PERSISTENCE_FOLLOW.equals(direct.relationships().get("follow_target")))
            throw new IOException("Shipped WWC three-slot snapshot incomplete");
        var result = PaperWonderfulLoadedEvidence.readStored(this, chicken, world.getUID(),
                "wwc", pdc);
        if (result.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS)
            throw new IOException("Product WCL adapter rejected three-slot WWC: " + result);
        var snapshot = result.snapshot().orElseThrow();
        Map<String, BigInteger> values = PaperWonderfulSnapshotAssets.slotValues(
                snapshot, 2 * 1024 * 1024, 16 * 1024 * 1024);
        if (!values.keySet().equals(Set.of("carpet", "shulker", "head"))
                || !BigInteger.ZERO.equals(values.get("carpet"))
                || !BigInteger.valueOf(10000).equals(values.get("shulker"))
                || !BigInteger.valueOf(5000).equals(values.get("head")))
            throw new IOException("Shipped WWC three-slot monetary valuation incomplete");
        var keys = co.surumene.simplecoins.domain.WclPhysicalSlotIdentity.keys(
                chicken, "wwc", values.keySet());
        String entity = "entity/" + chicken;
        if (!(entity + "/wcl/wwc/carpet").equals(keys.get("carpet"))
                || !(entity + "/wcl/wwc/shulker").equals(keys.get("shulker"))
                || !(entity + "/equipment/head").equals(keys.get("head")))
            throw new IOException("Shipped WWC head/carpet/shulker physical aliases incomplete");
        getLogger().info("PAPER_A05_REAL_WWC_THREE_SLOTS_HEAD_ALIAS_PASSED total=15000");
    }

    /**
     * Isolated Paper 26.2: a stand carrying the exact shipped WWC
     * display_owner/display_role marker pair visually mirrors a real Chicken's
     * provider-readable money shulker. This is not a game-managed display
     * lifecycle test or a globally frozen accounting epoch.
     */
    private void verifyWwcDisplayProxyCollision(
            Map<String, WonderfulStorageReader> providers, World world) throws IOException {
        // This CI world need not hold spawn-chunk tickets. An Entity spawned
        // into an unloaded Chunk can be returned with valid=false and no
        // Bukkit UUID lookup. Load the disposable fixture Chunk FIRST.
        Chunk fixtureChunk = world.getChunkAt(0, 0);
        if (!fixtureChunk.isLoaded())
            throw new IOException("WWC display probe fixture Chunk did not load");
        Chicken owner = world.spawn(new Location(world, 8.5, 120, 8.5), Chicken.class);
        ArmorStand stand = null;
        try {
            byte[] moneyShulker = createMoneyShulker();
            seedChickenStorage(owner.getPersistentDataContainer(),
                    PERSISTENCE_FOLLOW, moneyShulker);
            // A synthetically tagged, newly spawned Chicken is not yet a
            // WWC-managed loaded individual. Use the shipped saved-PDC reader
            // rather than assuming readLoaded will register a game individual.
            var wcl = success(providers.get("wwc").readStored(owner.getUniqueId(),
                    world.getUID(), Optional.empty(), owner.getPersistentDataContainer()),
                    "WWC display original stored-PDC source");
            if (wcl.items().size() != 1 || !"shulker".equals(wcl.items().getFirst().slot()))
                throw new IOException("WWC display parent shulker source missing");
            var original = PaperWonderfulLoadedEvidence.readStored(this,
                    owner.getUniqueId(), world.getUID(), "wwc",
                    owner.getPersistentDataContainer());
            if (original.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS)
                throw new IOException("Product WCL could not read stored display parent shulker");
            var values = PaperWonderfulSnapshotAssets.slotValues(
                    original.snapshot().orElseThrow(), 2 * 1024 * 1024, 16 * 1024 * 1024);
            if (!BigInteger.valueOf(10000).equals(values.get("shulker")))
                throw new IOException("WWC display parent monetary source changed");
            stand = world.spawn(new Location(world, 9.0, 120, 9.0), ArmorStand.class);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setPersistent(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.getPersistentDataContainer().set(
                    new NamespacedKey("whatawonderfulchicken", "display_owner"),
                    PersistentDataType.STRING, owner.getUniqueId().toString());
            stand.getPersistentDataContainer().set(
                    new NamespacedKey("whatawonderfulchicken", "display_role"),
                    PersistentDataType.STRING, "SHULKER_BOX");
            stand.getEquipment().setHelmet(ItemStack.deserializeBytes(moneyShulker));
            String expected = "world/" + world.getName() + "/entity/" + owner.getUniqueId()
                    + "/wcl/wwc/shulker";
            var registryEntity = Bukkit.getEntity(owner.getUniqueId());
            getLogger().info("PAPER_A05_WWC_DISPLAY_PARENT_DIAGNOSTIC"
                    + " ownerValid=" + owner.isValid()
                    + " registryFound=" + (registryEntity != null)
                    + " registryChicken=" + (registryEntity instanceof Chicken)
                    + " registryWorldSame=" + (registryEntity != null
                            && registryEntity.getWorld() == stand.getWorld())
                    + " registryValid=" + (registryEntity != null && registryEntity.isValid())
                    + " pdcType=" + owner.getPersistentDataContainer().get(
                            new NamespacedKey("whatawonderfulchicken", "type"),
                            PersistentDataType.STRING)
                    + " standValid=" + stand.isValid()
                    + " standPersistent=" + stand.isPersistent()
                    + " standMarker=" + stand.isMarker()
                    + " standInvisible=" + stand.isInvisible()
                    + " standInvulnerable=" + stand.isInvulnerable());
            String physical = PaperWwcDisplayPhysicalAlias.verifiedPhysicalKey(
                    stand, "head", world.getName());
            if (!expected.equals(physical))
                throw new IOException("WWC display mirrored inventory physical identity mismatch");
            var chunk = stand.getLocation().getChunk();
            var source = PaperLoadedChunkAssets.scan(chunk, 10000, 10000,
                    20000, 2 * 1024 * 1024, 16 * 1024 * 1024);
            long hits = source.entries().stream().filter(e ->
                    expected.equals(e.physicalKey())
                            && BigInteger.valueOf(10000).equals(e.amount())).count();
            if (hits != 1)
                throw new IOException("Paper live scanner ignored or duplicated WWC display alias");
            var candidate = new SettlementAccumulator(
                    new WorldScope(true, Set.of(), Set.of()), Set.of("wcl", "visual"));
            candidate.accept(new SettlementAccumulator.Asset("wcl",
                    "entity/" + owner.getUniqueId() + "/wcl/wwc/shulker",
                    world.getName(), null, false, false, false,
                    BigInteger.valueOf(10000), "wwc-original"));
            try {
                candidate.accept(new SettlementAccumulator.Asset("visual", expected,
                        world.getName(), null, false, false, false,
                        BigInteger.valueOf(10000), "wwc-projection"));
                throw new IOException("Visual proxy illegally doubled a WWC stored asset");
            } catch (IllegalArgumentException rejected) {
                // Expected collision; partial candidate must never be committed.
            }
            candidate.completeSource("wcl");
            candidate.completeSource("visual");
            try {
                candidate.commit();
                throw new IOException("Rejected WWC proxy still allowed settlement commit");
            } catch (IllegalStateException blocked) {
                // Fail-closed.
            }
            stand.getPersistentDataContainer().set(
                    new NamespacedKey("whatawonderfulchicken", "display_role"),
                    PersistentDataType.STRING, "UNKNOWN_ROLE");
            try {
                PaperWwcDisplayPhysicalAlias.verifiedPhysicalKey(
                        stand, "head", world.getName());
                throw new IOException("Unknown WWC visual projection role was accepted");
            } catch (IOException negative) {
                if (!negative.getMessage().startsWith("Unknown WWC display role:"))
                    throw negative;
            }
            getLogger().info("PAPER_A05_WWC_DISPLAY_PROXY_COLLISION_PASSED value=10000");
        } finally {
            if (stand != null) stand.remove();
            owner.remove();
        }
    }

    private static void seedChickenStorage(PersistentDataContainer pdc,
                                           UUID followTarget, byte[] shulker) {
        pdc.set(new NamespacedKey("whatawonderfulchicken", "type"),
                PersistentDataType.STRING, "wonderful_chicken");
        pdc.set(new NamespacedKey("whatawonderfulchicken", "data_version"),
                PersistentDataType.INTEGER, 3);
        pdc.set(new NamespacedKey("whatawonderfulchicken", "follow_target"),
                PersistentDataType.STRING, followTarget.toString());
        pdc.set(new NamespacedKey("whatawonderfulchicken", "equipment_shulker_box"),
                PersistentDataType.BYTE_ARRAY, shulker);
    }

    private static StorageSnapshot success(StorageReadResult result, String label) {
        if (result instanceof StorageReadResult.Success ok) return ok.snapshot();
        throw new IllegalStateException(label + " returned " + result);
    }

    private static void assertChickenStorage(StorageSnapshot snapshot, UUID entity, UUID world,
                                             UUID commander, byte[] expectedShulker,
                                             CoinItemCodec coins) {
        if (!entity.equals(snapshot.entityId()) || !world.equals(snapshot.worldId())
                || !commander.equals(snapshot.relationships().get("follow_target"))
                || snapshot.items().size() != 1
                || !"shulker".equals(snapshot.items().getFirst().slot())
                || !Arrays.equals(snapshot.items().getFirst().bytes(), expectedShulker)) {
            throw new IllegalStateException("WWC saved/LIVE snapshot integrity mismatch " + snapshot);
        }
        ItemStack reconstructed = ItemStack.deserializeBytes(snapshot.items().getFirst().bytes());
        if (!(reconstructed.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox box))
            throw new IllegalStateException("WWC serialized ShulkerBox lost nested storage");
        assertMoney(box.getInventory().getItem(0), coins, CoinDenomination.FIVE_THOUSAND, 2);
    }

    private static void assertCoin(byte[] serialized, CoinItemCodec coins,
                                   CoinDenomination value, int amount) {
        assertMoney(ItemStack.deserializeBytes(serialized), coins, value, amount);
    }

    private static void assertMoney(ItemStack item, CoinItemCodec coins,
                                    CoinDenomination denomination, int amount) {
        if (item == null || item.getAmount() != amount
                || coins.identify(item).orElseThrow() != denomination)
            throw new IllegalStateException("Physical currency mismatch: " + item);
    }

    private static void requireUnsupported(StorageReadResult result, String provider) {
        if (!(result instanceof StorageReadResult.Failure fail)
                || !"UNSUPPORTED_SCHEMA".equals(fail.code()))
            throw new IllegalStateException(provider + " must fail closed on unsupported schema: " + result);
    }

    /**
     * An explicitly constructed WWW v3 protocol fixture, NOT copied game state.
     * Exactly matches codec's documented record order; tested against the real
     * packaged reader and its full parser, including trailing-data validation.
     */
    private static byte[] wolfRuntimeFixture(UUID owner, UUID commander, byte[] weapon,
                                             byte[] cargo) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeInt(0x57575254); // WWRT
        out.writeByte(3); // existing producer's runtime container version
        out.writeByte(1); writeUuid(out, owner);
        out.writeLong(0L); // adult biological time
        writeString(out, "WAIT");
        out.writeByte(1); writeUuid(out, commander);
        writeString(out, "VERY_WIDE");
        out.writeByte(0); // no wait position
        out.writeInt(0); // no affection records
        out.writeByte(1); writeItem(out, weapon);
        out.writeInt(1); // 1 cargo slot
        out.writeInt(7); writeItem(out, cargo);
        out.writeInt(1); // generation
        for (int i = 0; i < 6; i++) out.writeByte(0); // no pedigree parents/ancestors
        out.writeByte(0); // no administrator ability overrides
        out.flush();
        return buffer.toByteArray();
    }

    private static void writeUuid(DataOutputStream out, UUID value) throws IOException {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static void writeItem(DataOutputStream out, byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static final String PERSISTENCE_TAG = "simplecoins-a05-persisted-wwc-probe";
    private static final String WOLF_PERSISTENCE_TAG = "simplecoins-a05-persisted-www-probe";
    private static final String FAR_WWC_TAG = "simplecoins-a05-unloaded-wwc-nbt";
    private static final String FAR_WWW_TAG = "simplecoins-a05-unloaded-www-nbt";
    private static final int FAR_CHUNK = 40;
    private static final UUID WOLF_OWNER = UUID.fromString("00000000-0000-0000-0000-00000000a052");
    private static final UUID WOLF_COMMANDER = UUID.fromString("00000000-0000-0000-0000-00000000a053");
    private static final UUID PERSISTENCE_FOLLOW =
            UUID.fromString("00000000-0000-0000-0000-00000000a055");

    private Path persistedIdFile() {
        return getDataFolder().toPath().resolve("wwc-physical-entity-uuid.txt");
    }

    private Path persistedCoinFile() {
        return getDataFolder().toPath().resolve("wwc-shulker-coins.b64");
    }

    private Path persistedWolfIdFile() {
        return getDataFolder().toPath().resolve("www-physical-entity-uuid.txt");
    }

    private Path persistedWolfRuntimeFile() {
        return getDataFolder().toPath().resolve("www-runtime-state.b64");
    }
    private Path farWitness(String file) {
        return getDataFolder().toPath().resolve("far-" + file);
    }

    /** Phase one: a real Vanilla Chicken carrying WWC-readable PDC, saved by Paper itself. */
    private void persistChickenEntity(World world, Map<String, WonderfulStorageReader> providers)
            throws Exception {
        Path entityIdFile = persistedIdFile();
        if (Files.exists(entityIdFile) || Files.exists(persistedCoinFile())
                || Files.exists(persistedWolfIdFile()) || Files.exists(persistedWolfRuntimeFile()))
            throw new IllegalStateException("Persistence fixture must start from a clean test world");
        Chunk chunk = world.getChunkAt(4, 4); // disposable test-world fixture ONLY
        if (!chunk.isLoaded()) throw new IllegalStateException("Paper did not load fixture chunk");
        Chicken chicken = world.spawn(new Location(world, 72.5, 120, 72.5), Chicken.class);
        chicken.addScoreboardTag(PERSISTENCE_TAG);
        byte[] shulker = createMoneyShulker();
        seedChickenStorage(chicken.getPersistentDataContainer(), PERSISTENCE_FOLLOW, shulker);
        // The fixture has no genome/phenotype payload. Unlike the detached
        // Reader v3 checks, a real spawned Chicken is observed by WWC's
        // lifecycle listener; v3 would falsely claim a complete genome.
        // Use the reader-supported legacy v2 storage shape for this entity.
        chicken.getPersistentDataContainer().set(
                new NamespacedKey("whatawonderfulchicken", "data_version"),
                PersistentDataType.INTEGER, 2);
        if (!chicken.isValid() || Bukkit.getEntity(chicken.getUniqueId()) == null)
            throw new IllegalStateException("Fixture was not registered with Paper Entity tracking");
        assertChickenStorage(success(providers.get("wwc").readLoaded(chicken.getUniqueId()),
                        "WWC persistence save preflight"),
                chicken.getUniqueId(), world.getUID(), PERSISTENCE_FOLLOW,
                shulker, new CoinItemCodec(CoinItemCodec.defaultNames()));
        Files.createDirectories(getDataFolder().toPath());
        Files.writeString(entityIdFile, chicken.getUniqueId().toString() + "\n");
        Files.writeString(persistedCoinFile(), Base64.getEncoder().encodeToString(shulker) + "\n");

        // A different real Paper Entity, using the shipped WWW reader's schema.
        // Deliberately fixture-typed PDC: not an initialized/managed WWW wolf.
        Wolf wolf = world.spawn(new Location(world, 74.5, 120, 74.5), Wolf.class);
        wolf.addScoreboardTag(WOLF_PERSISTENCE_TAG);
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        byte[] runtime = wolfRuntimeFixture(WOLF_OWNER, WOLF_COMMANDER,
                coins.create(CoinDenomination.TEN_THOUSAND, 1).serializeAsBytes(),
                coins.create(CoinDenomination.THOUSAND, 5).serializeAsBytes());
        PersistentDataContainer wolfPdc = wolf.getPersistentDataContainer();
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "type"),
                PersistentDataType.STRING, "wonderful_wolf");
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                PersistentDataType.INTEGER, 1);
        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "runtime_state"),
                PersistentDataType.BYTE_ARRAY, runtime);
        if (!wolf.isValid() || Bukkit.getEntity(wolf.getUniqueId()) == null)
            throw new IllegalStateException("WWW fixture not registered as valid Paper Wolf");
        assertPersistedWolf(providers.get("www"), wolf.getUniqueId(), world.getUID(), wolfPdc, coins);
        Files.writeString(persistedWolfIdFile(), wolf.getUniqueId().toString() + "\n");
        Files.writeString(persistedWolfRuntimeFile(),
                Base64.getEncoder().encodeToString(runtime) + "\n");
        persistFarUnloadedFixtures(world, providers, coins);
        world.save();
        getLogger().info("PAPER_A05_WWW_ENTITY_PERSISTED_BEFORE_RESTART_PASSED"
                + " entity=" + wolf.getUniqueId() + " world=" + world.getUID());
        getLogger().info("PAPER_A05_WWC_ENTITY_PERSISTED_BEFORE_RESTART_PASSED"
                + " entity=" + chicken.getUniqueId()
                + " chunk=4,4 world=" + world.getUID());
        // Do not remove fixture before shutdown: the next JVM must load it from the real region.
    }

    /** Phase two: no fixture reconstruction; read the actual Paper entity saved by phase one. */
    private void reloadChickenEntity(World world, Map<String, WonderfulStorageReader> providers)
            throws Exception {
        Path entityIdFile = persistedIdFile();
        if (!Files.isRegularFile(entityIdFile) || !Files.isRegularFile(persistedCoinFile()))
            throw new IllegalStateException("First Paper process did not provide fixture identities");
        UUID id = UUID.fromString(Files.readString(entityIdFile).strip());
        byte[] expected = Base64.getDecoder().decode(Files.readString(persistedCoinFile()).strip());
        Chunk chunk = world.getChunkAt(4, 4); // explicitly load known fixture chunk only
        Chicken found = null;
        int tagged = 0;
        for (Entity candidate : chunk.getEntities()) {
            if (!candidate.getScoreboardTags().contains(PERSISTENCE_TAG)) continue;
            tagged++;
            if (!(candidate instanceof Chicken chicken))
                throw new IllegalStateException("Wrong tagged Entity class after Paper reload");
            found = chicken;
        }
        if (tagged != 1 || found == null || !found.isValid()
                || !id.equals(found.getUniqueId())
                || Bukkit.getEntity(id) == null)
            throw new IllegalStateException("Saved Paper Chicken missing or duplicated after restart"
                    + " found=" + tagged + " expectedEntity=" + id);
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        StorageSnapshot loaded = success(providers.get("wwc").readLoaded(id), "WWC reloaded LIVE");
        StorageSnapshot stored = success(providers.get("wwc").readStored(id, world.getUID(),
                Optional.of(new StorageSnapshot.Position(found.getLocation().getX(),
                        found.getLocation().getY(), found.getLocation().getZ())),
                found.getPersistentDataContainer()), "WWC reloaded stored PDC");
        assertChickenStorage(loaded, id, world.getUID(), PERSISTENCE_FOLLOW, expected, coins);
        assertChickenStorage(stored, id, world.getUID(), PERSISTENCE_FOLLOW, expected, coins);
        if (!loaded.items().equals(stored.items()) || !loaded.relationships().equals(stored.relationships()))
            throw new IllegalStateException("Reloaded WWC sources disagree");
        getLogger().info("PAPER_A05_WWC_REAL_ENTITY_RELOAD_COIN_SHULKER_PASSED"
                + " entity=" + id + " world=" + world.getUID());

        if (!Files.isRegularFile(persistedWolfIdFile())
                || !Files.isRegularFile(persistedWolfRuntimeFile()))
            throw new IllegalStateException("Missing first-JVM WWW evidence");
        UUID wolfId = UUID.fromString(Files.readString(persistedWolfIdFile()).strip());
        byte[] runtime = Base64.getDecoder().decode(
                Files.readString(persistedWolfRuntimeFile()).strip());
        Wolf foundWolf = null;
        int matchingWolves = 0;
        for (Entity candidate : chunk.getEntities()) {
            if (!candidate.getScoreboardTags().contains(WOLF_PERSISTENCE_TAG)) continue;
            matchingWolves++;
            if (!(candidate instanceof Wolf wolf))
                throw new IllegalStateException("Tagged WWW fixture is not a Paper Wolf");
            foundWolf = wolf;
        }
        if (matchingWolves != 1 || foundWolf == null || !foundWolf.isValid()
                || !wolfId.equals(foundWolf.getUniqueId())
                || Bukkit.getEntity(wolfId) == null)
            throw new IllegalStateException("Persisted WWW fixture Wolf absent/duplicated");
        byte[] savedRuntime = foundWolf.getPersistentDataContainer().get(
                new NamespacedKey("whatawonderfulwolf", "runtime_state"),
                PersistentDataType.BYTE_ARRAY);
        if (!Arrays.equals(runtime, savedRuntime))
            throw new IllegalStateException("WWW runtime changed across Paper restart");
        assertPersistedWolf(providers.get("www"), wolfId, world.getUID(),
                foundWolf.getPersistentDataContainer(), coins);
        getLogger().info("PAPER_A05_WWW_REAL_ENTITY_RELOAD_OWNER_CURRENCY_PASSED"
                + " entity=" + wolfId + " world=" + world.getUID());
    }

    private static void assertPersistedWolf(WonderfulStorageReader www, UUID entity,
                                            UUID world, PersistentDataContainer pdc,
                                            CoinItemCodec coins) {
        StorageSnapshot snapshot = success(
                www.readStored(entity, world, Optional.empty(), pdc), "WWW persisted PDC");
        if (!entity.equals(snapshot.entityId()) || !world.equals(snapshot.worldId())
                || !WOLF_OWNER.equals(snapshot.relationships().get("owner"))
                || !WOLF_COMMANDER.equals(snapshot.relationships().get("commander"))
                || snapshot.items().size() != 2)
            throw new IllegalStateException("WWW saved ownership/currency identity mismatch");
        boolean seenWeapon = false, seenCargo = false;
        for (var item : snapshot.items()) {
            switch (item.slot()) {
                case "weapon" -> {
                    if (seenWeapon) throw new IllegalStateException("Duplicated weapon slot");
                    assertCoin(item.bytes(), coins, CoinDenomination.TEN_THOUSAND, 1);
                    seenWeapon = true;
                }
                case "cargo:7" -> {
                    if (seenCargo) throw new IllegalStateException("Duplicated cargo slot");
                    assertCoin(item.bytes(), coins, CoinDenomination.THOUSAND, 5);
                    seenCargo = true;
                }
                default -> throw new IllegalStateException("Unrecognized WWW money slot " + item.slot());
            }
        }
        if (!seenWeapon || !seenCargo)
            throw new IllegalStateException("WWW coin slot evidence missing");
    }

    private static byte[] createMoneyShulker() {
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        if (!(shulker.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox box))
            throw new IllegalStateException("Real ShulkerBox BlockStateMeta unavailable");
        box.getInventory().setItem(0, coins.create(CoinDenomination.FIVE_THOUSAND, 2));
        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        return shulker.serializeAsBytes();
    }

    /**
     * Paper process A only: generate two saved monetary Entities away from
     * all spawn chunks. They are both real Paper entities but manually typed
     * WWC/WWW fixture PDC, not a game-managed genome-individual lifecycle.
     */
    private void persistFarUnloadedFixtures(World world,
                                             Map<String, WonderfulStorageReader> providers,
                                             CoinItemCodec coins) throws Exception {
        for (String n : List.of("wwc-uuid.txt", "www-uuid.txt",
                "wwc-shulker.b64", "www-runtime.b64")) {
            if (Files.exists(farWitness(n)))
                throw new IllegalStateException("Far-source evidence already exists: " + n);
        }
        Chunk far = world.getChunkAt(FAR_CHUNK, FAR_CHUNK);
        if (!far.isLoaded()) throw new IllegalStateException("Cannot generate isolated far chunk");
        double anchor = FAR_CHUNK * 16.0;
        Chicken chicken = world.spawn(
                new Location(world, anchor + 8.5, 120, anchor + 8.5), Chicken.class);
        chicken.addScoreboardTag(FAR_WWC_TAG);
        byte[] shulker = createMoneyShulker();
        seedChickenStorage(chicken.getPersistentDataContainer(), PERSISTENCE_FOLLOW, shulker);
        chicken.getPersistentDataContainer().set(
                new NamespacedKey("whatawonderfulchicken", "data_version"),
                PersistentDataType.INTEGER, 2);
        assertChickenStorage(success(providers.get("wwc").readLoaded(chicken.getUniqueId()),
                "WWC far loaded pre-save"), chicken.getUniqueId(), world.getUID(),
                PERSISTENCE_FOLLOW, shulker, coins);
        Wolf wolf = world.spawn(
                new Location(world, anchor + 10.5, 120, anchor + 10.5), Wolf.class);
        wolf.addScoreboardTag(FAR_WWW_TAG);
        // Paper must persist both the genuine vanilla equipment item and
        // the same WCL runtime weapon, plus a verifiable tame-owner. Only
        // then can the unchanged SAVED source prove one physical coin.
        wolf.setOwner(Bukkit.getOfflinePlayer(WOLF_OWNER));
        ItemStack mirroredWeapon = coins.create(CoinDenomination.TEN_THOUSAND, 1);
        wolf.getEquipment().setItemInMainHand(mirroredWeapon);
        byte[] runtime = wolfRuntimeFixture(WOLF_OWNER, WOLF_COMMANDER,
                mirroredWeapon.serializeAsBytes(),
                coins.create(CoinDenomination.THOUSAND, 5).serializeAsBytes());
        PersistentDataContainer pdc = wolf.getPersistentDataContainer();
        pdc.set(new NamespacedKey("whatawonderfulwolf", "type"),
                PersistentDataType.STRING, "wonderful_wolf");
        pdc.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                PersistentDataType.INTEGER, 1);
        pdc.set(new NamespacedKey("whatawonderfulwolf", "runtime_state"),
                PersistentDataType.BYTE_ARRAY, runtime);
        assertPersistedWolf(providers.get("www"), wolf.getUniqueId(), world.getUID(), pdc, coins);
        Files.writeString(farWitness("wwc-uuid.txt"), chicken.getUniqueId().toString() + "\n");
        Files.writeString(farWitness("www-uuid.txt"), wolf.getUniqueId().toString() + "\n");
        Files.writeString(farWitness("wwc-shulker.b64"),
                Base64.getEncoder().encodeToString(shulker) + "\n");
        Files.writeString(farWitness("www-runtime.b64"),
                Base64.getEncoder().encodeToString(runtime) + "\n");
        getLogger().info("PAPER_A05_FAR_TWO_STORED_PROVIDER_FIXTURES_SAVED_PASSED"
                + " chunk=" + FAR_CHUNK + "," + FAR_CHUNK);
    }

    /**
     * Paper process B: the two Entities are NOT loaded. The only input is
     * actual read-only disk Entity Anvil NBT and the witnesses written by A.
     * Reconstitute only type-checked PDC keys on detached ItemMeta and invoke
     * the two real shipped WCL readStored providers, without getChunkAt.
     */
    private void verifyFarUnloadedStoredItems(World world,
                                               Map<String, WonderfulStorageReader> providers)
            throws Exception {
        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK))
            throw new IllegalStateException("Far test chunk already loaded; cannot claim offline scan");
        UUID wwcUuid = UUID.fromString(Files.readString(farWitness("wwc-uuid.txt")).strip());
        UUID wwwUuid = UUID.fromString(Files.readString(farWitness("www-uuid.txt")).strip());
        if (wwcUuid.equals(wwwUuid)) throw new IOException("Aliased WWC and WWW Entity identity");
        byte[] wwcBytes = Base64.getDecoder().decode(
                Files.readString(farWitness("wwc-shulker.b64")).strip());
        byte[] wwwBytes = Base64.getDecoder().decode(
                Files.readString(farWitness("www-runtime.b64")).strip());
        Path region = world.getWorldPath()
                .resolve("entities/r.1.1.mca").toAbsolutePath().normalize();

        // Obtain source closure from the real Paper World folder, not a
        // hand-selected one-file Manifest. Both terrain and Entity Anvil,
        // plus any world-independent playerdata, are mandatory observed input.
        WorldStorageDiscovery.Manifest boundedManifest = WorldStorageDiscovery.discover(
                Map.of(world.getName(), world.getWorldFolder().toPath()),
                new WorldScope(true, Set.of(), Set.of()), 100000);
        if (boundedManifest.origin() == null ||
                boundedManifest.worldFiles().stream()
                        .filter(input -> input.kind() == WorldStorageDiscovery.Kind.ENTITY
                                && input.path().toAbsolutePath().normalize().equals(region))
                        .count() != 1)
            throw new IOException("Paper WorldFolder discovery omitted or duplicated the far Entity MCA");
        // Paper 26.2 persists per-dimension UUID under data/paper/metadata.dat.
        // A manually claimed Map must not become a trusted WCL saved source.
        var metadata = PaperSavedWorldMetadataWitness.capture(
                boundedManifest, Map.of(world.getName(), world.getUID()));
        if (!metadata.worldIds().equals(Map.of(world.getName(), world.getUID())))
            throw new IOException("Paper saved World metadata differs from actual Bukkit UUID");
        metadata.requireUnchanged();
        try {
            UUID wrong = new UUID(world.getUID().getMostSignificantBits() ^ 1L,
                    world.getUID().getLeastSignificantBits());
            PaperSavedWorldMetadataWitness.capture(
                    boundedManifest, Map.of(world.getName(), wrong));
            throw new IOException("False Paper saved World UUID was accepted");
        } catch (IOException expected) {
            if (!expected.getMessage().startsWith(
                    "Expected World UUID disagrees with Paper 26.2 metadata: "))
                throw expected;
        }
        getLogger().info("PAPER_A05_SAVED_WORLD_PAPER_METADATA_IDENTITY_PASSED");
        int[] traversed = {0, 0};
        java.util.List<PaperWonderfulSavedChunkCandidates.Candidate> candidates =
                new java.util.ArrayList<>();
        java.util.Set<UUID> seenCandidates = new java.util.HashSet<>();
        java.util.Set<UUID> seenWallets = new java.util.HashSet<>();
        int[] visitedPlayers = {0};
        int[] visitedChunks = {0};
        var verifiedTraversal = WorldSnapshotTraversal.traverseVerified(
                boundedManifest, 2 * 1024 * 1024, 16 * 1024 * 1024,
                new WorldSnapshotTraversal.Visitor() {
                    @Override public void chunk(WorldStorageDiscovery.SourceFile source,
                                                AnvilRegionReader.Chunk saved) throws IOException {
                        visitedChunks[0]++;
                        // All discovered saved region/terrain sources are
                        // visited and stamped, not only the chosen target.
                        if (source.kind() != WorldStorageDiscovery.Kind.ENTITY) return;
                        if (!source.world().equals(world.getName()))
                            throw new IOException("Cross-world saved Entity source");
                        var sourced = PaperWonderfulSavedChunkCandidates.collect(
                                WclServicesProbe.this, source, world.getUID(), saved,
                                seenCandidates, seenWallets, 2 * 1024 * 1024, 16 * 1024 * 1024);
                        candidates.addAll(sourced);
                        if (saved.x() == FAR_CHUNK && saved.z() == FAR_CHUNK
                                && !sourced.isEmpty()) {
                            // Real detached saved PDC from an unloaded Anvil Entity:
                            // use one authentic WWC holder as an isolated
                            // local source without claiming source closure.
                            Object justWwc = null;
                            if (!(saved.nbt().get("Entities") instanceof List<?> rawEntities))
                                throw new IOException("Missing actual SAVED Entity chunk payload");
                            for (Object original : rawEntities) {
                                if (original instanceof Map<?, ?> data
                                        && "minecraft:chicken".equals(data.get("id"))
                                        && wwcUuid.equals(readSavedUuid(data.get("UUID")))) {
                                    if (justWwc != null)
                                        throw new IOException("Duplicate real WWC in saved source");
                                    justWwc = original;
                                }
                            }
                            if (justWwc == null)
                                throw new IOException("Real SAVED WCL WWC not found for isolated test");
                            var wwcOnly = new AnvilRegionReader.Chunk(
                                    saved.x(), saved.z(),
                                    Map.of("Entities", List.of(justWwc)));
                            String sourceId = source.path().toAbsolutePath().normalize().toString();
                            var wwcStaged = new SettlementAccumulator(
                                    new WorldScope(true, Set.of(), Set.of()), Set.of(sourceId));
                            var localWcl = new PaperWonderfulSavedSettlementSource(
                                    WclServicesProbe.this, Map.of(world.getName(), world.getUID()),
                                    boundedManifest, Set.of(), null, Set.of(),
                                    2 * 1024 * 1024, 16 * 1024 * 1024);
                            localWcl.stage(source, wwcOnly, wwcStaged, new java.util.HashSet<>());
                            wwcStaged.completeSource(sourceId);
                            var localResult = wwcStaged.commit();
                            // The isolated Paper fixture has no corresponding
                            // playerdata for this commander. Confirmed 02A
                            // therefore requires DEAD rather than PERSONAL.
                            if (!BigInteger.valueOf(10000).equals(localResult.dead())
                                    || !BigInteger.ZERO.equals(localResult.statistics().worldTotal())
                                    || localResult.deadLocations().stream().noneMatch(loc ->
                                            PERSISTENCE_FOLLOW.equals(loc.owner())
                                            && BigInteger.valueOf(10000).equals(loc.value())))
                                throw new IOException("SAVED WWC real 10000C not staged as DEAD"
                                        + " dead=" + localResult.dead()
                                        + " personal=" + localResult.personalBalances()
                                        + " worldTotal=" + localResult.statistics().worldTotal());
                            // A real Paper-persisted Wolf with a native 10000C
                            // main-hand coin and the SAME WCL weapon must
                            // contribute that item once, not twice. Its WCL
                            // commander has priority over its tame owner.
                            var savedWww = sourced.stream()
                                    .filter(candidate -> "www".equals(candidate.providerId()))
                                    .findFirst().orElseThrow(() ->
                                            new IOException("No real saved WWW source"));
                            var expectedCommander = new co.surumene.simplecoins.domain.WclOwnerEvidence.Decision(
                                    co.surumene.simplecoins.domain.WclOwnerEvidence.Kind.OWNER,
                                    WOLF_COMMANDER);
                            if (!expectedCommander.equals(savedWww.savedOwnerEvidence(true, saved)))
                                throw new IOException("Real SAVED WWW tame-owner evidence mismatch");
                            var once = new SettlementAccumulator(
                                    new WorldScope(true, Set.of(), Set.of()), Set.of(sourceId));
                            var sharedWallets = new java.util.HashSet<UUID>();
                            ResolvedEntitySettlementImport.stageChunk(
                                    once, sourceId, source.world(), saved.nbt(),
                                    Map.of(wwwUuid.toString(),
                                            new ResolvedEntitySettlementImport.Ownership(
                                                    WOLF_COMMANDER, false, false)),
                                    sharedWallets);
                            var allWcl = new PaperWonderfulSavedSettlementSource(
                                    WclServicesProbe.this, Map.of(world.getName(), world.getUID()),
                                    boundedManifest, Set.of(), null, Set.of(),
                                    2 * 1024 * 1024, 16 * 1024 * 1024);
                            allWcl.stage(source, saved, once, sharedWallets);
                            once.completeSource(sourceId);
                            var onceResult = once.commit();
                            if (!BigInteger.valueOf(25000).equals(onceResult.dead())
                                    || !BigInteger.ZERO.equals(onceResult.statistics().worldTotal()))
                                throw new IOException("Real SAVED WWW 10000C mirror counted incorrectly"
                                        + " dead=" + onceResult.dead()
                                        + " total=" + onceResult.statistics().worldTotal());
                            getLogger().info("PAPER_A05_SAVED_WCL_NATIVE_WWW_MIRROR_ONCE_PASSED");
                            // Negative owner test: remove only the independent
                            // vanilla tame-owner witness from this otherwise
                            // persisted Entity. The WCL report must still HOLD;
                            // this synthetic alteration is NOT a real source.
                            Object withoutTame = null;
                            for (Object value : (List<?>) saved.nbt().get("Entities")) {
                                if (value instanceof Map<?,?> entity &&
                                        wwwUuid.equals(readSavedUuid(entity.get("UUID")))) {
                                    Map<String,Object> modified = new HashMap<>();
                                    for (var entry : entity.entrySet())
                                        modified.put((String)entry.getKey(), entry.getValue());
                                    modified.remove("Owner");
                                    withoutTame = modified;
                                }
                            }
                            if (withoutTame == null)
                                throw new IOException("Actual WWW entity absent for owner-negative fixture");
                            var ownerNegative = new AnvilRegionReader.Chunk(
                                    saved.x(), saved.z(), Map.of("Entities", List.of(withoutTame)));
                            if (savedWww.savedOwnerEvidence(true, ownerNegative).kind()
                                    != co.surumene.simplecoins.domain.WclOwnerEvidence.Kind.HOLD)
                                throw new IOException("WWW taming owner-contradiction HOLD regression");
                            getLogger().info("PAPER_A05_SAVED_WCL_STAGE_UNPUBLISHED_WWC_10000_PASSED");

                            try {
                                PaperWonderfulSavedChunkCandidates.collect(
                                        WclServicesProbe.this, source, world.getUID(), saved,
                                        seenCandidates, seenWallets, 2 * 1024 * 1024, 16 * 1024 * 1024);
                                throw new IOException("Duplicate saved WCL source was accepted");
                            } catch (IOException duplicate) {
                                if (!duplicate.getMessage().startsWith(
                                        "Duplicate WCL saved Entity UUID across selected chunks: "))
                                    throw duplicate;
                            }
                            getLogger().info("PAPER_A05_PRODUCT_SAVED_WCL_DUPLICATE_ENTITY_REJECTED_PASSED");
                        }
                        if (!(saved.nbt().get("Entities") instanceof List<?> entities))
                            throw new IOException("Traversed Entity Chunk missing saved Entities list");
                        for (Object raw : entities) {
                            if (!(raw instanceof Map<?, ?> entity)) continue;
                            if (!(entity.get("Tags") instanceof List<?> tags)) continue;
                            boolean isWwc = tags.contains(FAR_WWC_TAG);
                            boolean isWww = tags.contains(FAR_WWW_TAG);
                            if (!isWwc && !isWww) continue;
                            if (isWwc && isWww
                                    || saved.x() != FAR_CHUNK || saved.z() != FAR_CHUNK)
                                throw new IOException("Tagged provider Entity not in its unique chunk");
                            UUID identity = readSavedUuid(entity.get("UUID"));
                            if (isWwc) {
                                if (++traversed[0] != 1 || !wwcUuid.equals(identity)
                                        || !"minecraft:chicken".equals(entity.get("id")))
                                    throw new IOException("Traversal WWC UUID/type mismatch");
                            } else {
                                if (++traversed[1] != 1 || !wwwUuid.equals(identity)
                                        || !"minecraft:wolf".equals(entity.get("id")))
                                    throw new IOException("Traversal WWW UUID/type mismatch");
                            }
                        }
                    }
                    @Override public void player(UUID player, Map<String, Object> data)
                            throws IOException {
                        if (player == null || data == null)
                            throw new IOException("Incomplete discovered playerdata source");
                        visitedPlayers[0]++;
                    }
                }, null);
        if (verifiedTraversal.report().regionFiles() != boundedManifest.worldFiles().size()
                || verifiedTraversal.report().chunks() != visitedChunks[0]
                || verifiedTraversal.report().playerFiles() != visitedPlayers[0]
                || visitedPlayers[0] != boundedManifest.playerDataFiles().size()
                || traversed[0] != 1 || traversed[1] != 1)
            throw new IOException("Discovered Paper saved-source traversal was incomplete");
        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK))
            throw new IOException("WorldSnapshotTraversal loaded the far world chunk");
        var farCandidates = candidates.stream()
                .filter(c -> c.chunkX() == FAR_CHUNK && c.chunkZ() == FAR_CHUNK)
                .toList();
        if (farCandidates.size() != 2)
            throw new IOException("Product saved WCL scan must find exactly two far candidates: "
                    + farCandidates.size());
        BigInteger candidateValue = BigInteger.ZERO;
        java.util.Set<String> providerIds = new java.util.HashSet<>();
        for (var candidate : farCandidates) {
            if (!candidate.worldId().equals(world.getUID())
                    || !candidate.source().world().equals(world.getName())
                    || !candidate.source().path().toAbsolutePath().normalize().equals(region)
                    || !providerIds.add(candidate.providerId()))
                throw new IOException("Product saved WCL source provenance/provider mismatch");
            if ("wwc".equals(candidate.providerId())) {
                if (!wwcUuid.equals(candidate.entityId())
                        || !PERSISTENCE_FOLLOW.equals(
                                candidate.relationships().get("follow_target"))
                        || candidate.slotValues().size() != 1
                        || !BigInteger.valueOf(10000).equals(candidate.slotValues().get("shulker")))
                    throw new IOException("Product saved WWC sourced candidate mismatch");
            } else if ("www".equals(candidate.providerId())) {
                if (!wwwUuid.equals(candidate.entityId())
                        || !WOLF_COMMANDER.equals(candidate.relationships().get("commander"))
                        || !WOLF_OWNER.equals(candidate.relationships().get("owner"))
                        || candidate.slotValues().size() != 2
                        || !BigInteger.valueOf(10000).equals(candidate.slotValues().get("weapon"))
                        || !BigInteger.valueOf(5000).equals(candidate.slotValues().get("cargo:7")))
                    throw new IOException("Product saved WWW sourced candidate mismatch");
            } else throw new IOException("Unexpected product saved WCL provider");
            var physicalSlots = candidate.physicalSlotKeys();
            if (!physicalSlots.keySet().equals(candidate.slotValues().keySet()))
                throw new IOException("Product saved WCL physical keys omitted monetary slots");
            String prefix = "entity/" + candidate.entityId();
            if ("www".equals(candidate.providerId())) {
                if (!(prefix + "/equipment/mainhand").equals(physicalSlots.get("weapon"))
                        || !(prefix + "/wcl/www/cargo:7").equals(physicalSlots.get("cargo:7")))
                    throw new IOException("WWW rendered weapon was incorrectly counted independently");
            } else if (!(prefix + "/wcl/wwc/shulker").equals(physicalSlots.get("shulker"))) {
                throw new IOException("WWC shulker was incorrectly equated with vanilla equipment");
            }
            for (BigInteger value : candidate.slotValues().values())
                candidateValue = candidateValue.add(value);
        }
        if (!providerIds.equals(Set.of("wwc", "www"))
                || !BigInteger.valueOf(25000).equals(candidateValue))
            throw new IOException("Product saved WCL source total did not conserve 25000C");
        getLogger().info("PAPER_A05_SAVED_WCL_SLOT_ALIASES_MAPPED_PASSED");
        getLogger().info("PAPER_A05_PRODUCT_DISCOVERED_SAVED_WCL_CANDIDATES_25000_PASSED"
                + " providers=" + providerIds.size() + " candidates=" + farCandidates.size());
        verifyProductSavedChunkCandidateNegatives(world, boundedManifest);
        int[] recognized = {0, 0};
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        AnvilRegionReader.scan(region, 2 * 1024 * 1024, 16 * 1024 * 1024, chunk -> {
            if (chunk.x() != FAR_CHUNK || chunk.z() != FAR_CHUNK) {
                if (chunk.nbt().containsKey("Entities")) {
                    for (Object raw : (List<?>) chunk.nbt().get("Entities")) {
                        if (raw instanceof Map<?, ?> candidate
                                && candidate.get("Tags") instanceof List<?> tags
                                && (tags.contains(FAR_WWC_TAG) || tags.contains(FAR_WWW_TAG)))
                            throw new IOException("Monetary fixture changed physical chunk");
                    }
                }
                return;
            }
            if (!(chunk.nbt().get("Entities") instanceof List<?> entities))
                throw new IOException("Offline far Entity Anvil chunk missing Entities list");
            for (Object raw : entities) {
                if (!(raw instanceof Map<?, ?> entity))
                    throw new IOException("Malformed saved Entity compound");
                if (!(entity.get("Tags") instanceof List<?> tags)) continue;
                boolean chickenTag = tags.contains(FAR_WWC_TAG);
                boolean wolfTag = tags.contains(FAR_WWW_TAG);
                if (!chickenTag && !wolfTag) continue;
                if (chickenTag && wolfTag)
                    throw new IOException("Entity matches both independent storage providers");
                UUID entityId = readSavedUuid(entity.get("UUID"));
                if (!(entity.get("BukkitValues") instanceof Map<?, ?> stored))
                    throw new IOException("Tagged far Entity has no saved BukkitValues PDC");
                PersistentDataContainer detached = new ItemStack(Material.PAPER)
                        .getItemMeta().getPersistentDataContainer();
                if (chickenTag) {
                    if (++recognized[0] != 1 || !wwcUuid.equals(entityId)
                            || !"minecraft:chicken".equals(entity.get("id"))
                            || !"wonderful_chicken".equals(
                                    stored.get("whatawonderfulchicken:type"))
                            || !(stored.get("whatawonderfulchicken:data_version")
                                    instanceof Integer version) || version != 2
                            || !PERSISTENCE_FOLLOW.toString().equals(
                                    stored.get("whatawonderfulchicken:follow_target"))
                            || !(stored.get("whatawonderfulchicken:equipment_shulker_box")
                                    instanceof byte[] shulker)
                            || !Arrays.equals(wwcBytes, shulker))
                        throw new IOException("Saved far WWC source differs from original fixture");
                    detached.set(new NamespacedKey("whatawonderfulchicken", "type"),
                            PersistentDataType.STRING, "wonderful_chicken");
                    detached.set(new NamespacedKey("whatawonderfulchicken", "data_version"),
                            PersistentDataType.INTEGER, 2);
                    detached.set(new NamespacedKey("whatawonderfulchicken", "follow_target"),
                            PersistentDataType.STRING, PERSISTENCE_FOLLOW.toString());
                    detached.set(new NamespacedKey("whatawonderfulchicken", "equipment_shulker_box"),
                            PersistentDataType.BYTE_ARRAY, shulker);
                    StorageSnapshot source = success(
                            providers.get("wwc").readStored(entityId, world.getUID(),
                                    Optional.empty(), detached), "WWC detached unloaded NBT");
                    assertChickenStorage(source, wwcUuid, world.getUID(), PERSISTENCE_FOLLOW,
                            wwcBytes, coins);
                    var productionStored = PaperWonderfulLoadedEvidence.readStored(
                            this, entityId, world.getUID(), "wwc", detached);
                    if (productionStored.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                            || productionStored.snapshot().isEmpty()
                            || !PERSISTENCE_FOLLOW.equals(
                                productionStored.snapshot().orElseThrow()
                                        .relationships().get("follow_target"))
                            || productionStored.snapshot().orElseThrow().items().size() != 1
                            || !"shulker".equals(
                                productionStored.snapshot().orElseThrow().items().getFirst().slot())
                            || !Arrays.equals(wwcBytes,
                                productionStored.snapshot().orElseThrow().items().getFirst().bytes()))
                        throw new IOException("Product offline WWC saved PDC evidence mismatch");
                    var valueBySlot = PaperWonderfulSnapshotAssets.slotValues(
                            productionStored.snapshot().orElseThrow(), 2 * 1024 * 1024,
                            16 * 1024 * 1024);
                    if (valueBySlot.size() != 1
                            || !BigInteger.valueOf(10000).equals(valueBySlot.get("shulker")))
                        throw new IOException("Product offline WWC shulker value mismatch: "
                                + valueBySlot);
                    getLogger().info("PAPER_A05_PRODUCT_SAVED_WWC_SHULKER_VALUE_10000_PASSED");
                    assertProductSavedPdcReconstruction(world, entityId, "wwc", entity,
                            productionStored.snapshot().orElseThrow());
                    var wrongSpecies = PaperWonderfulLoadedEvidence.readStored(
                            this, entityId, world.getUID(), "www", detached);
                    if (wrongSpecies.status() != PaperWonderfulLoadedEvidence.Status.NOT_FOUND
                            || wrongSpecies.snapshot().isPresent())
                        throw new IOException("Wrong-provider offline reader must return no candidate");

                } else {
                    if (++recognized[1] != 1 || !wwwUuid.equals(entityId)
                            || !"minecraft:wolf".equals(entity.get("id"))
                            || !"wonderful_wolf".equals(
                                    stored.get("whatawonderfulwolf:type"))
                            || !(stored.get("whatawonderfulwolf:schema_version")
                                    instanceof Integer version) || version != 1
                            || !(stored.get("whatawonderfulwolf:runtime_state")
                                    instanceof byte[] runtime)
                            || !Arrays.equals(wwwBytes, runtime))
                        throw new IOException("Saved far WWW source differs from original fixture");
                    detached.set(new NamespacedKey("whatawonderfulwolf", "type"),
                            PersistentDataType.STRING, "wonderful_wolf");
                    detached.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                            PersistentDataType.INTEGER, 1);
                    detached.set(new NamespacedKey("whatawonderfulwolf", "runtime_state"),
                            PersistentDataType.BYTE_ARRAY, runtime);
                    assertPersistedWolf(providers.get("www"), wwwUuid, world.getUID(),
                            detached, coins);
                    var productionStored = PaperWonderfulLoadedEvidence.readStored(
                            this, entityId, world.getUID(), "www", detached);
                    if (productionStored.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                            || productionStored.snapshot().isEmpty())
                        throw new IOException("Product offline WWW read failed");
                    var wolfSource = productionStored.snapshot().orElseThrow();
                    if (!wolfSource.entityId().equals(wwwUuid)
                            || !wolfSource.worldId().equals(world.getUID())
                            || !"www".equals(wolfSource.providerId())
                            || !WOLF_COMMANDER.equals(wolfSource.relationships().get("commander"))
                            || !WOLF_OWNER.equals(wolfSource.relationships().get("owner"))
                            || wolfSource.items().size() != 2
                            || wolfSource.items().stream().noneMatch(item ->
                                "weapon".equals(item.slot())
                                && Arrays.equals(
                                    coins.create(CoinDenomination.TEN_THOUSAND, 1).serializeAsBytes(),
                                    item.bytes())))
                        throw new IOException("Product offline WWW identities/serialized items differ");
                    var valueBySlot = PaperWonderfulSnapshotAssets.slotValues(
                            wolfSource, 2 * 1024 * 1024, 16 * 1024 * 1024);
                    if (valueBySlot.size() != 2
                            || !BigInteger.valueOf(10000).equals(valueBySlot.get("weapon"))
                            || !BigInteger.valueOf(5000).equals(valueBySlot.get("cargo:7")))
                        throw new IOException("Product offline WWW weapon/cargo values mismatch: "
                                + valueBySlot);
                    getLogger().info("PAPER_A05_PRODUCT_SAVED_WWW_WEAPON_CARGO_VALUE_15000_PASSED");
                    assertProductSavedPdcReconstruction(world, entityId, "www", entity,
                            productionStored.snapshot().orElseThrow());

                }
            }
        });
        // The *same* source stamp must survive the second, provider-owned
        // readStored pass: new bytes, ctime or physical identity are rejected.
        verifiedTraversal.witness().requireUnchanged();
        boundedManifest.requireUnchangedDiscovery();
        getLogger().info("PAPER_A05_REAL_PAPER_WORLDFOLDER_DISCOVERY_CLOSURE_PASSED"
                + " world=" + world.getName()
                + " regionFiles=" + boundedManifest.worldFiles().size()
                + " chunks=" + visitedChunks[0]
                + " playerFiles=" + visitedPlayers[0]
                + " provenance=true farChunkUnloaded=true");
        getLogger().info("PAPER_A05_VERIFIED_SAVED_TRAVERSAL_TWO_WCL_PROVIDERS_PASSED"
                + " uniqueEntityIds=2 discoveredRegionFiles="
                + boundedManifest.worldFiles().size() + " chunkNeverLoaded=true");
        if (recognized[0] != 1 || recognized[1] != 1)
            throw new IOException("Unloaded WWC/WWW saved source count incomplete WWC="
                    + recognized[0] + " WWW=" + recognized[1]);
        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK))
            throw new IllegalStateException("Saved Entity read accidentally loaded far chunk");
        getLogger().info("PAPER_A05_PRODUCT_STORED_WWC_WWW_SOURCE_PROOF_PASSED"
                + " entities=2 source=verified-Anvil-PDC chunkUnloaded=true");
        getLogger().info("PAPER_A05_UNLOADED_WWC_WWW_SAVED_NBT_TO_REAL_WCL_PASSED"
                + " source=entities/r.1.1.mca entities=2"
                + " regionChunk=" + FAR_CHUNK + "," + FAR_CHUNK
                + " totalCoins=25000");
    }

    /**
     * Compare a typed product reconstruction of the actual Anvil compound with
     * the independently seeded WCL PDC fixture from the same saved source.
     * No Paper chunk/Entity API is used to load this saved object.
     */
    private void assertProductSavedPdcReconstruction(
            World world, UUID id, String providerId, Map<?, ?> actualEntity,
            PaperWonderfulLoadedEvidence.Snapshot expected) throws IOException {
        PersistentDataContainer reconstructed = PaperWonderfulSavedPdc.reconstruct(
                id, providerId, actualEntity, 2 * 1024 * 1024);
        var read = PaperWonderfulLoadedEvidence.readStored(
                this, id, world.getUID(), providerId, reconstructed);
        if (read.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                || read.snapshot().isEmpty())
            throw new IOException("Reconstructed saved WCL PDC reader failed: " + read);
        var snapshot = read.snapshot().orElseThrow();
        if (!snapshot.entityId().equals(expected.entityId())
                || !snapshot.worldId().equals(expected.worldId())
                || !snapshot.providerId().equals(expected.providerId())
                || !snapshot.relationships().equals(expected.relationships())
                || snapshot.items().size() != expected.items().size())
            throw new IOException("Reconstructed saved WCL identities differ from verified source");
        Map<String, byte[]> expectedBytes = new HashMap<>();
        Map<String, byte[]> restoredBytes = new HashMap<>();
        for (var item : expected.items()) {
            if (expectedBytes.putIfAbsent(item.slot(), item.bytes()) != null)
                throw new IOException("Duplicate reference WCL slot");
        }
        for (var item : snapshot.items()) {
            if (restoredBytes.putIfAbsent(item.slot(), item.bytes()) != null)
                throw new IOException("Duplicate reconstructed WCL slot");
        }
        if (!restoredBytes.keySet().equals(expectedBytes.keySet()))
            throw new IOException("Reconstructed WCL slots missing or added");
        for (String slot : expectedBytes.keySet())
            if (!Arrays.equals(expectedBytes.get(slot), restoredBytes.get(slot)))
                throw new IOException("WCL source bytes changed during PDC reconstruction");
        if (!PaperWonderfulSnapshotAssets.slotValues(
                snapshot, 2 * 1024 * 1024, 16 * 1024 * 1024).equals(
                    PaperWonderfulSnapshotAssets.slotValues(
                            expected, 2 * 1024 * 1024, 16 * 1024 * 1024)))
            throw new IOException("Reconstructed WCL monetary values differ");
        try {
            PaperWonderfulSavedPdc.reconstruct(
                    UUID.fromString("00000000-0000-0000-0000-00000000a0f0"),
                    providerId, actualEntity, 2 * 1024 * 1024);
            throw new IOException("Wrong saved Entity UUID accepted by product PDC reader");
        } catch (IOException expectedMismatch) {
            if (!"Saved WCL Entity UUID disagrees with selected source"
                    .equals(expectedMismatch.getMessage()))
                throw expectedMismatch;
        }
        getLogger().info("PAPER_A05_PRODUCT_SAVED_ANVIL_PDC_RECONSTRUCTION_PASSED"
                + " provider=" + providerId);
        verifyVersionedPdcRestoration(world, id, providerId, actualEntity, expected);
    }

    /**
     * Technical schema compatibility ONLY. This clones a real saved NBT
     * compound and synthesizes current schema metadata; it is deliberately
     * not represented as a naturally persisted v4/v2 genome source.
     */
    private void verifyVersionedPdcRestoration(
            World world, UUID id, String providerId, Map<?, ?> source,
            PaperWonderfulLoadedEvidence.Snapshot reference) throws IOException {
        Map<String, Object> cloned = new HashMap<>();
        for (var entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key))
                throw new IOException("Saved Entity contains a non-string NBT key");
            cloned.put(key, entry.getValue());
        }
        if (!(cloned.get("BukkitValues") instanceof Map<?, ?> original))
            throw new IOException("Missing original saved PDC map");
        Map<String, Object> pdc = new HashMap<>();
        for (var entry : original.entrySet()) {
            if (!(entry.getKey() instanceof String key))
                throw new IOException("Saved BukkitValues contains a non-string key");
            pdc.put(key, entry.getValue());
        }
        String ns = "wwc".equals(providerId) ? "whatawonderfulchicken" : "whatawonderfulwolf";
        String versionKey = ns + (providerId.equals("wwc") ? ":data_version" : ":schema_version");
        pdc.put(versionKey, providerId.equals("wwc") ? 4 : 2);
        String genomeKey = ns + ":genome_ref";
        pdc.put(genomeKey, "00000000-0000-0000-0000-00000000a0f1");
        cloned.put("BukkitValues", pdc);
        PersistentDataContainer detached = PaperWonderfulSavedPdc.reconstruct(
                id, providerId, cloned, 2 * 1024 * 1024);
        var result = PaperWonderfulLoadedEvidence.readStored(
                this, id, world.getUID(), providerId, detached);
        if (result.status() != PaperWonderfulLoadedEvidence.Status.SUCCESS
                || result.snapshot().isEmpty()
                || !result.snapshot().orElseThrow().relationships().equals(reference.relationships())
                || !PaperWonderfulSnapshotAssets.slotValues(
                    result.snapshot().orElseThrow(), 2 * 1024 * 1024,
                    16 * 1024 * 1024).equals(PaperWonderfulSnapshotAssets.slotValues(
                    reference, 2 * 1024 * 1024, 16 * 1024 * 1024)))
            throw new IOException("Synthetic v4/v2 WCL storage compatibility failed");

        pdc.remove(genomeKey);
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 2 * 1024 * 1024);
            throw new IOException("Missing external schema genome reference was accepted");
        } catch (IOException expectedMissing) {
            if (!expectedMissing.getMessage().startsWith("Missing saved WCL string: "))
                throw expectedMissing;
        }
        pdc.put(genomeKey, "not-a-canonical-uuid");
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 2 * 1024 * 1024);
            throw new IOException("Invalid external genome reference was accepted");
        } catch (IOException expectedInvalid) {
            if (!expectedInvalid.getMessage().startsWith("Invalid saved WCL UUID: "))
                throw expectedInvalid;
        }
        pdc.put(genomeKey, "00000000-0000-0000-0000-00000000a0f1");
        pdc.put(versionKey, 99);
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 2 * 1024 * 1024);
            throw new IOException("Unsupported saved WCL schema was accepted");
        } catch (IOException invalidSchema) {
            if (!invalidSchema.getMessage().startsWith("Unsupported saved "))
                throw invalidSchema;
        }
        pdc.put(versionKey, providerId.equals("wwc") ? 4 : 2);
        String physicalKey = ns + (providerId.equals("wwc")
                ? ":equipment_shulker_box" : ":runtime_state");
        Object actualBytes = pdc.put(physicalKey, "not a byte array");
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 2 * 1024 * 1024);
            throw new IOException("Wrong-type saved WCL inventory was accepted");
        } catch (IOException badType) {
            if (!badType.getMessage().startsWith("Malformed/oversized saved WCL bytes: "))
                throw badType;
        }
        pdc.put(physicalKey, actualBytes);
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 1);
            throw new IOException("Over-budget saved WCL inventory was accepted");
        } catch (IOException overBudget) {
            if (!overBudget.getMessage().startsWith("Malformed/oversized saved WCL bytes: "))
                throw overBudget;
        }
        Object originalSpecies = cloned.put("id", "minecraft:cow");
        try {
            PaperWonderfulSavedPdc.reconstruct(id, providerId, cloned, 2 * 1024 * 1024);
            throw new IOException("Wrong species of saved WCL Entity was accepted");
        } catch (IOException badSpecies) {
            if (!"Saved WCL Entity species/provider mismatch".equals(badSpecies.getMessage()))
                throw badSpecies;
        }
        cloned.put("id", originalSpecies);
        getLogger().info("PAPER_A05_PRODUCT_SYNTHETIC_CURRENT_PDC_SCHEMA_PASSED"
                + " provider=" + providerId);
    }

    /**
     * This deliberately supplies isolated, unauthoritative NBT fragments to
     * check the product collector's fail-closed target discriminator. Only the
     * separately verified Anvil traversal above is a real source observation.
     */
    private void verifyProductSavedChunkCandidateNegatives(
            World world, WorldStorageDiscovery.Manifest manifest) throws IOException {
        WorldStorageDiscovery.SourceFile source = manifest.worldFiles().stream()
                .filter(f -> f.kind() == WorldStorageDiscovery.Kind.ENTITY)
                .findFirst().orElseThrow(() -> new IOException("No saved Entity source"));
        Map<String, Object> ordinary = Map.of(
                "id", "minecraft:chicken",
                "BukkitValues", Map.of("unrelatedplugin:flag", Integer.valueOf(1)));
        var vanillaChunk = new AnvilRegionReader.Chunk(61, 61,
                Map.of("Entities", List.of(ordinary)));
        if (!PaperWonderfulSavedChunkCandidates.collect(this, source, world.getUID(),
                vanillaChunk, new java.util.HashSet<>(), new java.util.HashSet<>(),
                2 * 1024 * 1024, 16 * 1024 * 1024).isEmpty())
            throw new IOException("Ordinary non-WCL chicken was counted as WCL storage");

        Map<String, Object> orphan = Map.of(
                "id", "minecraft:chicken",
                "BukkitValues", Map.of("whatawonderfulchicken:equipment_shulker_box",
                        new byte[]{1, 2, 3}));
        expectSavedCandidateFailure(source, world, orphan,
                "Saved WCL marker/species is incomplete or contradictory");
        Map<String, Object> wrongSpecies = Map.of(
                "id", "minecraft:cow",
                "BukkitValues", Map.of("whatawonderfulwolf:type", "wonderful_wolf"));
        expectSavedCandidateFailure(source, world, wrongSpecies,
                "Saved WCL marker/species is incomplete or contradictory");
        Map<String, Object> both = Map.of(
                "id", "minecraft:wolf",
                "BukkitValues", Map.of("whatawonderfulchicken:type", "wonderful_chicken",
                        "whatawonderfulwolf:type", "wonderful_wolf"));
        expectSavedCandidateFailure(source, world, both,
                "Saved Entity contains contradictory WWC/WWW identities");
        getLogger().info("PAPER_A05_PRODUCT_SAVED_WCL_UNKNOWN_OR_CONTRADICTORY_REJECTED_PASSED");
    }

    private void expectSavedCandidateFailure(
            WorldStorageDiscovery.SourceFile source, World world,
            Map<String, Object> entity, String reason) throws IOException {
        var chunk = new AnvilRegionReader.Chunk(61, 61,
                Map.of("Entities", List.of(entity)));
        try {
            PaperWonderfulSavedChunkCandidates.collect(this, source, world.getUID(),
                    chunk, new java.util.HashSet<>(), new java.util.HashSet<>(),
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            throw new IOException("Bad saved WCL candidate was accepted");
        } catch (IOException expected) {
            if (!reason.equals(expected.getMessage()))
                throw expected;
        }
    }

    private static UUID readSavedUuid(Object raw) throws IOException {
        if (!(raw instanceof int[] values) || values.length != 4)
            throw new IOException("Entity UUID is not canonical four-int saved NBT");
        long hi = ((long) values[0] << 32) | Integer.toUnsignedLong(values[1]);
        long lo = ((long) values[2] << 32) | Integer.toUnsignedLong(values[3]);
        return new UUID(hi, lo);
    }
}
