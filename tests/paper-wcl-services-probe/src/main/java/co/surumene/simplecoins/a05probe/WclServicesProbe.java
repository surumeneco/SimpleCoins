package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldSnapshotTraversal;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.SavedPlayerAssets;
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
import co.surumene.simplecoins.paper.PaperProfileBanSource;
import co.surumene.simplecoins.paper.PaperLoadedChunkAssets;
import co.surumene.simplecoins.paper.PaperWwcDisplayPhysicalAlias;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.ResolvedEntitySettlementImport;
import co.surumene.simplecoins.domain.PaperSavedWorldMetadataWitness;
import co.surumene.whatawonderfulchicken.WhatAWonderfulChickenPlugin;
import co.surumene.whatawonderfulchicken.service.WonderfulChickenStore;
import co.surumene.whatawonderfulchicken.data.WonderfulChickenData;
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
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.block.sign.Side;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Player;
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
    private static boolean withBlockLocker() {
        return Boolean.getBoolean("a05.probe.blocklocker");
    }

    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("PAPER_A05_PROBE_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        // A01 bounded investigation: a separate disposable process exercises
        // the product's existing seven-family fence. Never part of normal A05
        // reader proofs, distribution, or production startup.
        boolean addItemProbe = Boolean.getBoolean("a01.inventoryAddItemClosureProbe");
        boolean removeItemProbe = Boolean.getBoolean("a01.inventoryRemoveItemClosureProbe");
        boolean clearProbe = Boolean.getBoolean("a01.inventoryClearClosureProbe");
        boolean clearSlotProbe = Boolean.getBoolean("a01.inventoryClearSlotClosureProbe");
        boolean hopperProbe = Boolean.getBoolean("a01.inventoryHopperTransferClosureProbe");
        boolean hopperEventVetoProbe = Boolean.getBoolean("a01.inventoryHopperMoveEventVetoProbe");
        boolean hopperPrebodyProbe = Boolean.getBoolean("a01.inventoryHopperPrebodyProbe");
        boolean hopperOutputProbe = Boolean.getBoolean("a01.inventoryHopperOutputWriterProbe");
        boolean hopperOutputPrebodyProbe = Boolean.getBoolean("a01.inventoryHopperOutputPrebodyProbe");
        if (addItemProbe || removeItemProbe || clearProbe || clearSlotProbe
                || hopperProbe || hopperEventVetoProbe || hopperPrebodyProbe
                || hopperOutputProbe || hopperOutputPrebodyProbe) {
            // A test JVM admits exactly one sealed-writer probe: one product
            // generation cannot be re-armed after it is faulted or sealed.
            int modes = (addItemProbe ? 1 : 0) + (removeItemProbe ? 1 : 0)
                    + (clearProbe ? 1 : 0) + (clearSlotProbe ? 1 : 0)
                    + (hopperProbe ? 1 : 0) + (hopperEventVetoProbe ? 1 : 0)
                    + (hopperPrebodyProbe ? 1 : 0) + (hopperOutputProbe ? 1 : 0) + (hopperOutputPrebodyProbe ? 1 : 0);
            if (modes != 1) {
                getLogger().severe("PAPER_A01_MULTIPLE_CHEST_CLOSURE_PROBES_REJECTED");
                Runtime.getRuntime().halt(2);
                return;
            }
            Bukkit.getScheduler().runTaskLater(this, () -> {
                String failed = hopperOutputPrebodyProbe
                        ? "PAPER_A01_REAL_HOPPER_OUTPUT_PREBODY_PROBE_FAILED"
                        : hopperOutputProbe
                        ? "PAPER_A01_REAL_HOPPER_OUTPUT_PROBE_FAILED"
                        : hopperPrebodyProbe
                        ? "PAPER_A01_REAL_HOPPER_PREBODY_PROBE_FAILED"
                        : hopperEventVetoProbe
                                ? "PAPER_A01_REAL_HOPPER_EVENT_VETO_PROBE_FAILED"
                        : hopperProbe
                                ? "PAPER_A01_REAL_HOPPER_TRANSFER_CLOSURE_PROBE_FAILED"
                        : clearSlotProbe
                                ? "PAPER_A01_REAL_CHEST_CLEAR_SLOT_CLOSURE_PROBE_FAILED"
                        : clearProbe
                                ? "PAPER_A01_REAL_CHEST_CLEAR_CLOSURE_PROBE_FAILED"
                        : removeItemProbe
                                ? "PAPER_A01_REAL_CHEST_REMOVEITEM_CLOSURE_PROBE_FAILED"
                                : "PAPER_A01_REAL_CHEST_ADDITEM_CLOSURE_PROBE_FAILED";
                try {
                    if (hopperOutputPrebodyProbe) {
                        InventoryHopperOutputWriterProbe.beginRequirePrebody(this);
                        return; // Actual prebody positive control and 60 genuine ticks.
                    }
                    if (hopperOutputProbe) {
                        InventoryHopperOutputWriterProbe.begin(this);
                        return; // Two genuine tick intervals: PASSIVE then bounded SEALED.
                    }
                    if (hopperPrebodyProbe) {
                        InventoryHopperTransferClosureProbe.beginPrebody(this);
                        return; // Classify 60 actual ordinary server ticks.
                    }
                    if (hopperEventVetoProbe) {
                        InventoryHopperTransferClosureProbe.beginEventVeto(this);
                        return; // Real ticks complete before JVM exit.
                    }
                    if (hopperProbe) {
                        InventoryHopperTransferClosureProbe.begin(this);
                        return; // Completion is deferred until actual Hopper ticks.
                    }
                    String outcome = clearSlotProbe
                            ? InventoryClearSlotClosureProbe.execute()
                            : clearProbe
                                    ? InventoryClearClosureProbe.execute()
                            : removeItemProbe
                                    ? InventoryRemoveItemClosureProbe.execute()
                                    : InventoryAddItemClosureProbe.execute();
                    getLogger().info(outcome);
                    System.out.println(outcome);
                    System.out.flush();
                    // The limited proof intentionally leaves the fence sealed
                    // or faulted. Do not run later Paper shutdown saves.
                    Runtime.getRuntime().halt(0);
                } catch (Throwable failure) {
                    getLogger().log(Level.SEVERE, failed, failure);
                    System.err.println(failed);
                    System.err.flush();
                    Runtime.getRuntime().halt(2);
                }
            }, 50L);
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
                if (withBlockLocker()) {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin("BlockLocker");
                    if (plugin == null || !plugin.isEnabled())
                        throw new IOException("Real BlockLocker 1.15 plugin is required for this mode");
                    if (!plugin.getDescription().getVersion().equals("1.15"))
                        throw new IOException("Incorrect real BlockLocker fixture version");
                } else if (Bukkit.getPluginManager().getPlugin("BlockLocker") != null) {
                    throw new IOException("Legacy isolated WCL fixture must not load BlockLocker");
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
                if (Boolean.getBoolean("a01.moneyOwnerWriterProbe")) {
                    if (!"baseline".equals(phase) || withBlockLocker())
                        throw new IOException("A01 LIVE writer experiment must use isolated baseline only");
                    verifyActualWwcOwnerWriter(selected, world);
                }
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
                    case "reload-saved" -> {
                        // This JVM handles the entire authentic SAVED-source
                        // candidate + source/witness validations. No near
                        // Entity chunk load is allowed in the same process:
                        // that triggers WWC's legacy genome migration while
                        // other expensive fixture checks are in flight.
                        if (Boolean.getBoolean("a01.probe.offMainHybrid")) {
                            // Disposable startup experiment: prevent ordinary simulation and
                            // automatic saves during capture. Give already queued startup IO
                            // time to finish, without blocking the primary scheduler.
                            // Full source witnesses remain checked by the worker: this window
                            // does not assert that arbitrary plugin writers are frozen.
                            if (!Bukkit.getOnlinePlayers().isEmpty())
                                throw new IOException("Startup static window requires zero players");
                            Bukkit.getServer().getServerTickManager().setFrozen(true);
                            for (World loaded : Bukkit.getWorlds()) loaded.setAutoSave(false);
                            Bukkit.getScheduler().runTaskLater(this, () -> {
                                try {
                                    if (!Bukkit.getOnlinePlayers().isEmpty())
                                        throw new IOException("Player entered static window");
                                    getLogger().info("PAPER_A01_MANAGED_STATIC_WINDOW_TRIAL");
                                    verifyFarUnloadedStoredItems(world, selected);
                                } catch (Throwable failure) {
                                    getLogger().log(Level.SEVERE, "PAPER_A05_SERVICES_PROBE_FAILED", failure);
                                    Bukkit.shutdown();
                                }
                            }, 40L);
                        } else {
                            verifyFarUnloadedStoredItems(world, selected);
                            Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
                        }
                        return;
                    }
                    case "reload" -> {
                        // A second *independent* Paper JVM recovers the same
                        // persisted near WWC/WWW entities. Do not skip either
                        // proof or raise Paper's watchdog to mask interference.
                        reloadChickenEntity(world, selected);
                        Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
                        return;
                    }
                    default -> throw new IllegalArgumentException("Unknown A05 probe phase: " + phase);
                }
                verifyKnownWriterEpochCoverage();
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
            // The protected persistence fixture deliberately installs an
            // unmapped IP BAN, for which normal accounting must fail closed.
            // Keep the ordinary GUI scenario in the baseline without that fixture.
            if (!withBlockLocker()) {
                // WWW persists the GUI cargo only on close. Test the real
                // distributed holder, actual live provider, and committed store.
                var hidden=coins.create(CoinDenomination.ONE,1);
                var ability=co.surumene.www.domain.Ability.INVENTORY;
                var cargoFixture=individual.withAdminAbilityOverrides(Map.of(ability,4.0));
                www.loadedIndividuals().saveAndRegister(wolf,cargoFixture);
                www.abilityRuntime().refresh(wolf);
                // The admin override is further modified by personality, age and
                // injury. Build the real fixture from effective capacity, not the
                // requested override value or rounded GUI row size.
                int capacity=(int)Math.round(www.abilityRuntime().find(wolf.getUniqueId())
                        .orElseThrow().canonical(ability));
                if(capacity<1||capacity>=9)throw new IOException("Unexpected WWW fixture capacity: "+capacity);
                int visible=capacity-1;
                var cargoIndividual=cargoFixture.withStorage(individual.weapon(),
                        Map.of(visible,new ItemStackSnapshot(cargo.serializeAsBytes()),
                                capacity,new ItemStackSnapshot(hidden.serializeAsBytes())));
                www.loadedIndividuals().saveAndRegister(wolf,cargoIndividual);
                var cargoHolder=new co.surumene.www.ui.WonderfulWolfInventoryHolder(wolf.getUniqueId());
                var cargoGui=Bukkit.createInventory(cargoHolder,18);
                cargoHolder.inventory(cargoGui);
                cargoGui.setItem(9+visible,cargo.clone());
                cargoGui.setItem(9+capacity,ItemStack.of(Material.BLACK_STAINED_GLASS_PANE));
                String cargoLocation="world/"+world.getName()+"/entity/"+wolf.getUniqueId()+"/wcl/www/cargo:"+visible;
                String weaponLocation="world/"+world.getName()+"/entity/"+wolf.getUniqueId()+"/equipment/mainhand";
                var cargoEndpoint=co.surumene.simplecoins.domain.MemoryAccounting.Endpoint.dead(commander,
                        co.surumene.simplecoins.domain.AssetOwner.DeadReason.MISSING_PLAYERDATA,cargoLocation);
                var weaponEndpoint=co.surumene.simplecoins.domain.MemoryAccounting.Endpoint.dead(commander,
                        co.surumene.simplecoins.domain.AssetOwner.DeadReason.MISSING_PLAYERDATA,weaponLocation);
                var hiddenEndpoint=co.surumene.simplecoins.domain.MemoryAccounting.Endpoint.dead(commander,
                        co.surumene.simplecoins.domain.AssetOwner.DeadReason.MISSING_PLAYERDATA,
                        "world/"+world.getName()+"/entity/"+wolf.getUniqueId()+"/wcl/www/cargo:"+capacity);
                var ledger=new co.surumene.simplecoins.domain.MemoryAccounting(Map.of(),BigInteger.ZERO,
                        Map.of(cargoEndpoint,BigInteger.valueOf(5000),weaponEndpoint,BigInteger.valueOf(10000),
                                hiddenEndpoint,BigInteger.ONE));
                var fault=new java.util.concurrent.atomic.AtomicReference<Exception>();
                var local=new co.surumene.simplecoins.paper.PaperEntityAccounting(this,
                        new WorldScope(true,Set.of(),Set.of()),()->true,
                        moves->{if(ledger.recordNext(moves,Set.of())!=co.surumene.simplecoins.domain.MemoryAccounting.Outcome.APPLIED)
                            throw new IllegalStateException("WWW GUI delta rejected");},fault::set);
                local.openCargo(cargoGui);
                local.observeInventory(cargoGui);
                var smaller=coins.create(CoinDenomination.THOUSAND,2);
                cargoGui.setItem(9+visible,smaller);
                local.flush();
                if(fault.get()!=null||!ledger.snapshot().dead().equals(Map.of(cargoEndpoint,BigInteger.valueOf(2000),weaponEndpoint,BigInteger.valueOf(10000),hiddenEndpoint,BigInteger.ONE)))
                    throw new IOException("WWW open cargo did not reflect actual value: "+ledger.snapshot(),fault.get());
                local.observeInventory(cargoGui);
                cargoGui.clear(9+visible);
                local.flush();
                if(fault.get()!=null||!ledger.snapshot().dead().equals(Map.of(weaponEndpoint,BigInteger.valueOf(10000),hiddenEndpoint,BigInteger.ONE)))
                    throw new IOException("WWW empty cargo lost hidden storage or retained stale visible cargo: "+ledger.snapshot(),fault.get());
                local.observeInventory(cargoGui);
                www.loadedIndividuals().saveAndRegister(wolf,cargoIndividual.withStorage(cargoIndividual.weapon(),
                        Map.of(capacity,new ItemStackSnapshot(hidden.serializeAsBytes()))));
                local.closeCargo(cargoGui);
                local.flush();
                if(fault.get()!=null||!ledger.snapshot().dead().equals(Map.of(weaponEndpoint,BigInteger.valueOf(10000),hiddenEndpoint,BigInteger.ONE)))
                    throw new IOException("WWW close counted already observed cargo again: "+ledger.snapshot(),fault.get());
                www.loadedIndividuals().saveAndRegister(wolf,individual);
                www.abilityRuntime().refresh(wolf);
                getLogger().info("PAPER_A05_WWW_OPEN_CARGO_ACCOUNTED_ONCE_PASSED");
            }

            if (Boolean.getBoolean("a01.moneyOwnerWriterProbe")) {
                if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
                    throw new IOException("Real WWW writer requires zero-player Paper main thread");
                // A05 safety: reuse the REAL captured source and staged
                // candidate path, not a fabricated local pre/post boolean.
                // Capture WCL owner A before the actual WWW plugin writes B.
                // The earlier publisher-less success must stay compatible,
                // but THIS new candidate has stale WCL owner and must HOLD.
                var untrustedOldCapture = PaperCapturedHybridSettlement.capture(
                        List.of(world),
                        new PaperLiveUniverseAssets.Budget(
                                1, 4096, 4096, 4096, 131072, 1,
                                2 * 1024 * 1024, 16 * 1024 * 1024));
                untrustedOldCapture.includeLoadedWcl(this, wolf, "www",
                        2 * 1024 * 1024, 16 * 1024 * 1024);
                UUID secondCommander =
                        UUID.fromString("00000000-0000-0000-0000-00000000a073");
                byte[] originalMainhand =
                        wolf.getEquipment().getItemInMainHand().serializeAsBytes();
                // Actual shipped WWW loaded writer, not manually written PDC.
                var updated = individual.withCommandState(Mode.FOLLOW,
                        Optional.of(secondCommander), ActionDistance.NORMAL,
                        Optional.empty());
                writeWithAlreadyAdmittedOwnerProof("WWW",
                        () -> www.loadedIndividuals().saveAndRegister(wolf, updated));
                var observedNext = PaperWonderfulLiveCandidates.collect(
                        this, wolf, "www", new java.util.HashSet<>(),
                        2 * 1024 * 1024, 16 * 1024 * 1024);
                if (!commander.equals(candidate.ownerEvidence().owner())
                        || !secondCommander.equals(observedNext.ownerEvidence().owner())
                        || !secondCommander.equals(www.loadedIndividuals().find(wolf.getUniqueId())
                                .orElseThrow().commanderId().orElseThrow())
                        || !candidate.slotValues().equals(observedNext.slotValues())
                        || !BigInteger.valueOf(10000).equals(observedNext.slotValues().get("weapon"))
                        || !BigInteger.valueOf(5000).equals(observedNext.slotValues().get("cargo:7"))
                        || !Arrays.equals(originalMainhand,
                                wolf.getEquipment().getItemInMainHand().serializeAsBytes())
                        || !Arrays.equals(candidate.sourceItems().get("weapon").bytes(),
                                observedNext.sourceItems().get("weapon").bytes())
                        || !Arrays.equals(candidate.sourceItems().get("cargo:7").bytes(),
                                observedNext.sourceItems().get("cargo:7").bytes()))
                    throw new IOException("WWW real owner writer altered monetary ItemStacks"
                            + " or was invisible to the product WCL source");
                getLogger().info("PAPER_A01_REAL_WWW_MONETARY_COMMANDER_WRITER_VALUE_INVARIANT_PASSED"
                        + " value=15000 previous=" + commander + " new=" + secondCommander);
                verifyAgentBlocksLoadedOwnerWriter("WWW",
                        () -> www.loadedIndividuals().saveAndRegister(wolf,
                                updated.withCommandState(Mode.FOLLOW,
                                        Optional.of(UUID.fromString(
                                                "00000000-0000-0000-0000-00000000a074")),
                                        ActionDistance.NORMAL, Optional.empty())),
                        () -> {
                            verifyDirectNativeMainhandUnderClosedWriterGate(
                                    wolf, originalMainhand, secondCommander,
                                    observedNext.slotValues());
                            var retained = PaperWonderfulLiveCandidates.collect(
                                    this, wolf, "www", new java.util.HashSet<>(),
                                    2 * 1024 * 1024, 16 * 1024 * 1024);
                            return secondCommander.equals(retained.ownerEvidence().owner())
                                    && observedNext.slotValues().equals(retained.slotValues())
                                    && Arrays.equals(originalMainhand,
                                            wolf.getEquipment().getItemInMainHand()
                                                    .serializeAsBytes())
                                    && Arrays.equals(observedNext.sourceItems().get("weapon").bytes(),
                                            retained.sourceItems().get("weapon").bytes())
                                    && Arrays.equals(observedNext.sourceItems().get("cargo:7").bytes(),
                                            retained.sourceItems().get("cargo:7").bytes());
                        });
                // Without an in-process WCL freshness check, the unchanged
                // monetary sum would let this stale owner-A capture stage after
                // actual WWW's owner-B mutation, while saved MCA is irrelevant.
                try {
                    var candidateAfterWriter = untrustedOldCapture.stageCandidate(
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
                    candidateAfterWriter.pending().commit();
                    throw new IOException("Unsafe stale WWW owner was staged after actual plugin write");
                } catch (IOException failedClosed) {
                    if (!failedClosed.getMessage().startsWith(
                            "LIVE WCL holder changed during saved stage:"))
                        throw failedClosed;
                }
                getLogger().info("PAPER_A05_REAL_WWW_STALE_LIVE_OWNER_CANDIDATE_REJECTED_PASSED");
                // Real Paper source drift after WCL owner/cargo reread: the
                // managed WWW item remains 10,000C, but vanilla mainhand
                // changes independently. A same-WCL-only freshness check
                // would still publish the *old* native mainhand value.
                var staleNativeMirror = PaperCapturedHybridSettlement.capture(
                        List.of(world),
                        new PaperLiveUniverseAssets.Budget(
                                1, 4096, 4096, 4096, 131072, 1,
                                2 * 1024 * 1024, 16 * 1024 * 1024));
                staleNativeMirror.includeLoadedWcl(this, wolf, "www",
                        2 * 1024 * 1024, 16 * 1024 * 1024);
                ItemStack originalDisplayed = wolf.getEquipment().getItemInMainHand().clone();
                try {
                    wolf.getEquipment().setItemInMainHand(new ItemStack(Material.STONE));
                    if (Arrays.equals(originalDisplayed.serializeAsBytes(),
                            wolf.getEquipment().getItemInMainHand().serializeAsBytes()))
                        throw new IOException("Real Paper equipment did not change");
                    var unchangedWcl = PaperWonderfulLiveCandidates.collect(
                            this, wolf, "www", new java.util.HashSet<>(),
                            2 * 1024 * 1024, 16 * 1024 * 1024);
                    if (!secondCommander.equals(unchangedWcl.ownerEvidence().owner())
                            || !BigInteger.valueOf(10000).equals(
                                    unchangedWcl.slotValues().get("weapon"))
                            || !BigInteger.valueOf(5000).equals(
                                    unchangedWcl.slotValues().get("cargo:7"))
                            || !Arrays.equals(observedNext.sourceItems().get("weapon").bytes(),
                                    unchangedWcl.sourceItems().get("weapon").bytes()))
                        throw new IOException("WWW provider changed with vanilla-only equipment edit");
                    try {
                        var invalidStaged = staleNativeMirror.stageCandidate(
                                new WorldStorageDiscovery.Manifest(List.of(), Map.of()),
                                new WorldScope(true, Set.of(), Set.of()), Set.of(),
                                (sourceWorld, position) -> null,
                                (sourceWorld, savedChunk) -> Map.of(),
                                2 * 1024 * 1024, 16 * 1024 * 1024,
                                (source, value) -> new SettlementAccumulator.Asset(
                                        source.id(), value.physicalKey(), value.world(),
                                        value.physicalKey().equals("world/" + world.getName()
                                                + "/entity/" + wolf.getUniqueId()
                                                + "/equipment/mainhand") ? secondCommander : null,
                                        false, false, false,
                                        value.amount(), value.physicalKey()));
                        invalidStaged.pending().commit();
                        throw new IOException("Unsafe stale vanilla mainhand mirror was staged");
                    } catch (IOException rejected) {
                        if (rejected.getMessage() == null
                                || !rejected.getMessage().startsWith(
                                        "LIVE WCL holder changed during saved stage:"))
                            throw rejected;
                    }
                } finally {
                    wolf.getEquipment().setItemInMainHand(originalDisplayed);
                }
                getLogger().info("PAPER_A05_STALE_NATIVE_EQUIPMENT_MIRROR_REJECTED_PASSED");
            }
        } finally {
            www.loadedIndividuals().unregister(wolf);
            wolf.remove();
        }
    }


    /**
     * A01/G0: invoke the real shipped WWC plugin's currently loaded
     * WonderfulChickenStore.save (not synthetic PDC fabrication). An actual
     * Chicken's WCL-owned 10,000C does not change, while its follow_target
     * changes. This is a positive reachability proof of one LIVE owner writer,
     * NOT a claim that it is blocked by a global settlement writer epoch.
     */
    @FunctionalInterface
    private interface CheckedSourceUnchanged {
        boolean evaluate() throws Exception;
    }

    /**
     * Issue-work/verification only: prove the *actual shipped* owner writer
     * cannot enter once the test JVM Agent closes its two known entries.
     * Does not claim global writer coverage, already-in-flight drain or G0.
     */
    /**
     * Form a test-only in-JVM proof from five *observed, invoked and returned*
     * known writer surfaces. The negative CI controls deliberately omit the
     * real Chest Inventory hook: an incomplete source roster cannot mint it.
     */
    /** Test a short effect inside the SAME monitor as token verification. */
    private void verifyAtomicKnownSourceCommit(Class<?> hook, Object token,
            boolean allFiveObserved) throws Exception {
        Class<?> epoch = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook$KnownSourcesClosedEpoch",
                true, null);
        var action = hook.getMethod("runUnderKnownSourcesClosedEpoch", epoch, Runnable.class);
        var effects = new java.util.concurrent.atomic.AtomicInteger();
        if (!allFiveObserved) {
            try {
                action.invoke(null, null, (Runnable) () -> effects.incrementAndGet());
                throw new IOException("Incomplete writer roster ran guarded action");
            } catch (java.lang.reflect.InvocationTargetException rejected) {
                if (!(rejected.getCause() instanceof IllegalStateException))
                    throw rejected;
            }
            if (effects.get() != 0)
                throw new IOException("Partial source minted an actual callback effect");
            getLogger().info("PAPER_A01_INCOMPLETE_SOURCE_ATOMIC_ACTION_REJECTED_PASSED");
            return;
        }
        if (token == null) throw new IOException("Full source proof token missing");
        var started = new java.util.concurrent.CountDownLatch(1);
        var reopened = new java.util.concurrent.CountDownLatch(1);
        var contenderError = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread contender = new Thread(() -> {
            started.countDown();
            try {
                hook.getMethod("reopenForProbe").invoke(null);
            } catch (Throwable failure) {
                contenderError.set(failure);
            } finally {
                reopened.countDown();
            }
        }, "a01-real-writer-epoch-reopen-contender");
        contender.setDaemon(true);
        try {
            action.invoke(null, token, (Runnable) () -> {
                try {
                    contender.start();
                    if (!started.await(2000L, java.util.concurrent.TimeUnit.MILLISECONDS))
                        throw new IOException("Reopen contender never started");
                    if (reopened.await(180L, java.util.concurrent.TimeUnit.MILLISECONDS))
                        throw new IOException("Writer gate reopened during guarded callback");
                    if (!(Boolean) hook.getMethod("knownEpochStillCurrent", epoch)
                            .invoke(null, token))
                        throw new IOException("Known source changed during guarded callback");
                    // Reentrant same-thread reopen must also be forbidden.
                    try {
                        hook.getMethod("reopenForProbe").invoke(null);
                        throw new IOException("Guarded callback reopened its own writer gate");
                    } catch (java.lang.reflect.InvocationTargetException denied) {
                        if (!(denied.getCause() instanceof IllegalStateException))
                            throw denied;
                    }
                    effects.incrementAndGet();
                } catch (Exception failure) {
                    throw new IllegalStateException(
                            "Actual known-epoch callback validation failed", failure);
                }
            });
        } finally {
            contender.join(5000L);
        }
        if (contender.isAlive() || contenderError.get() != null
                || reopened.getCount() != 0L || effects.get() != 1
                || (Boolean) hook.getMethod("isClosed").invoke(null))
            throw new IOException("Atomic known-epoch action was not isolated until its return",
                    contenderError.get());
        getLogger().info("PAPER_A01_KNOWN_SOURCE_ATOMIC_ACTION_BLOCKED_REOPEN_PASSED");
    }

    private void verifyKnownWriterEpochCoverage() throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.coverageProof")) return;
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Known-writer closure evidence requires isolated main-thread Paper");
        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
        boolean expectFull = Boolean.getBoolean("a01.liveOwnerAgent.instrumentInventory");
        boolean observed = (Boolean) hook.getMethod("knownCoverageComplete").invoke(null);
        if (observed != expectFull)
            throw new IOException("Unexpected real writer transformation/return roster: "
                    + observed + ", expected=" + expectFull);
        int active = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "active");
        if (active != 0) throw new IOException("Cannot test covered generation with active writer");
        hook.getMethod("closeForProbe").invoke(null);
        Object token = null;
        try {
            if (!(Boolean) hook.getMethod("awaitDrain", long.class).invoke(null, 800L))
                throw new IOException("Known-writer epoch did not drain");
            try {
                token = hook.getMethod("mintKnownSourcesClosedEpoch").invoke(null);
            } catch (java.lang.reflect.InvocationTargetException rejected) {
                if (expectFull || !(rejected.getCause() instanceof IllegalStateException denied)
                        || !"Missing observed known writer coverage or drain"
                                .equals(denied.getMessage()))
                    throw rejected;
            }
            if (expectFull) {
                if (token == null || !(Boolean) hook.getMethod("knownEpochStillCurrent",
                        token.getClass()).invoke(null, token))
                    throw new IOException("Fully observed known writer epoch not stable while closed");
                getLogger().info("PAPER_A01_KNOWN_SOURCE_CLOSED_EPOCH_MINTED_FROM_REAL_HOOKS_PASSED");
            } else {
                if (token != null)
                    throw new IOException("Missing actual inventory writer still minted an epoch");
                getLogger().info("PAPER_A01_INCOMPLETE_SOURCE_ROSTER_EPOCH_MINT_REJECTED_PASSED");
            }
            verifyAtomicKnownSourceCommit(hook, token, expectFull);
        } finally {
            if ((Boolean) hook.getMethod("isClosed").invoke(null))
                hook.getMethod("reopenForProbe").invoke(null);
        }
        if (expectFull) {
            var current = hook.getMethod("knownEpochStillCurrent", token.getClass());
            if ((Boolean) current.invoke(null, token))
                throw new IOException("Known writer epoch survived admission reopening");
            hook.getMethod("closeForProbe").invoke(null);
            try {
                if ((Boolean) current.invoke(null, token))
                    throw new IOException("Stale closed epoch was replayed in a later generation");
                var atomic = hook.getMethod("runUnderKnownSourcesClosedEpoch",
                        token.getClass(), Runnable.class);
                var staleEffects = new java.util.concurrent.atomic.AtomicInteger();
                try {
                    atomic.invoke(null, token, (Runnable) () -> staleEffects.incrementAndGet());
                    throw new IOException("Stale generation executed protected action");
                } catch (java.lang.reflect.InvocationTargetException rejected) {
                    if (!(rejected.getCause() instanceof IllegalStateException))
                        throw rejected;
                }
                if (staleEffects.get() != 0)
                    throw new IOException("Stale token produced side effects");
            } finally {
                hook.getMethod("reopenForProbe").invoke(null);
            }
            getLogger().info("PAPER_A01_KNOWN_SOURCE_EPOCH_REOPEN_AND_REPLAY_DENIED_PASSED");
        }
    }

    /**
     * Real block-chest coin mutation through the normal Bukkit inventory API,
     * in a disposable preloaded chunk of the isolated Paper fixture.
     */
    private void verifyActualChestInventoryMutationUnderClosedGate(World world)
            throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.directInventoryProof")) return;
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Chest monetary writer proof must use isolated main-thread baseline");

        var block = world.getBlockAt(49, 120, 49);
        Material initialType = block.getType();
        // The test controls a fresh disposable world; all mutations are later
        // removed, and the loaded chunk is owned by the fixture.
        block.setType(Material.CHEST);
        try {
            if (!(block.getState() instanceof org.bukkit.block.Chest chest))
                throw new IOException("Physical test Chest did not materialize");
            var inventory = chest.getBlockInventory();
            getLogger().info("PAPER_A01_REAL_BLOCK_INVENTORY_IMPL_CLASS="
                    + inventory.getClass().getName());
            var coinCodec = new CoinItemCodec(CoinItemCodec.defaultNames());
            ItemStack originalMoney = coinCodec.create(CoinDenomination.TEN_THOUSAND, 1);
            inventory.setItem(0, originalMoney.clone());
            byte[] expected = originalMoney.serializeAsBytes();
            ItemStack observed = inventory.getItem(0);
            if (observed == null || observed.getAmount() != 1
                    || coinCodec.identify(observed).orElseThrow() != CoinDenomination.TEN_THOUSAND
                    || !Arrays.equals(expected, observed.serializeAsBytes()))
                throw new IOException("Real Chest monetary source never materialized");

            Class<?> hook = Class.forName(
                    "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
            if ((Boolean) hook.getMethod("isClosed").invoke(null))
                throw new IOException("Cannot begin independent Chest probe with closed gate");
            hook.getMethod("closeForProbe").invoke(null);
            try {
                boolean uninstrumented = Boolean.getBoolean(
                        "a01.liveOwnerAgent.expectInventoryBypass");
                if (uninstrumented) {
                    inventory.setItem(0, new ItemStack(Material.STONE));
                    ItemStack changed = inventory.getItem(0);
                    if (changed == null || changed.getType() != Material.STONE
                            || Arrays.equals(expected, changed.serializeAsBytes()))
                        throw new IOException("Uncontrolled CraftInventory write did not run");
                    getLogger().info("PAPER_A01_BLOCK_CHEST_DIRECT_SETITEM_BYPASS_REPRODUCED");
                    inventory.setItem(0, originalMoney.clone());
                } else {
                    int previousDenied = (Integer) hook.getMethod("count", String.class)
                            .invoke(null, "inventoryDenied");
                    try {
                        inventory.setItem(0, new ItemStack(Material.STONE));
                        throw new IOException("Unfenced Inventory.setItem removed real 10000C");
                    } catch (IllegalStateException denied) {
                        if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                            throw denied;
                    }
                    if ((Integer) hook.getMethod("count", String.class)
                            .invoke(null, "inventoryDenied") != previousDenied + 1)
                        throw new IOException("Inventory writer closed gate was not invoked");
                    getLogger().info("PAPER_A01_BLOCK_CHEST_SETITEM_REJECTED_PASSED");
                }
                if (Boolean.getBoolean("a01.liveOwnerAgent.probeBulkInventoryBypass")) {
                    // Real Paper 26.2 build134 CraftInventory#setContents
                    // delegates to setItem(int,ItemStack). This follows the
                    // actual stack trace from the bounded source-first Red
                    // attempt; do NOT add an unjustified second Agent hook.
                    ItemStack[] overwritten = inventory.getContents().clone();
                    overwritten[0] = new ItemStack(Material.STONE);
                    int beforeBulkDenials = (Integer) hook.getMethod("count", String.class)
                            .invoke(null, "inventoryDenied");
                    try {
                        inventory.setContents(overwritten);
                        throw new IOException(
                                "CraftInventory.setContents bypassed already fenced setItem");
                    } catch (IllegalStateException denied) {
                        if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                            throw denied;
                    }
                    if ((Integer) hook.getMethod("count", String.class)
                            .invoke(null, "inventoryDenied") != beforeBulkDenials + 1)
                        throw new IOException(
                                "Actual setContents did not delegate to instrumented setItem");
                    getLogger().info(
                            "PAPER_A01_CHEST_SETCONTENTS_DELEGATES_TO_GATED_SETITEM_PASSED");
                }
                ItemStack retained = inventory.getItem(0);
                if (retained == null || retained.getAmount() != 1
                        || coinCodec.identify(retained).orElseThrow()
                                != CoinDenomination.TEN_THOUSAND
                        || !Arrays.equals(expected, retained.serializeAsBytes()))
                    throw new IOException("Chest coin changed across direct writer test");
            } finally {
                hook.getMethod("reopenForProbe").invoke(null);
            }
        } finally {
            block.setType(initialType);
        }
    }

    /**
     * A separate Paper equipment writer bypasses WWW's registered store and
     * even the already-instrumented PDC mutation surface. Use the real loaded
     * WWW monetary mirror (10,000C native mainhand / 5,000C cargo).
     */
    private void verifyDirectNativeMainhandUnderClosedWriterGate(
            Wolf wolf, byte[] originalNative, UUID expectedCommander,
            Map<String, BigInteger> expectedValues) throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.directEquipmentProof")) return;
        if (!Bukkit.isPrimaryThread()) throw new IOException("Native equipment proof off main thread");
        var equipment = wolf.getEquipment();
        if (equipment == null) throw new IOException("Actual WWW wolf has no equipment");
        getLogger().info("PAPER_A01_REAL_EQUIPMENT_IMPL_CLASS=" + equipment.getClass().getName());
        if (!Arrays.equals(originalNative, equipment.getItemInMainHand().serializeAsBytes()))
            throw new IOException("WWW native mirror invalid before closed equipment proof");
        boolean uninstrumented = Boolean.getBoolean(
                "a01.liveOwnerAgent.expectEquipmentBypass");
        if (uninstrumented) {
            var original = equipment.getItemInMainHand().clone();
            try {
                equipment.setItemInMainHand(new ItemStack(Material.STONE));
                var wcl = PaperWonderfulLiveCandidates.collect(
                        this, wolf, "www", new java.util.HashSet<>(),
                        2 * 1024 * 1024, 16 * 1024 * 1024);
                if (Arrays.equals(originalNative,
                                equipment.getItemInMainHand().serializeAsBytes())
                        || !expectedCommander.equals(wcl.ownerEvidence().owner())
                        || !expectedValues.equals(wcl.slotValues()))
                    throw new IOException("Independent native equipment bypass not observed");
                getLogger().info("PAPER_A01_NATIVE_MAINHAND_BYPASS_NEGATIVE_REPRODUCED");
            } finally {
                equipment.setItemInMainHand(original);
            }
            if (!Arrays.equals(originalNative,
                    equipment.getItemInMainHand().serializeAsBytes()))
                throw new IOException("Failed to restore native equipment after bypass probe");
            return;
        }

        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
        if (!(Boolean) hook.getMethod("isClosed").invoke(null))
            throw new IOException("Equipment proof needs actual closed WCL gate");
        int previousDenials = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "equipmentDenied");
        try {
            equipment.setItemInMainHand(new ItemStack(Material.STONE));
            throw new IOException("Unfenced native equipment setter changed the 10000C mirror");
        } catch (IllegalStateException denied) {
            if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                throw denied;
        }
        int denied = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "equipmentDenied");
        var retained = PaperWonderfulLiveCandidates.collect(
                this, wolf, "www", new java.util.HashSet<>(),
                2 * 1024 * 1024, 16 * 1024 * 1024);
        if (denied != previousDenials + 1
                || !Arrays.equals(originalNative,
                    equipment.getItemInMainHand().serializeAsBytes())
                || !expectedCommander.equals(retained.ownerEvidence().owner())
                || !expectedValues.equals(retained.slotValues()))
            throw new IOException("Equipment bypass escaped shared writer gate");
        getLogger().info("PAPER_A01_NATIVE_MAINHAND_SETTER_REJECTED_PASSED");
    }

    /**
     * A separate writer can modify owner PDC directly without entering
     * WonderfulChickenStore.save. The NEGATIVE control must prove the bypass;
     * the positive experiment must reject both setter and remover while the
     * same already-closed writer gate is held.
     */
    private void verifyPaperPdcDirectOwnerWriterUnderClosedGate(
            WonderfulStorageReader reader, Chicken chicken, World world,
            PersistentDataContainer pdc, NamespacedKey ownerKey, UUID owner)
            throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.directPdcProof")) return;
        String ownerBefore = pdc.get(ownerKey, PersistentDataType.STRING);
        if (!owner.toString().equals(ownerBefore))
            throw new IOException("Incorrect precondition for direct PDC mutation counterexample");
        String foreign = "00000000-0000-0000-0000-00000000a076";
        getLogger().info("PAPER_A01_REAL_PDC_IMPL_CLASS=" + pdc.getClass().getName());
        boolean uninstrumented = Boolean.getBoolean("a01.liveOwnerAgent.expectPdcBypass");
        if (uninstrumented) {
            try {
                pdc.set(ownerKey, PersistentDataType.STRING, foreign);
                var changed = success(reader.readStored(
                        chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                        "Direct Paper PDC owner bypass negative observation");
                if (!foreign.equals(pdc.get(ownerKey, PersistentDataType.STRING))
                        || !UUID.fromString(foreign).equals(
                                changed.relationships().get("follow_target")))
                    throw new IOException("PDC mutation bypass was not actually observed");
                getLogger().info("PAPER_A01_PDC_OWNER_DIRECT_BYPASS_NEGATIVE_REPRODUCED");
            } finally {
                pdc.set(ownerKey, PersistentDataType.STRING, ownerBefore);
            }
            var restored = success(reader.readStored(
                    chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                    "Restored owner after negative-control direct PDC bypass");
            if (!owner.equals(restored.relationships().get("follow_target")))
                throw new IOException("PDC negative-control restoration failed");
            return;
        }

        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
        if (!(Boolean) hook.getMethod("isClosed").invoke(null))
            throw new IOException("Direct PDC probe must run inside same closed writer interval");
        int previousDenials = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "pdcDenied");
        try {
            pdc.set(ownerKey, PersistentDataType.STRING, foreign);
            throw new IOException("Unsafe direct Paper PDC set passed the closed writer gate");
        } catch (IllegalStateException denied) {
            if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                throw denied;
        }
        try {
            pdc.remove(ownerKey);
            throw new IOException("Unsafe direct Paper PDC remove passed the closed writer gate");
        } catch (IllegalStateException denied) {
            if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                throw denied;
        }
        int actualDenials = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "pdcDenied");
        var retained = success(reader.readStored(
                chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                "Actual PDC set and remove closed writer evidence");
        if (actualDenials != previousDenials + 2
                || !owner.toString().equals(pdc.get(ownerKey, PersistentDataType.STRING))
                || !owner.equals(retained.relationships().get("follow_target")))
            throw new IOException("PDC direct owner writer escaped the closed gate");
        getLogger().info("PAPER_A01_PDC_DIRECT_SET_AND_REMOVE_REJECTED_PASSED");
    }

    /**
     * Hold the REAL provider method after its instrumented entry but before
     * the plugin's owner/PDC write. A controller thread may touch only the
     * bootstrap hook: all Bukkit and WCL reads remain on Paper's main thread.
     */
    private void writeWithAlreadyAdmittedOwnerProof(String writer, Runnable actualWrite)
            throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.inflight")) {
            actualWrite.run();
            return;
        }
        if (!Bukkit.isPrimaryThread())
            throw new IOException("Real owner writer must execute on Paper main thread");
        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
        String key = writer.toLowerCase(java.util.Locale.ROOT);
        int previousReturns = (Integer) hook.getMethod("count", String.class)
                .invoke(null, key + "Returns");
        int previousPdcReturns = (Integer) hook.getMethod("count", String.class)
                .invoke(null, "pdcReturns");
        hook.getMethod("armHold", String.class).invoke(null, writer);
        var controllerFailure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread controller = new Thread(() -> {
            try {
                if (!(Boolean) hook.getMethod("awaitHeldEntry", String.class, long.class)
                        .invoke(null, writer, 8000L))
                    throw new IOException("Real " + writer + " method did not enter armed hold");
                int during = (Integer) hook.getMethod("count", String.class)
                        .invoke(null, "active");
                if (during != 1)
                    throw new IOException("Expected one actual " + writer
                            + " in-flight writer, observed " + during);
                hook.getMethod("closeForProbe").invoke(null);
                if ((Boolean) hook.getMethod("awaitDrain", long.class)
                        .invoke(null, 250L))
                    throw new IOException("A01 prematurely reported drained while body held");
                getLogger().info("PAPER_A01_LIVE_AGENT_" + writer
                        + "_ALREADY_ADMITTED_DRAIN_BLOCKED_PASSED");
                hook.getMethod("releaseHeldEntry", String.class).invoke(null, writer);
                if (!(Boolean) hook.getMethod("awaitDrain", long.class)
                        .invoke(null, 8000L))
                    throw new IOException("A01 " + writer + " did not drain after normal return");
                if ((Integer) hook.getMethod("count", String.class)
                        .invoke(null, "active") != 0)
                    throw new IOException("A01 reported drain but retained active writer");
            } catch (Throwable failed) {
                controllerFailure.set(failed);
                // Never strand the Paper main thread on a failed test.
                try {
                    hook.getMethod("releaseHeldEntry", String.class).invoke(null, writer);
                } catch (Throwable releaseFailure) {
                    failed.addSuppressed(releaseFailure);
                }
            }
        }, "a01-real-" + key + "-writer-drain-controller");
        controller.setDaemon(true);
        controller.start();
        Throwable actualFailure = null;
        try {
            actualWrite.run();
        } catch (Throwable failed) {
            actualFailure = failed;
        }
        controller.join(11000L);
        if (controller.isAlive())
            throw new IOException("A01 real owner writer drain controller hung");
        if (actualFailure != null)
            throw new IOException("Real " + writer + " provider writer failed after admitted hold",
                    actualFailure);
        if (controllerFailure.get() != null)
            throw new IOException("A01 real " + writer + " drain proof failed",
                    controllerFailure.get());
        int nowReturns = (Integer) hook.getMethod("count", String.class)
                .invoke(null, key + "Returns");
        if (nowReturns != previousReturns + 1
                || !(Boolean) hook.getMethod("isClosed").invoke(null))
            throw new IOException("Owner writer did not return exactly once under closed gate");
        if ("WWC".equals(writer) && Boolean.getBoolean("a01.liveOwnerAgent.instrumentPdc")
                && (Integer) hook.getMethod("count", String.class)
                        .invoke(null, "pdcReturns") <= previousPdcReturns)
            throw new IOException("Previously admitted WWC writer failed to enter nested PDC mutator");
        hook.getMethod("clearHold", String.class).invoke(null, writer);
        getLogger().info("PAPER_A01_LIVE_AGENT_" + writer
                + "_ALREADY_ADMITTED_DRAINED_AFTER_REAL_RETURN_PASSED");
        // Intentional: retain the closed gate for a subsequent NEW writer
        // rejection against the same accounting generation.
    }

    private void verifyAgentBlocksLoadedOwnerWriter(String writer, Runnable attempted,
            CheckedSourceUnchanged sourceUnchanged) throws Exception {
        if (!Boolean.getBoolean("a01.liveOwnerAgent.probe")) return;
        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.a01agent.LiveOwnerWriterHook", true, null);
        String key = writer.toLowerCase(java.util.Locale.ROOT);
        int previousReturns = (Integer) hook.getMethod("count", String.class)
                .invoke(null, key + "Returns");
        int previousDenials = (Integer) hook.getMethod("count", String.class)
                .invoke(null, key + "Denied");
        if (previousReturns <= 0)
            throw new IOException("Real " + writer + " Agent method has no genuine prior return");
        if (!(Boolean) hook.getMethod("isClosed").invoke(null))
            hook.getMethod("closeForProbe").invoke(null);
        try {
            try {
                attempted.run();
                throw new IOException("A01 live owner writer bypassed closed Agent: " + writer);
            } catch (IllegalStateException denied) {
                if (!"A01 live provider writer fenced".equals(denied.getMessage()))
                    throw denied;
            }
            if (!sourceUnchanged.evaluate())
                throw new IOException("Genuine WCL owner or money changed after Agent denial");
            int actualDenials = (Integer) hook.getMethod("count", String.class)
                    .invoke(null, key + "Denied");
            int actualReturns = (Integer) hook.getMethod("count", String.class)
                    .invoke(null, key + "Returns");
            int active = (Integer) hook.getMethod("count", String.class)
                    .invoke(null, "active");
            if (actualDenials != previousDenials + 1
                    || actualReturns != previousReturns || active != 0)
                throw new IOException("Unverifiable Agent admission/return evidence: "
                        + writer + " denied=" + actualDenials
                        + " returns=" + actualReturns + " active=" + active);
        } finally {
            hook.getMethod("reopenForProbe").invoke(null);
        }
        getLogger().info("PAPER_A01_LIVE_AGENT_" + writer
                + "_CLOSED_REAL_WRITER_REJECTED_PASSED");
    }

    private void verifyActualWwcOwnerWriter(
            Map<String, WonderfulStorageReader> readers, World world) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Real WWC owner writer requires default-deny main-thread baseline");
        var plugin = (WhatAWonderfulChickenPlugin)
                Bukkit.getPluginManager().getPlugin("WhatAWonderfulChicken");
        if (plugin == null || !plugin.isEnabled())
            throw new IOException("Shipped WWC must be active for real store write");
        // Use the *actual registered plugin's* initialized store, not an
        // independent clone or a test-created fake persistence implementation.
        var field = WhatAWonderfulChickenPlugin.class.getDeclaredField("store");
        field.setAccessible(true);
        Object instance = field.get(plugin);
        if (!(instance instanceof WonderfulChickenStore store))
            throw new IOException("WWC plugin does not own a genuine loaded store");
        Chunk fixture = world.getChunkAt(3, 3);
        if (!fixture.isLoaded())
            throw new IOException("WWC owner writer chunk not physically loaded");
        UUID firstOwner = UUID.fromString("00000000-0000-0000-0000-00000000a071");
        UUID secondOwner = UUID.fromString("00000000-0000-0000-0000-00000000a072");
        Chicken chicken = world.spawn(new Location(world, 52.5, 120, 52.5), Chicken.class);
        try {
            var data = new WonderfulChickenData();
            data.followTarget(firstOwner);
            data.shulkerBox(ItemStack.deserializeBytes(createMoneyShulker()));
            store.save(chicken, data);
            var pdc = chicken.getPersistentDataContainer();
            var ownerKey = new NamespacedKey("whatawonderfulchicken", "follow_target");
            var shulkerKey = new NamespacedKey("whatawonderfulchicken", "equipment_shulker_box");
            byte[] firstBytes = pdc.get(shulkerKey, PersistentDataType.BYTE_ARRAY);
            if (!firstOwner.toString().equals(pdc.get(ownerKey, PersistentDataType.STRING))
                    || firstBytes == null)
                throw new IOException("Real WWC save did not publish owner and saved shulker PDC");
            var before = success(readers.get("wwc").readStored(
                    chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                    "WWC genuine store before owner move");
            var firstValues = PaperWonderfulSnapshotAssets.slotValues(
                    PaperWonderfulLoadedEvidence.readStored(this,
                            chicken.getUniqueId(), world.getUID(), "wwc", pdc)
                            .snapshot().orElseThrow(),
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            if (!firstOwner.equals(before.relationships().get("follow_target"))
                    || before.items().size() != 1
                    || !BigInteger.valueOf(10000).equals(firstValues.get("shulker")))
                throw new IOException("WWC genuine first owner/value not observed by product");
            // One genuine plugin-owned LIVE writer changes the monetary owner,
            // without changing coins, their physical slot or any player count.
            String location="world/"+world.getName()+"/entity/"+chicken.getUniqueId()+"/wcl/wwc/shulker";
            var oldEndpoint=co.surumene.simplecoins.domain.MemoryAccounting.Endpoint.dead(firstOwner,
                    co.surumene.simplecoins.domain.AssetOwner.DeadReason.MISSING_PLAYERDATA,location);
            var newEndpoint=co.surumene.simplecoins.domain.MemoryAccounting.Endpoint.dead(secondOwner,
                    co.surumene.simplecoins.domain.AssetOwner.DeadReason.MISSING_PLAYERDATA,location);
            var accounting=new co.surumene.simplecoins.domain.MemoryAccounting(Map.of(),BigInteger.ZERO,
                    Map.of(oldEndpoint,BigInteger.valueOf(10000)));
            var observedFailure=new java.util.concurrent.atomic.AtomicReference<Exception>();
            var local=new co.surumene.simplecoins.paper.PaperEntityAccounting(this,
                    new co.surumene.simplecoins.domain.WorldScope(true,Set.of(),Set.of()),()->true,
                    moves->{if(accounting.recordNext(moves,Set.of())!=co.surumene.simplecoins.domain.MemoryAccounting.Outcome.APPLIED)
                        throw new IllegalStateException("Normal WCL delta rejected");},observedFailure::set);
            // Run ordinary cargo regression BEFORE the separate Agent deliberately
            // closes its writer gate. Its negative control must remain unchanged.
            var holder=new co.surumene.whatawonderfulchicken.gui.WonderfulChickenInventoryHolder(chicken.getUniqueId());
            var gui=Bukkit.createInventory(holder,36);
            holder.inventory(gui);
            var coinCodec=new CoinItemCodec(CoinItemCodec.defaultNames());
            ItemStack originalBox=data.shulkerBox().clone();
            var initialMeta=(BlockStateMeta)originalBox.getItemMeta();
            var initialBox=(ShulkerBox)initialMeta.getBlockState();
            for(int i=0;i<27;i++)gui.setItem(9+i,initialBox.getInventory().getItem(i));
            local.openCargo(gui);
            local.observeInventory(gui);
            // Replace all visible cargo with 5000C while the persisted box is
            // still 10000C. The live GUI, not the stored bytes, owns the value.
            for(int i=9;i<36;i++)gui.clear(i);
            gui.setItem(9,coinCodec.create(CoinDenomination.FIVE_THOUSAND,1));
            local.flush();
            if(observedFailure.get()!=null||!accounting.snapshot().dead().equals(Map.of(oldEndpoint,BigInteger.valueOf(5000))))
                throw new IOException("WWC deferred cargo not observed in live GUI",observedFailure.get());
            local.observeInventory(gui);
            gui.clear(9);
            local.flush();
            if(observedFailure.get()!=null||!accounting.snapshot().dead().isEmpty())
                throw new IOException("WWC empty live cargo still counted persisted coins",observedFailure.get());
            local.observeInventory(gui);
            ItemStack emptyBox=originalBox.clone();
            var emptyMeta=(BlockStateMeta)emptyBox.getItemMeta();
            var emptyState=(ShulkerBox)emptyMeta.getBlockState();
            emptyState.getInventory().clear();
            emptyMeta.setBlockState(emptyState);
            emptyBox.setItemMeta(emptyMeta);
            data.shulkerBox(emptyBox);
            store.save(chicken,data);
            local.closeCargo(gui);
            local.flush();
            if(observedFailure.get()!=null||!accounting.snapshot().dead().isEmpty())
                throw new IOException("WWC cargo close duplicated or lost storage",observedFailure.get());
            // Restore the initial genuine fixture before testing the owner move.
            local.observeEntity(chicken);
            data.shulkerBox(originalBox);
            store.save(chicken,data);
            local.flush();
            if(observedFailure.get()!=null||!accounting.snapshot().dead().equals(Map.of(oldEndpoint,BigInteger.valueOf(10000))))
                throw new IOException("WWC restored fixture was not accounted",observedFailure.get());
            getLogger().info("PAPER_A05_WWC_OPEN_CARGO_ACCOUNTED_ONCE_PASSED");
            local.observeEntity(chicken);
            data.followTarget(secondOwner);
            writeWithAlreadyAdmittedOwnerProof("WWC", () -> store.save(chicken, data));
            local.flush();
            if(observedFailure.get()!=null)throw new IOException("Normal WCL read failed",observedFailure.get());
            if(!accounting.snapshot().dead().equals(Map.of(newEndpoint,BigInteger.valueOf(10000))))
                throw new IOException("Normal WCL owner move not conserved: "+accounting.snapshot());
            getLogger().info("PAPER_A05_WWC_NORMAL_OWNER_MOVEMENT_PASSED");

            var after = success(readers.get("wwc").readStored(
                    chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                    "WWC genuine store after owner move");
            var finalValues = PaperWonderfulSnapshotAssets.slotValues(
                    PaperWonderfulLoadedEvidence.readStored(this,
                            chicken.getUniqueId(), world.getUID(), "wwc", pdc)
                            .snapshot().orElseThrow(),
                    2 * 1024 * 1024, 16 * 1024 * 1024);
            if (!secondOwner.toString().equals(pdc.get(ownerKey, PersistentDataType.STRING))
                    || !secondOwner.equals(after.relationships().get("follow_target"))
                    || !firstOwner.equals(before.relationships().get("follow_target"))
                    || !Arrays.equals(firstBytes,
                            pdc.get(shulkerKey, PersistentDataType.BYTE_ARRAY))
                    || !Arrays.equals(before.items().getFirst().bytes(),
                            after.items().getFirst().bytes())
                    || !firstValues.equals(finalValues)
                    || !BigInteger.valueOf(10000).equals(finalValues.get("shulker"))
                    || !Bukkit.getOnlinePlayers().isEmpty())
                throw new IOException("Real WWC write changed money or failed to change LIVE owner");
            getLogger().info("PAPER_A01_REAL_WWC_MONETARY_OWNER_WRITER_VALUE_INVARIANT_PASSED"
                    + " value=10000 previous=" + firstOwner + " new=" + secondOwner);
            verifyAgentBlocksLoadedOwnerWriter("WWC",
                    () -> {
                        // Never mutate the store's already cached data object:
                        // the actual writer is what the Agent must reject.
                        var uncommitted = data.copy();
                        uncommitted.followTarget(UUID.fromString(
                                "00000000-0000-0000-0000-00000000a075"));
                        store.save(chicken, uncommitted);
                    },
                    () -> {
                        verifyPaperPdcDirectOwnerWriterUnderClosedGate(
                                readers.get("wwc"), chicken, world, pdc, ownerKey, secondOwner);
                        var retained = success(readers.get("wwc").readStored(
                                chicken.getUniqueId(), world.getUID(), Optional.empty(), pdc),
                                "WWC genuine store after closed writer rejection");
                        return secondOwner.toString().equals(
                                    pdc.get(ownerKey, PersistentDataType.STRING))
                                && secondOwner.equals(retained.relationships().get("follow_target"))
                                && Arrays.equals(firstBytes,
                                    pdc.get(shulkerKey, PersistentDataType.BYTE_ARRAY))
                                && Arrays.equals(after.items().getFirst().bytes(),
                                    retained.items().getFirst().bytes());
                    });
            verifyActualChestInventoryMutationUnderClosedGate(world);
        } finally {
            store.invalidate(chicken);
            chicken.remove();
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
        // This persisted near WWW is part of the discovered SAVED universe.
        // Give its runtime owner a real Paper tame-owner witness and ensure
        // the WCL weapon is physically mirrored by the vanilla mainhand.
        // Otherwise whole-source staging must HOLD, as the initial CI proved.
        wolf.setOwner(Bukkit.getOfflinePlayer(WOLF_OWNER));
        wolf.getEquipment().setItemInMainHand(
                coins.create(CoinDenomination.TEN_THOUSAND, 1));
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
        // The older no-BlockLocker 55,000C fixture deliberately remains
        // without playerdata. The REAL protected-world branch must distinguish
        // a profile-CLEAR, present player from a profile-BANNED, present player.
        if (withBlockLocker()) {
            persistDetachedEmptyOwner(PERSISTENCE_FOLLOW, "A06ClearOwner");
            persistDetachedEmptyOwner(WOLF_COMMANDER, "A06BannedCmdr");
        }
        world.save();
        getLogger().info("PAPER_A05_WWW_ENTITY_PERSISTED_BEFORE_RESTART_PASSED"
                + " entity=" + wolf.getUniqueId() + " world=" + world.getUID());
        getLogger().info("PAPER_A05_WWC_ENTITY_PERSISTED_BEFORE_RESTART_PASSED"
                + " entity=" + chicken.getUniqueId()
                + " chunk=4,4 world=" + world.getUID());
        // Do not remove fixture before shutdown: the next JVM must load it from the real region.
    }

    /**
     * Genuine Paper 26.2 native save of a detached offline ServerPlayer.
     * This fixture never connects/logs in; the file is created by the real
     * CraftPlayer.saveData -> NMS PlayerDataStorage.save path. It is not
     * synthetically authored NBT or a counterfeit discovery Manifest.
     */
    private void persistDetachedEmptyOwner(UUID id, String name) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IOException("Detached owner source needs an isolated Paper primary thread");
        Path file = getServer().getWorldContainer().toPath().toAbsolutePath().normalize()
                .resolve("world/players/data/" + id + ".dat");
        if (Files.exists(file))
            throw new IOException("Detached owner fixture file already existed: " + file);
        Object server = getServer().getClass().getMethod("getServer").invoke(getServer());
        Class<?> serverType = Class.forName("net.minecraft.server.MinecraftServer");
        Object nativeWorld = serverType.getMethod("overworld").invoke(server);
        Class<?> worldType = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class)
                .newInstance(id, name);
        Class<?> infoType = Class.forName("net.minecraft.server.level.ClientInformation");
        Object info = infoType.getMethod("createDefault").invoke(null);
        Class<?> playerType = Class.forName("net.minecraft.server.level.ServerPlayer");
        Object nativePlayer = playerType.getConstructor(
                serverType, worldType, profileType, infoType)
                .newInstance(server, nativeWorld, profile, info);
        Object wrapped = playerType.getMethod("getBukkitEntity").invoke(nativePlayer);
        if (!(wrapped instanceof Player player) || player.isOnline()
                || !id.equals(player.getUniqueId()) || !player.getInventory().isEmpty())
            throw new IOException("Detached offline player was not an empty, isolated Paper subject");
        player.saveData();
        if (!Files.isRegularFile(file))
            throw new IOException("Paper native save did not persist canonical offline UUID.dat");
        var nbt = PlayerDataReader.read(file, 2 * 1024 * 1024, 16 * 1024 * 1024);
        if (!SavedPlayerAssets.extract(nbt).isEmpty())
            throw new IOException("Unexpected money in the actual saved offline playerdata");
        getLogger().info("PAPER_A06_ACTUAL_PAPER_OFFLINE_OWNERDATA_SAVED_PASSED"
                + " id=" + id + " world=world");
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
        // Additional authentic saved BlockEntity, intentionally in the same
        // FAR UNLOADED Chunk as the external-WCL entities. The LIVE source
        // capture cannot see it, so the 5,000C must be accounted via the
        // selected SAVED terrain Region and source-scoped BlockLocker index.
        var chestBlock = world.getBlockAt((int) anchor + 5, 121, (int) anchor + 5);
        chestBlock.setType(Material.CHEST);
        if (!(chestBlock.getState() instanceof org.bukkit.block.Chest chest))
            throw new IOException("Far physical chest failed to initialize");
        // Modify the captured TileState inventory, then commit the exact
        // same snapshot once. Editing the live inventory before update()
        // would be overwritten by the stale captured (empty) BlockState.
        chest.getSnapshotInventory().setItem(0,
                coins.create(CoinDenomination.FIVE_THOUSAND, 1));
        if (!chest.update(true, false))
            throw new IOException("Far physical chest BlockState update failed");
        if (!(chestBlock.getState() instanceof org.bukkit.block.Chest persistedChest)
                || persistedChest.getBlockInventory().getItem(0) == null)
            throw new IOException("Far physical 5000C chest was not filled");
        if (withBlockLocker()) {
            int x = (int) anchor, z = (int) anchor;
            // Real distributed BlockLocker is enabled during the first
            // physical Paper world write. Same FAR saved terrain as both
            // actual WWC and WWW entities, and never loaded in process B.
            attachBlockLockerSign(world, x + 5, 121, z + 5,
                    "FixtureOwner#" + PERSISTENCE_FOLLOW, null);
            var privateEvidence = co.surumene.simplecoins.paper.BlockLockerLiveEvidence
                    .observe(chestBlock);
            if (!PERSISTENCE_FOLLOW.equals(privateEvidence.owner())
                    || privateEvidence.everyoneProtected())
                throw new IOException("Real BlockLocker PRIVATE source did not own 5000C chest");
            var freeBlock = world.getBlockAt(x + 13, 121, z + 5);
            freeBlock.setType(Material.CHEST);
            if (!(freeBlock.getState() instanceof org.bukkit.block.Chest freeChest))
                throw new IOException("Failed to initialize real Everyone money chest");
            freeChest.getSnapshotInventory().setItem(0,
                    coins.create(CoinDenomination.THOUSAND, 1));
            if (!freeChest.update(true, false))
                throw new IOException("Failed to persist real Everyone money chest");
            attachBlockLockerSign(world, x + 13, 121, z + 5,
                    "UnresolvedOwner", "[Everyone]");
            var freeEvidence = co.surumene.simplecoins.paper.BlockLockerLiveEvidence
                    .observe(freeBlock);
            if (freeEvidence.owner() != null || !freeEvidence.everyoneProtected())
                throw new IOException("Real BlockLocker Everyone source was not effective");
            getLogger().info("PAPER_A04_A05_REAL_BLOCKLOCKER_BOTH_PHYSICAL_SIGNS_SEEDED_PASSED");
        }
        Files.writeString(farWitness("wwc-uuid.txt"), chicken.getUniqueId().toString() + "\n");
        Files.writeString(farWitness("www-uuid.txt"), wolf.getUniqueId().toString() + "\n");
        Files.writeString(farWitness("wwc-shulker.b64"),
                Base64.getEncoder().encodeToString(shulker) + "\n");
        Files.writeString(farWitness("www-runtime.b64"),
                Base64.getEncoder().encodeToString(runtime) + "\n");
        getLogger().info("PAPER_A05_FAR_TWO_STORED_PROVIDER_FIXTURES_SAVED_PASSED"
                + " chunk=" + FAR_CHUNK + "," + FAR_CHUNK);
    }

    /** Genuine v1.15 wall-sign fixture, not an artificial saved owner index. */
    private static void attachBlockLockerSign(
            World world, int x, int y, int z,
            String mainProfile, String secondary) throws IOException {
        var block = world.getBlockAt(x, y, z + 1);
        block.setType(Material.OAK_WALL_SIGN);
        WallSign direction = (WallSign) block.getBlockData();
        direction.setFacing(BlockFace.SOUTH);
        block.setBlockData(direction);
        if (!(block.getState() instanceof Sign sign))
            throw new IOException("Real BlockLocker wall-sign fixture did not initialize");
        sign.getSide(Side.FRONT).setLine(0, "[Private]");
        sign.getSide(Side.FRONT).setLine(1, mainProfile);
        if (secondary != null)
            sign.getSide(Side.FRONT).setLine(2, secondary);
        if (!sign.update(true, false))
            throw new IOException("Real BlockLocker wall-sign was not persisted");
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

        // Paper 26.2 stores playerdata under the shared LEVEL root
        // (world/players/data), not under the dimension's WorldFolder.
        // Derive this root from the ACTUAL Paper world and require it to
        // match the selected world name. Supplying no shared roots would
        // silently omit real UUID.dat sources while scanning all Anvil files.
        Path observedWorldFolder = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path sharedLevelRoot = getServer().getWorldContainer().toPath()
                .toAbsolutePath().normalize().resolve(world.getName());
        if (!observedWorldFolder.startsWith(sharedLevelRoot)
                || (withBlockLocker()
                    && !Files.isDirectory(sharedLevelRoot.resolve("players/data"))))
            throw new IOException("Paper 26.2 actual world and shared playerdata root disagree:"
                    + " worldFolder=" + observedWorldFolder + " levelRoot=" + sharedLevelRoot);
        // When WorldFolder is itself the level root its walk already includes
        // players/data. Only add the same root separately when Paper exposes a
        // per-dimension folder; otherwise doing both duplicates one UUID.dat.
        Set<Path> extraSharedRoots = observedWorldFolder.equals(sharedLevelRoot)
                ? Set.of() : Set.of(sharedLevelRoot);
        WorldStorageDiscovery.Manifest boundedManifest = WorldStorageDiscovery.discover(
                Map.of(world.getName(), observedWorldFolder),
                new WorldScope(true, Set.of(), Set.of()), 100000,
                extraSharedRoots);
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
                            // The actual manifest determines whether a
                            // profile-CLEAR WWC owner has playerdata. This
                            // local saved-only stage still does not prove an
                            // all-provider BAN epoch or publish any balance.
                            boolean hasClearOwnerData = withBlockLocker();
                            if (hasClearOwnerData
                                    ? !BigInteger.ZERO.equals(localResult.dead())
                                        || !BigInteger.valueOf(10000).equals(
                                                localResult.statistics().worldTotal())
                                        || !localResult.personalBalances().equals(
                                                Map.of(PERSISTENCE_FOLLOW, BigInteger.valueOf(10000)))
                                    : !BigInteger.valueOf(10000).equals(localResult.dead())
                                        || !BigInteger.ZERO.equals(localResult.statistics().worldTotal())
                                        || localResult.deadLocations().stream().noneMatch(loc ->
                                                PERSISTENCE_FOLLOW.equals(loc.owner())
                                                && BigInteger.valueOf(10000).equals(loc.value())))
                                throw new IOException("SAVED WWC local ownership contradicts real playerdata"
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
                                                    WOLF_COMMANDER, withBlockLocker(),
                                                    withBlockLocker())),
                                    sharedWallets);
                            var allWcl = new PaperWonderfulSavedSettlementSource(
                                    WclServicesProbe.this, Map.of(world.getName(), world.getUID()),
                                    boundedManifest,
                                    withBlockLocker() ? Set.of(WOLF_COMMANDER) : Set.of(),
                                    null, Set.of(),
                                    2 * 1024 * 1024, 16 * 1024 * 1024);
                            allWcl.stage(source, saved, once, sharedWallets);
                            once.completeSource(sourceId);
                            var onceResult = once.commit();
                            if (!BigInteger.valueOf(withBlockLocker() ? 15000 : 25000)
                                        .equals(onceResult.dead())
                                    || !BigInteger.valueOf(withBlockLocker() ? 10000 : 0)
                                        .equals(onceResult.statistics().worldTotal()))
                                throw new IOException("Real SAVED WWW 10000C mirror counted incorrectly"
                                        + " dead=" + onceResult.dead()
                                        + " personal=" + onceResult.personalBalances()
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
        verifyWholeHybridSavedWclCandidate(world, boundedManifest, wwcUuid, wwwUuid);
    }


    /**
     * Cross-source bounded proof: actually run the existing product hybrid
     * candidate pipeline against the SAME authentic discovered WorldFolder
     * and both saved WCL providers. The global source/writer epoch remains
     * deliberately unproven; no result is published or admitted.
     */
    private void verifyWholeHybridSavedWclCandidate(
            World world, WorldStorageDiscovery.Manifest manifest,
            UUID chickenId, UUID wolfId) throws Exception {
        UUID nearChickenId = UUID.fromString(Files.readString(persistedIdFile()).strip());
        UUID nearWolfId = UUID.fromString(Files.readString(persistedWolfIdFile()).strip());
        if (Set.of(chickenId, wolfId, nearChickenId, nearWolfId).size() != 4)
            throw new IOException("The full saved source fixture must contain four distinct WCL holders");
        if (!Bukkit.isPrimaryThread() || world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK)
                || manifest.origin() == null)
            throw new IOException("Whole WCL integration requires authentic SAVED source discovery");
        if (withBlockLocker()) {
            if (!manifest.playerDataFiles().keySet().equals(
                    Set.of(PERSISTENCE_FOLLOW, WOLF_COMMANDER)))
                throw new IOException("Real protected Paper fixture must discover both owner UUID.dat");
            for (UUID id : Set.of(PERSISTENCE_FOLLOW, WOLF_COMMANDER)) {
                Path source = manifest.playerDataFiles().get(id);
                if (source == null || !Files.isRegularFile(source)
                        || !SavedPlayerAssets.extract(
                                PlayerDataReader.read(source, 2 * 1024 * 1024, 16 * 1024 * 1024))
                                .isEmpty())
                    throw new IOException("Paper ownerdata absent or contains unexpected money: " + id);
            }
        } else if (!manifest.playerDataFiles().isEmpty()) {
            throw new IOException("Baseline fixture unexpectedly acquired a playerdata source");
        }
        var scope = new WorldScope(true, Set.of(), Set.of());
        if (!manifest.origin().scope().equals(scope))
            throw new IOException("Whole WCL fixture changed source discovery scope");
        final String sourceWorldName = world.getName();
        final UUID sourceWorldId = world.getUID();
        var owners = (co.surumene.simplecoins.domain.SettlementScanRunner.EntityOwnerLookup)
                (sourceWorld, chunk) -> {
                    if (!sourceWorld.equals(sourceWorldName))
                        throw new IOException("Unexpected saved Entity World source");
                    // Both far and near saved fixtures are mandatory members
                    // of the discovered source universe; never silently
                    // discard a second WCL holder to force a 25,000C total.
                    return Map.of(
                            chickenId.toString(),
                            new ResolvedEntitySettlementImport.Ownership(
                                    PERSISTENCE_FOLLOW, false, false),
                            nearChickenId.toString(),
                            new ResolvedEntitySettlementImport.Ownership(
                                    PERSISTENCE_FOLLOW, false, false),
                            wolfId.toString(),
                            new ResolvedEntitySettlementImport.Ownership(
                                    WOLF_COMMANDER, false, false),
                            nearWolfId.toString(),
                            new ResolvedEntitySettlementImport.Ownership(
                                    WOLF_COMMANDER, false, false));
                };
        var noPositiveLive = (co.surumene.simplecoins.domain.HybridLiveSourceStage.Classifier)
                (source, value) -> {
                    if (value.amount().signum() != 0)
                        throw new IOException("Unexpected unverified positive LIVE asset: "
                                + value.physicalKey());
                    return new SettlementAccumulator.Asset(source.id(), value.physicalKey(),
                            value.world(), null, false, false, false,
                            value.amount(), value.physicalKey());
                };
        var budgets = new PaperLiveUniverseAssets.Budget(
                1, 4096, 4096, 4096, 131072, 1,
                2 * 1024 * 1024, 16 * 1024 * 1024);
        int chestX = FAR_CHUNK * 16 + 5;
        int chestZ = FAR_CHUNK * 16 + 5;
        int everyoneX = FAR_CHUNK * 16 + 13;
        boolean protectedMode = withBlockLocker();
        // In protected mode these two real physical chests were signed while
        // the actual BlockLocker 1.15 JAR was loaded during Paper process A.
        // A saved sign source still cannot establish a global writer epoch.
        if (protectedMode != (Bukkit.getPluginManager().getPlugin("BlockLocker") != null))
            throw new IOException("BlockLocker installed population disagrees with fixture mode");
        var terrain = co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations.capture(
                manifest, scope, 2 * 1024 * 1024, 16 * 1024 * 1024,
                4096, Set.of("[Private]"), Set.of("[More Users]"),
                Set.of("Everyone"), raw -> raw);
        // Full discovered terrain may contain other genuine vanilla chests
        // from generated structures. Refusing or ignoring them just to keep
        // a one-chest oracle would misrepresent the required source universe.
        long exactMonetaryChest = terrain.chests().stream().filter(observed ->
                observed.world().equals(world.getName())
                && observed.observation().storageMembers().stream().anyMatch(p ->
                        p.x() == chestX && p.y() == 121 && p.z() == chestZ)).count();
        if (exactMonetaryChest != 1)
            throw new IOException("Exact physical SAVED chest missing/ambiguous"
                    + " expected=" + chestX + ",121," + chestZ
                    + " matches=" + exactMonetaryChest
                    + " actualChestGroups=" + terrain.chests().size()
                    + " observed=" + terrain.chests().stream().limit(12)
                        .map(e -> e.observation().storageMembers().toString()).toList());
        if (protectedMode) {
            long expectedEveryone = terrain.chests().stream().filter(observed ->
                    observed.world().equals(world.getName())
                    && observed.observation().storageMembers().stream().anyMatch(p ->
                            p.x() == everyoneX && p.y() == 121 && p.z() == chestZ)).count();
            if (expectedEveryone != 1)
                throw new IOException("Missing or ambiguous real SAVED Everyone money chest");
        }
        // ExternalProof flags here mean only the isolated, physically
        // observed Paper fixture: verified source, all attached signs,
        // deduplicated chest, and a bounded stable file witness. No promise
        // about the production deployment's writer or BAN closure.
        var knownNonProtection =
                (co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations
                        .Snapshot.IndependentProofs) observed -> {
                    var signs = co.surumene.simplecoins.domain
                            .SavedBlockLockerSignSummary.summarize(observed.observation());
                    boolean isPrivate = observed.observation().storageMembers().stream()
                            .anyMatch(p -> p.x() == chestX && p.y() == 121 && p.z() == chestZ);
                    boolean isEveryone = observed.observation().storageMembers().stream()
                            .anyMatch(p -> p.x() == everyoneX && p.y() == 121 && p.z() == chestZ);
                    if (protectedMode && isPrivate) {
                        if (!signs.protectionSignObserved() || signs.mainOwnerConflict()
                                || !signs.verifiedMainOwner().equals(Optional.of(PERSISTENCE_FOLLOW))
                                || signs.everyoneEffective() || !signs.everyoneAbsent())
                            throw new IOException("Real saved BlockLocker PRIVATE owner differs");
                        return new co.surumene.simplecoins.domain.SavedBlockLockerOwnerCandidate
                                .ExternalProof(true, true, true, true, false);
                    }
                    if (protectedMode && isEveryone) {
                        if (!signs.protectionSignObserved() || signs.mainOwnerConflict()
                                || !signs.everyoneEffective() || signs.everyoneAbsent()
                                || signs.verifiedMainOwner().isPresent())
                            throw new IOException("Real saved BlockLocker Everyone is not effective");
                        return new co.surumene.simplecoins.domain.SavedBlockLockerOwnerCandidate
                                .ExternalProof(true, true, true, true, false);
                    }
                    if (signs.protectionSignObserved())
                        throw new IOException("Unexpected protected saved BlockLocker chest");
                    return new co.surumene.simplecoins.domain.SavedBlockLockerOwnerCandidate
                            .ExternalProof(true, true, true, true, true);
                };
        var unknownProtection =
                (co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations
                        .Snapshot.IndependentProofs) observed ->
                        co.surumene.simplecoins.domain.SavedBlockLockerOwnerCandidate
                                .ExternalProof.unknown();

        // Non-BlockLocker baseline retains the isolated fixture input. In
        // the real BlockLocker/WCL integration, capture the *actual* official
        // Paper profile BAN status of both saved monetary owners. Neither
        // source IDs nor status may be synthesized from a fixture Boolean.
        String fixtureProvider = protectedMode
                ? PaperProfileBanSource.SOURCE_ID : "fixture-ban-status";
        var clear = protectedMode
                ? PaperProfileBanSource.observe(PERSISTENCE_FOLLOW)
                : new co.surumene.simplecoins.domain.BanEvidence.Source(
                        fixtureProvider,
                        co.surumene.simplecoins.domain.BanEvidence.Status.CLEAR);
        var commander = protectedMode
                ? PaperProfileBanSource.observe(WOLF_COMMANDER) : clear;
        if (protectedMode) {
            if (!PaperProfileBanSource.SOURCE_ID.equals(clear.id())
                    || clear.status() != co.surumene.simplecoins.domain.BanEvidence.Status.CLEAR
                    || !PaperProfileBanSource.SOURCE_ID.equals(commander.id())
                    || commander.status() != co.surumene.simplecoins.domain.BanEvidence.Status.BANNED)
                throw new IOException("Real Paper profile BAN identities/status did not match actual fixture");
            // The real Paper IP ban list has a distinct target namespace.
            // It cannot be joined to either offline UUID without an
            // independently authenticated IP→UUID association.
            @SuppressWarnings("deprecation")
            var actualIpEntries = Bukkit.getBanList(org.bukkit.BanList.Type.IP).getEntries();
            if (actualIpEntries.size() != 1
                    || actualIpEntries.stream().noneMatch(e ->
                            "203.0.113.42".equals(String.valueOf(e.getTarget()))))
                throw new IOException("Expected exact distinct real Paper IP BAN source");
            // With paper-ip declared mandatory but NO verified association
            // for these UUIDs, only profile data is insufficient. A complete
            // BanSourceCoverage must refuse rather than silently infer CLEAR.
            try {
                co.surumene.simplecoins.domain.BanSourceCoverage.capture(
                        Set.of(PaperProfileBanSource.SOURCE_ID, "paper-ip"),
                        Map.of(PERSISTENCE_FOLLOW, List.of(clear),
                               WOLF_COMMANDER, List.of(commander)));
                throw new IOException("Incomplete IP BAN identity evidence incorrectly completed");
            } catch (IOException expected) {
                if (!expected.getMessage().contains("Incomplete BAN provider coverage"))
                    throw expected;
            }
            getLogger().info("PAPER_A06_REAL_PROFILE_BANNED_AND_CLEAR_OWNERS_PASSED"
                    + " clearUuid=" + PERSISTENCE_FOLLOW
                    + " bannedUuid=" + WOLF_COMMANDER);
            getLogger().info("PAPER_A06_REAL_IP_BAN_UNKNOWN_UUID_MAPPING_HOLD_PASSED");
        }
        // Isolated test-limited provider set is deliberately *only* profile
        // BAN for protectedMode. No global IP or external-provider completeness
        // can be derived from this local candidate.
        var covered = co.surumene.simplecoins.domain.BanSourceCoverage.capture(
                Set.of(fixtureProvider),
                Map.of(PERSISTENCE_FOLLOW, List.of(clear),
                       WOLF_COMMANDER, List.of(commander)));
        if (protectedMode && (!covered.currentlyBanned(WOLF_COMMANDER)
                || covered.currentlyBanned(PERSISTENCE_FOLLOW)))
            throw new IOException("Authentic Paper profile BAN was not retained by coverage");
        // Explicit fixture-only ownership for OTHER saved container types.
        // The A04 BlockLocker source proof is valid only for physical chests.
        // The product must never silently assume a barrel/furnace/shulker is
        // FREE; the caller supplies this independent source-scoped authority.
        var nonChestOwners =
                (co.surumene.simplecoins.domain.SettlementScanRunner.SourceBlockOwnerLookup)
                (source, chunk, position) -> {
                    if (source.kind() != WorldStorageDiscovery.Kind.REGION
                            || !manifest.worldFiles().contains(source)
                            || !source.world().equals(sourceWorldName))
                        throw new IOException("Non-chest SAVED fixture owner lacks declared proof");
                    return new co.surumene.simplecoins.domain.BlockEntitySettlementImport
                            .Ownership(null, false, false, false);
                };

        // All real SAVED blocks and WCL slots are staged in ONE accumulated
        // candidate. The product router sends actual chests to A04 and every
        // other physically verified container to the independent resolver.
        var first = PaperCapturedHybridSettlement.capture(List.of(world), budgets);
        var live = first.includeDiscoveredLoadedWcl(this);
        if (live.holders().stream().anyMatch(e ->
                e.entity().getUniqueId().equals(chickenId)
                || e.entity().getUniqueId().equals(wolfId)
                || e.entity().getUniqueId().equals(nearChickenId)
                || e.entity().getUniqueId().equals(nearWolfId)))
            throw new IOException("Unloaded SAVED WCL fixture leaked into LIVE discovery");
        var selected = first.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage(
                this, Map.of(world.getName(), world.getUID()), manifest, scope,
                covered, terrain, knownNonProtection, nonChestOwners, owners,
                2 * 1024 * 1024, 16 * 1024 * 1024, noPositiveLive);
        // Reuse the very same physically verified BlockLocker/WCL/BAN Stage,
        // not a newly fabricated accumulator, as the source for the bounded
        // product A01 publisher in this isolated Paper test-only mode.
        boolean testBoundedPublication = Boolean.getBoolean("a01.probe.realSourcePublish");
        SettlementAccumulator.Result stagedResult = testBoundedPublication
                ? BoundedStagedPublicationProbe.commitAndVerify(selected, manifest)
                : selected.pending().commit(); // old local-only fixture behavior
        verifyCompleteUnpublishedWclCandidate(selected, manifest,
                chickenId, wolfId, nearChickenId, nearWolfId, protectedMode, stagedResult);
        if (testBoundedPublication) {
            getLogger().info("PAPER_A01_G1_REAL_SAVED_STAGE_ATOMIC_PUBLISH_BOUNDED_PASS"
                    + " totalPhysical=" + stagedResult.statistics().worldTotal().add(stagedResult.dead())
                    + " personal=" + stagedResult.statistics().personal()
                    + " free=" + stagedResult.free()
                    + " dead=" + stagedResult.dead()
                    + " TEST_ONLY_NO_WRITER_CLOSURE_NO_REAL_ADMISSION");
        }
        getLogger().info("PAPER_A05_FULL_HYBRID_FOUR_SAVED_WWC_WWW_50000_ONCE_PASSED");
        if (protectedMode) {
            getLogger().info("PAPER_A06_REAL_PLAYERDATA_BAN_P25000_D30000_F1000_PASSED");
            getLogger().info(
                    "PAPER_A04_A05_REAL_BLOCKLOCKER_WCL_FULL_SAVED_56000"
                    + "_PRIVATE_PERSONAL_EVERYONE_FREE_PASSED");
        } else {
            getLogger().info("PAPER_A05_FULL_HYBRID_FIXTURE_BAN_COVERED_STAGING_PASSED");
            getLogger().info("PAPER_A04_A05_FULL_HYBRID_REAL_SAVED_CHEST_WCL_55000_PASSED");
        }

        // 3. Missing BAN evidence for the actual WCL commander cannot be
        //    treated as CLEAR, even though the chest is verified FREE.
        var incomplete = co.surumene.simplecoins.domain.BanSourceCoverage.capture(
                Set.of(fixtureProvider),
                Map.of(PERSISTENCE_FOLLOW, List.of(clear)));
        var third = PaperCapturedHybridSettlement.capture(List.of(world), budgets);
        third.includeDiscoveredLoadedWcl(this);
        boolean absentOwnerRejected = false;
        try {
            third.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage(
                    this, Map.of(world.getName(), world.getUID()), manifest, scope,
                    incomplete, terrain, knownNonProtection, nonChestOwners, owners,
                    2 * 1024 * 1024, 16 * 1024 * 1024, noPositiveLive);
        } catch (IOException expected) {
            absentOwnerRejected = expected.getMessage() != null
                    && expected.getMessage().contains("Missing complete BAN source coverage for ");
            if (!absentOwnerRejected) throw expected;
        }
        if (!absentOwnerRejected)
            throw new IOException("Missing actual WCL BAN evidence returned a hybrid candidate");
        getLogger().info("PAPER_A05_FULL_HYBRID_MISSING_OWNER_BAN_REJECTED_PASSED");

        // 4. UNKNOWN local BlockLocker proof makes a fully populated WCL
        //    candidate impossible, and MUST consume this LIVE snapshot too.
        var fourth = PaperCapturedHybridSettlement.capture(List.of(world), budgets);
        fourth.includeDiscoveredLoadedWcl(this);
        boolean unprovenChestRejected = false;
        try {
            fourth.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage(
                    this, Map.of(world.getName(), world.getUID()), manifest, scope,
                    covered, terrain, unknownProtection, nonChestOwners, owners,
                    2 * 1024 * 1024, 16 * 1024 * 1024, noPositiveLive);
        } catch (IOException expected) {
            unprovenChestRejected = true;
        }
        if (!unprovenChestRejected)
            throw new IOException("UNKNOWN chest protection was falsely treated as FREE");
        boolean reuseDenied = false;
        try {
            fourth.stageCandidateWithCapturedBlockLockerAndSavedWclAndBanCoverage(
                    this, Map.of(world.getName(), world.getUID()), manifest, scope,
                    covered, terrain, knownNonProtection, nonChestOwners, owners,
                    2 * 1024 * 1024, 16 * 1024 * 1024, noPositiveLive);
        } catch (IllegalStateException expected) {
            reuseDenied = expected.getMessage() != null
                    && expected.getMessage().contains("already consumed");
        }
        if (!reuseDenied)
            throw new IOException("Failed BlockLocker proof allowed stale LIVE capture reuse");
        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK))
            throw new IOException("Full hybrid verification loaded SAVED-only fixture Chunk");
        terrain.witness().requireUnchanged();
        manifest.requireUnchangedDiscovery();
        getLogger().info("PAPER_A04_A05_UNKNOWN_CHEST_PROOF_FAIL_CLOSED_PASSED");
        if (Boolean.getBoolean("a01.probe.offMainHybrid")) {
            // This fixture deliberately scopes coverage to the real profile list;
            // production still requires the separate IP and external provider evidence.
            var fullProfile = protectedMode
                    ? co.surumene.simplecoins.domain.BanSourceCoverage.captureCompleteLists(
                            Set.of(fixtureProvider), Map.of(fixtureProvider,
                                    Bukkit.getBannedPlayers().stream()
                                            .filter(org.bukkit.OfflinePlayer::isBanned)
                                            .map(org.bukkit.OfflinePlayer::getUniqueId)
                                            .collect(java.util.stream.Collectors.toSet())))
                    : covered;
            var actualStartup = protectedMode
                    ? co.surumene.simplecoins.paper.PaperStartupSettlementSource.capturePrimary(
                            this, List.of(world), manifest, scope, fullProfile, budgets, 4096) : null;
            var workerCapture = PaperCapturedHybridSettlement.capture(List.of(world), budgets);
            workerCapture.includeDiscoveredLoadedWcl(this);
            var settings = protectedMode
                    ? co.surumene.simplecoins.paper.PaperBlockLockerSettings.capture() : null;
            Thread worker = new Thread(() -> {
                try {
                    if (Bukkit.isPrimaryThread()) throw new IOException("Worker fixture on primary");
                    var workerTerrain = settings != null
                            ? settings.captureSaved(manifest, scope, 2 * 1024 * 1024,
                                    16 * 1024 * 1024, 4096)
                            : co.surumene.simplecoins.domain.SavedBlockLockerTerrainObservations.capture(
                                    manifest, scope, 2 * 1024 * 1024, 16 * 1024 * 1024,
                                    4096, Set.of("[Private]"), Set.of("[More Users]"),
                                    Set.of("Everyone"), raw -> raw);
                    var staged = actualStartup != null
                            ? actualStartup.capture().staged()
                            : workerCapture.stageCombinedOffMain(this,
                            Map.of(sourceWorldName, sourceWorldId), manifest, scope, covered,
                            workerTerrain, knownNonProtection, nonChestOwners, owners,
                            2 * 1024 * 1024, 16 * 1024 * 1024, (source, value) -> {
                                if (!Bukkit.isPrimaryThread()) throw new IOException("LIVE owner read off primary");
                                return noPositiveLive.classify(source, value);
                            }, 120000);
                    verifyCompleteUnpublishedWclCandidate(staged, manifest, chickenId, wolfId,
                            nearChickenId, nearWolfId, protectedMode, staged.pending().commit());
                    Bukkit.getScheduler().runTask(this, () -> {
                        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK)) {
                            getLogger().severe("PAPER_A05_SERVICES_PROBE_FAILED: worker stage loaded SAVED-only chunk");
                        } else {
                            getLogger().info("PAPER_A01_G1_OFF_MAIN_HYBRID_STAGE_PASSED");
                        }
                        Bukkit.shutdown();
                    });
                } catch (Throwable failure) {
                    getLogger().log(java.util.logging.Level.SEVERE, "PAPER_A05_SERVICES_PROBE_FAILED", failure);
                    Bukkit.getScheduler().runTask(this, Bukkit::shutdown);
                }
            }, "simplecoins-hybrid-worker-fixture");
            worker.setDaemon(true);
            worker.start();
        }
    }

    private static void verifyCompleteUnpublishedWclCandidate(
            co.surumene.simplecoins.domain.SettlementScanRunner.Staged selected,
            WorldStorageDiscovery.Manifest manifest,
            UUID chickenId, UUID wolfId, UUID nearChickenId, UUID nearWolfId,
            boolean protectedMode, SettlementAccumulator.Result result) throws IOException {
        if (result == null)
            throw new IOException("Whole hybrid source had no committed monetary result");
        if (selected.savedContentWitness() == null
                || !selected.requiredSourceIds().containsAll(manifest.requiredSourceIds())
                || selected.report().regionFiles() != manifest.worldFiles().size()
                || selected.report().playerFiles() != manifest.playerDataFiles().size())
            throw new IOException("Whole hybrid did not retain all required saved sources");
        // The same staged accumulator was committed once: locally in the
        // legacy fixture or through the exact source-attested isolated product
        // publisher. Never commit the same generation a second time.
        BigInteger expectedDead = BigInteger.valueOf(protectedMode ? 30000 : 50000);
        BigInteger expectedFree = BigInteger.valueOf(protectedMode ? 1000 : 5000);
        BigInteger expectedPersonal = BigInteger.valueOf(protectedMode ? 25000 : 0);
        BigInteger expectedWorld = expectedPersonal.add(expectedFree);
        if (!expectedDead.equals(result.dead())
                || !expectedFree.equals(result.free())
                || !expectedWorld.equals(result.statistics().worldTotal())
                || !expectedPersonal.equals(result.statistics().personal())
                || result.statistics().registeredPlayers() != (protectedMode ? 1 : 0)
                || (protectedMode
                    ? !result.personalBalances().equals(
                            Map.of(PERSISTENCE_FOLLOW, BigInteger.valueOf(25000)))
                    : result.personalBalances().values().stream().anyMatch(v -> v.signum() != 0))
                || (!protectedMode && result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(chickenId.toString())
                        && e.value().equals(BigInteger.valueOf(10000))))
                || result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(wolfId.toString())
                        && e.value().equals(BigInteger.valueOf(10000)))
                || result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(wolfId.toString())
                        && e.value().equals(BigInteger.valueOf(5000)))
                || (!protectedMode && result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(nearChickenId.toString())
                        && e.value().equals(BigInteger.valueOf(10000))))
                || result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(nearWolfId.toString())
                        && e.value().equals(BigInteger.valueOf(10000)))
                || result.deadLocations().stream().noneMatch(e ->
                        e.location().contains(nearWolfId.toString())
                        && e.value().equals(BigInteger.valueOf(5000))))
            throw new IOException("Full hybrid WCL/BlockLocker money lost, doubled or misclassified");
        selected.savedContentWitness().requireUnchanged();
        manifest.requireUnchangedDiscovery();
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
