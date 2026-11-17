package co.surumene.simplecoins.a02invsee;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.SavedPlayerAssets;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperGrantInventoryMaterializer;
import co.surumene.simplecoins.paper.PaperGrantInventorySlotPlan;
import co.surumene.simplecoins.paper.PaperGrantReceiptPdc;
import co.surumene.simplecoins.paper.SavedGrantDeliveryFileReadback;
import co.surumene.simplecoins.paper.SavedGrantReceiptNbt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ONLY an isolated real Paper 26.2 + distributed InvSee++ proof-lifetime test.
 * No online players, main release, real game data or production issuance.
 */
public final class InvseeWriterProbe extends JavaPlugin {
    private static final UUID ID =
            UUID.fromString("00000000-0000-0000-0000-00000000a0f1");
    private static final int MAX_COMPRESSED = 16 * 1024 * 1024;
    private static final int MAX_INFLATED = 64 * 1024 * 1024;

    private GrantLedger ledger;
    private GrantLedger.Grant grant;
    private PaperGrantInventorySlotPlan staged;
    private Map<String, Object> priorRoot;
    private WorldStorageDiscovery.Manifest manifest;
    private long verifiedSequence;

    @Override
    public void onEnable() {
        if (!"127.0.0.1".equals(getServer().getIp())
                || !getServer().getOnlinePlayers().isEmpty()) {
            throw new IllegalStateException("Disposable loopback/no-online-players CI only");
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                initializePhysicalProofThenInvokeActualPlugin();
            } catch (Throwable failure) {
                fail(failure);
            }
        }, 55L);
    }

    private void fail(Throwable error) {
        getLogger().log(Level.SEVERE, "PAPER_A02_INVSEE_PROBE_FAILED", error);
        Bukkit.shutdown();
    }

    private Path dataFile() {
        return getServer().getWorldContainer().toPath().toAbsolutePath().normalize()
                .resolve("world/players/data/" + ID + ".dat");
    }

    private GrantLedger actualProductLedger() throws Exception {
        Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        if (product == null || !product.isEnabled())
            throw new IllegalStateException("SimpleCoins product not enabled");
        Field f = product.getClass().getDeclaredField("issuanceGrants");
        f.setAccessible(true);
        Object value = f.get(product);
        if (!(value instanceof GrantLedger found))
            throw new IllegalStateException("No active product grant journal");
        return found;
    }

    private Player detachedPlayer() throws ReflectiveOperationException {
        Object mc = getServer().getClass().getMethod("getServer").invoke(getServer());
        Class<?> mcType = Class.forName("net.minecraft.server.MinecraftServer");
        Object world = mcType.getMethod("overworld").invoke(mc);
        Class<?> levelType = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profile = Class.forName("com.mojang.authlib.GameProfile");
        Object gameProfile = profile.getConstructor(UUID.class, String.class)
                .newInstance(ID, "ProbeInvSeeWriter");
        Class<?> client = Class.forName("net.minecraft.server.level.ClientInformation");
        Object clientInfo = client.getMethod("createDefault").invoke(null);
        Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
        Constructor<?> constructor = serverPlayer.getConstructor(
                mcType, levelType, profile, client);
        Object entity = constructor.newInstance(mc, world, gameProfile, clientInfo);
        Object wrapper = serverPlayer.getMethod("getBukkitEntity").invoke(entity);
        if (!(wrapper instanceof Player player) || player.isOnline())
            throw new IllegalStateException("Detached NMS player is not isolated");
        return player;
    }

    private Map<String, Object> saved() throws Exception {
        return PlayerDataReader.read(dataFile(), MAX_COMPRESSED, MAX_INFLATED);
    }

    private WorldStorageDiscovery.Manifest discover() throws Exception {
        World world = Bukkit.getWorld("world");
        if (world == null) throw new IllegalStateException("Paper world missing");
        Path folder = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path levelRoot = dataFile().getParent().getParent().getParent();
        var found = WorldStorageDiscovery.discover(
                Map.of(world.getName(), folder),
                new WorldScope(true, Set.of(), Set.of(world.getName())),
                100000, Set.of(levelRoot));
        if (!dataFile().equals(found.playerDataFiles().get(ID))
                || found.playerDataFiles().size() != 1)
            throw new IllegalStateException("Real UUID.dat not discovered exactly once");
        return found;
    }

    private static BigInteger totalSavedMoney(Map<String, Object> root)
            throws Exception {
        return SavedPlayerAssets.extract(root).values().stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private void initializePhysicalProofThenInvokeActualPlugin() throws Exception {
        if (Files.exists(dataFile()))
            throw new IllegalStateException("Probe requires a fresh player UUID.dat");
        ledger = actualProductLedger();
        Player player = detachedPlayer();
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());

        // Save the baseline independently, with one unrelated physical item.
        player.getInventory().setItem(9, new ItemStack(Material.STONE, 2));
        player.saveData();
        priorRoot = saved();
        if (!BigInteger.ZERO.equals(totalSavedMoney(priorRoot)))
            throw new IllegalStateException("Baseline already has monetary item");

        grant = ledger.reserveCoins(ID, CoinDenomination.FIVE_THOUSAND, 1,
                "a02-invsee-disposable-ci");
        staged = PaperGrantInventorySlotPlan.stage(ledger, ID, grant,
                GrantReceiptState.empty(), player.getInventory().getStorageContents())
                .orElseThrow(() -> new IllegalStateException("No free real storage slot"));
        if (staged.placements().size() != 1
                || staged.placements().getFirst().index() != 0)
            throw new IllegalStateException("Unexpected candidate slot plan");
        ItemStack[] physical = PaperGrantInventoryMaterializer.buildCopy(
                staged, GrantReceiptState.empty(),
                player.getInventory().getStorageContents(), codec);
        player.getInventory().setStorageContents(physical);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(),
                staged.nextReceipt());
        player.saveData();
        manifest = discover();

        var proof = SavedGrantDeliveryFileReadback.verify(
                manifest, ledger, ID, staged, priorRoot, MAX_COMPRESSED,
                MAX_INFLATED);
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(saved()))
                || !BigInteger.valueOf(5000).equals(proof.matchedMonetaryValue())
                || proof.receipt().completedSequence() != grant.sequence()
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Real persisted proof before InvSee writer is invalid");
        verifiedSequence = proof.receipt().completedSequence();
        getLogger().info("PAPER_A02_INVSEE_REAL_SAVED_COIN_RECEIPT_PROOF_PASSED");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("InvSeePlusPlus");
        if (!(plugin instanceof InvseePlusPlus installed)
                || !plugin.isEnabled())
            throw new IllegalStateException("Actual InvSee++ plugin/API unavailable");
        InvseeAPI api = installed.getApi();
        if (api == null)
            throw new IllegalStateException("Actual InvSee++ has no API");
        api.mainSpectatorInventory(ID, "ProbeInvSeeWriter")
                .whenComplete((response, failure) ->
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (failure != null) {
                                fail(failure);
                                return;
                            }
                            try {
                                if (response == null || !response.isSuccess())
                                    throw new IllegalStateException(
                                            "InvSee++ refused synthetic known offline player: "
                                            + (response == null ? "null" : response.getReason()));
                                MainSpectatorInventory inventory = response.getInventory();
                                ItemStack[] storage = inventory.getStorageContents();
                                if (storage.length != PaperGrantInventorySlotPlan.STORAGE_SLOTS
                                        || codec.identify(storage[0]).orElse(null)
                                            != CoinDenomination.FIVE_THOUSAND
                                        || storage[9] == null
                                        || storage[9].getType() != Material.STONE)
                                    throw new IllegalStateException(
                                            "InvSee++ read failed to capture real coin and other item");
                                storage[0] = null;
                                inventory.setStorageContents(storage);
                                getLogger().info("PAPER_A02_INVSEE_REAL_OFFLINE_INVENTORY_CAPTURE_PASSED");
                                api.saveInventory(inventory).whenComplete((saved, saveError) ->
                                        Bukkit.getScheduler().runTaskLater(this, () -> {
                                            if (saveError != null) {
                                                fail(saveError);
                                                return;
                                            }
                                            try {
                                                if (saved == null || !saved.isSaved())
                                                    throw new IllegalStateException(
                                                            "InvSee++ offline save did not complete");
                                                verifyCompetingWriterAfterProof();
                                            } catch (Throwable error) {
                                                fail(error);
                                            }
                                        }, 2L));
                            } catch (Throwable error) {
                                fail(error);
                            }
                        }));
    }

    private void verifyCompetingWriterAfterProof() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Offline save probe left isolated primary thread");
        Map<String, Object> current = saved();
        if (!BigInteger.ZERO.equals(totalSavedMoney(current)))
            throw new IllegalStateException("InvSee++ did not remove saved physical coin");
        Object rawInventory = current.get("Inventory");
        if (!(rawInventory instanceof java.util.List<?> items)
                || items.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))
                    && stack.get("Slot") instanceof Number slot && slot.intValue() == 9))
            throw new IllegalStateException("InvSee++ damaged unrelated storage");
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(current,
                ledger, ID);
        if (receipt.completedSequence() != verifiedSequence || receipt.active())
            throw new IllegalStateException("Saved receipt was not retained by InvSee++");
        getLogger().info("PAPER_A02_INVSEE_ACTUAL_OFFLINE_SAVED_COIN_REMOVAL_RECEIPT_RETAINED");

        boolean staleRejected = false;
        try {
            SavedGrantDeliveryFileReadback.verify(
                    manifest, ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
        } catch (java.io.IOException expected) {
            staleRejected = true;
        }
        if (!staleRejected)
            throw new IllegalStateException("Fresh readback approved lost physical coin");
        getLogger().info("PAPER_A02_INVSEE_FRESH_READBACK_REFUSED_STALE_PROOF_PASSED");
        if (ledger.pending(ID).size() != 1)
            throw new IllegalStateException("External writer accidentally acknowledged pending grant");
        // NEGATIVE EXISTING LOW-LEVEL API counterexample: caller has a
        // previously valid proof sequence but no reader-to-ACK lease.
        ledger.acknowledge(grant, verifiedSequence);
        var replay = new GrantLedger(Bukkit.getPluginManager()
                .getPlugin("SimpleCoins").getDataFolder().toPath().resolve("grants"));
        if (replay.historySnapshot().size() != 1
                || !replay.historySnapshot().getFirst().delivered()
                || !replay.pending(ID).isEmpty()
                || !BigInteger.ZERO.equals(totalSavedMoney(saved())))
            throw new IllegalStateException("False low-level ACK was not durably reproduced");
        getLogger().info("PAPER_A02_INVSEE_ACTUAL_PLUGIN_POST_PROOF_FALSE_ACK_REPRODUCED");
        getLogger().info("PAPER_A02_INVSEE_NO_CLIENT_NO_PRODUCTION_ISSUANCE");
        Bukkit.shutdown();
    }
}
