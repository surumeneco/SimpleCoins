package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.AnvilRegionReader;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldSnapshotTraversal;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.api.inventory.StorageSnapshot;
import co.surumene.wcl.plugin.inventory.WonderfulStorageReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
                String phase = System.getProperty("a05.probe.phase", "baseline");
                switch (phase) {
                    case "baseline" -> { }
                    case "persist" -> persistChickenEntity(world, selected);
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
            getLogger().info("PAPER_A05_WWC_LIVE_STORED_SHULKER_COIN_PASSED");
        } finally {
            chicken.remove();
        }

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

        wolfPdc.set(new NamespacedKey("whatawonderfulwolf", "schema_version"),
                PersistentDataType.INTEGER, 98);
        requireUnsupported(www.readStored(
                wolfEntity, world.getUID(), Optional.empty(), wolfPdc), "WWW");
        getLogger().info("PAPER_A05_BOTH_STORED_SCHEMA_FAILURES_FAIL_CLOSED_PASSED");
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
        assertChickenStorage(success(providers.get("wwc").readLoaded(chicken.getUniqueId()),
                "WWC far loaded pre-save"), chicken.getUniqueId(), world.getUID(),
                PERSISTENCE_FOLLOW, shulker, coins);
        Wolf wolf = world.spawn(
                new Location(world, anchor + 10.5, 120, anchor + 10.5), Wolf.class);
        wolf.addScoreboardTag(FAR_WWW_TAG);
        byte[] runtime = wolfRuntimeFixture(WOLF_OWNER, WOLF_COMMANDER,
                coins.create(CoinDenomination.TEN_THOUSAND, 1).serializeAsBytes(),
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
        int[] traversed = {0, 0};
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
                                    instanceof Integer version) || version != 3
                            || !PERSISTENCE_FOLLOW.toString().equals(
                                    stored.get("whatawonderfulchicken:follow_target"))
                            || !(stored.get("whatawonderfulchicken:equipment_shulker_box")
                                    instanceof byte[] shulker)
                            || !Arrays.equals(wwcBytes, shulker))
                        throw new IOException("Saved far WWC source differs from original fixture");
                    detached.set(new NamespacedKey("whatawonderfulchicken", "type"),
                            PersistentDataType.STRING, "wonderful_chicken");
                    detached.set(new NamespacedKey("whatawonderfulchicken", "data_version"),
                            PersistentDataType.INTEGER, 3);
                    detached.set(new NamespacedKey("whatawonderfulchicken", "follow_target"),
                            PersistentDataType.STRING, PERSISTENCE_FOLLOW.toString());
                    detached.set(new NamespacedKey("whatawonderfulchicken", "equipment_shulker_box"),
                            PersistentDataType.BYTE_ARRAY, shulker);
                    StorageSnapshot source = success(
                            providers.get("wwc").readStored(entityId, world.getUID(),
                                    Optional.empty(), detached), "WWC detached unloaded NBT");
                    assertChickenStorage(source, wwcUuid, world.getUID(), PERSISTENCE_FOLLOW,
                            wwcBytes, coins);
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
                + " uniqueEntityIds=2 knownRegionFiles=1 chunkNeverLoaded=true");
        if (recognized[0] != 1 || recognized[1] != 1)
            throw new IOException("Unloaded WWC/WWW saved source count incomplete WWC="
                    + recognized[0] + " WWW=" + recognized[1]);
        if (world.isChunkLoaded(FAR_CHUNK, FAR_CHUNK))
            throw new IllegalStateException("Saved Entity read accidentally loaded far chunk");
        getLogger().info("PAPER_A05_UNLOADED_WWC_WWW_SAVED_NBT_TO_REAL_WCL_PASSED"
                + " source=entities/r.1.1.mca entities=2"
                + " regionChunk=" + FAR_CHUNK + "," + FAR_CHUNK
                + " totalCoins=25000");
    }

    private static UUID readSavedUuid(Object raw) throws IOException {
        if (!(raw instanceof int[] values) || values.length != 4)
            throw new IOException("Entity UUID is not canonical four-int saved NBT");
        long hi = ((long) values[0] << 32) | Integer.toUnsignedLong(values[1]);
        long lo = ((long) values[2] << 32) | Integer.toUnsignedLong(values[3]);
        return new UUID(hi, lo);
    }
}
