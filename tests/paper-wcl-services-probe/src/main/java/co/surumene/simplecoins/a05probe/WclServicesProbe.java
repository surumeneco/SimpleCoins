package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.api.inventory.StorageSnapshot;
import co.surumene.wcl.plugin.inventory.WonderfulStorageReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Chicken;
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
        Chicken chicken = world.spawn(new Location(world, 32.5, 120, 32.5), Chicken.class);
        try {
            seedChickenStorage(chicken.getPersistentDataContainer(), chickenCommander, storedShulker);
            WonderfulStorageReader wwc = providers.get("wwc");
            StorageSnapshot live = success(wwc.readLoaded(chicken.getUniqueId()), "WWC loaded");
            StorageSnapshot saved = success(wwc.readStored(chicken.getUniqueId(), world.getUID(),
                    Optional.of(new StorageSnapshot.Position(
                            chicken.getLocation().getX(), chicken.getLocation().getY(), chicken.getLocation().getZ())),
                    chicken.getPersistentDataContainer()), "WWC stored");
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
}
