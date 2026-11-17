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
import co.surumene.simplecoins.paper.SavedGrantDeliveryReadback;
import co.surumene.simplecoins.paper.SavedGrantReceiptNbt;
import com.janboerman.invsee.spigot.api.InvseeAPI;
import com.janboerman.invsee.spigot.api.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.MainSpectatorInventory;
import com.janboerman.invsee.spigot.api.event.SpectatorInventorySaveEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
    private final boolean vetoInvsee = Boolean.getBoolean("a02.invsee.cancel");
    private final boolean selectedMainhandProbe = Boolean.getBoolean("a02.invsee.probeSelectedMainhand");
    private final boolean durableFsyncAndHalt = Boolean.getBoolean("a02.invsee.fsyncThenHalt");
    private final boolean durableRecoveryCheck = Boolean.getBoolean("a02.invsee.checkAfterHalt");
    private final AtomicInteger vetoedSaveEvents = new AtomicInteger();

    @Override
    public void onEnable() {
        if (!"127.0.0.1".equals(getServer().getIp())
                || !getServer().getOnlinePlayers().isEmpty()) {
            throw new IllegalStateException("Disposable loopback/no-online-players CI only");
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                if (durableRecoveryCheck) verifyForcedSyncAfterAbruptRestart();
                else initializePhysicalProofThenInvokeActualPlugin();
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

    /**
     * Linux-only technical durability candidate: Paper saveData itself has no
     * durable-return contract. Force the ACTUAL compressed UUID.dat and its
     * containing directory after Paper returns, before our separate journal
     * ACK. This does NOT create a lock against filesystem or plugin writers.
     * It is deliberately absent from the product JAR.
     */
    private void forceActualPlayerdataAndContainingDirectory() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("File sync experiment must be isolated");
        Path file = dataFile();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)
                || Files.isSymbolicLink(file.getParent()))
            throw new IllegalStateException("Unsafe/nonregular playerdata under durability test");
        // O_NOFOLLOW through LinkOption prevents following a replaced leaf.
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        }
        // Required to request durability of Paper's temporary-file rename.
        try (FileChannel parent = FileChannel.open(
                file.getParent(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            parent.force(true);
        }
    }

    /** Cold restart of the SAME real Paper player file and product ledger. */
    private void verifyForcedSyncAfterAbruptRestart() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Files.isRegularFile(dataFile(),
                LinkOption.NOFOLLOW_LINKS) || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("No genuine offline UUID.dat after abrupt halt");
        GrantLedger replay = actualProductLedger();
        Map<String, Object> disk = saved();
        GrantReceiptState savedReceipt = SavedGrantReceiptNbt.readAndVerify(disk, replay, ID);
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(disk))
                || savedReceipt.active() || savedReceipt.completedSequence() != 1
                || replay.historySnapshot().size() != 1
                || !replay.historySnapshot().getFirst().delivered()
                || !replay.pending(ID).isEmpty())
            throw new IllegalStateException("Physical 5000 C, receipt or durable ACK lost");
        getLogger().info("PAPER_A02_FORCED_SYNC_ABRUPT_RESTART_PRESERVED_5000C_RECEIPT_ACK");
        getLogger().info("PAPER_A02_FORCED_SYNC_GUARD_REMAINS_CLOSED");
        Bukkit.shutdown();
    }

    private void initializePhysicalProofThenInvokeActualPlugin() throws Exception {
        if (Files.exists(dataFile()))
            throw new IllegalStateException("Probe requires a fresh player UUID.dat");
        ledger = actualProductLedger();
        Player player = detachedPlayer();
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());

        // Keep hotbar selected mainhand (slot 0) unchanged across the grant.
        // The #480 fixture revealed only absent versus empty equipment NBT;
        // do not infer a mirrored mainhand ItemStack from that observation.
        if (selectedMainhandProbe) {
            if (player.getInventory().getHeldItemSlot() != 0)
                throw new IllegalStateException("Expected default Paper selected hotbar index 0");
            player.getInventory().setItem(9, new ItemStack(Material.STONE, 2));
        } else {
            player.getInventory().setItem(0, new ItemStack(Material.STONE, 2));
        }
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
                || staged.placements().getFirst().index() != (selectedMainhandProbe ? 0 : 1))
            throw new IllegalStateException("Unexpected candidate slot plan");
        ItemStack[] physical = PaperGrantInventoryMaterializer.buildCopy(
                staged, GrantReceiptState.empty(),
                player.getInventory().getStorageContents(), codec);
        player.getInventory().setStorageContents(physical);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(),
                staged.nextReceipt());
        player.saveData();
        manifest = discover();

        if (selectedMainhandProbe) {
            // Independent negative test of a valid saved selected-hotbar
            // destination (slot zero). The saved equipment/mainhand view can
            // change even when no other physical equipment asset is mutated.
            Map<String, Object> after = saved();
            var oneRootProof = SavedGrantDeliveryReadback.verify(
                    after, ledger, ID, staged);
            if (!BigInteger.valueOf(5000).equals(totalSavedMoney(after))
                    || !BigInteger.valueOf(5000).equals(
                            oneRootProof.matchedMonetaryValue())
                    || oneRootProof.receipt().completedSequence() != grant.sequence()
                    || ledger.pending(ID).size() != 1)
                throw new IllegalStateException("Selected-slot real save is physically incomplete");
            String expectedProblem = "An unplanned Ender Chest or equipment asset changed";
            try {
                SavedGrantDeliveryFileReadback.verify(
                        manifest, ledger, ID, staged, priorRoot,
                        MAX_COMPRESSED, MAX_INFLATED);
                throw new IllegalStateException(
                        "Selected-slot transition unexpectedly accepted; review fixed behavior");
            } catch (java.io.IOException rejection) {
                if (!expectedProblem.equals(rejection.getMessage()))
                    throw new IllegalStateException(
                            "Saved selected-slot rejection has different cause", rejection);
            }
            getLogger().info("PAPER_A02_SELECTED_SLOT_SAVED_EQUIPMENT_VIEW_CHANGED"
                    + " beforeEquipment=" + priorRoot.get("equipment")
                    + " afterEquipment=" + after.get("equipment"));
            getLogger().info("PAPER_A02_SELECTED_SLOT_SINGLE_ROOT_5000_COIN_AND_RECEIPT_VALID");
            getLogger().info("PAPER_A02_SELECTED_SLOT_TRANSITION_REJECTS_VALID_SAVED_GRANT");
            getLogger().info("PAPER_A02_SELECTED_SLOT_PENDING_NO_ACK_PRESERVED");
            Bukkit.shutdown();
            return;
        }

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
        if (durableFsyncAndHalt) {
            forceActualPlayerdataAndContainingDirectory();
            // The file is re-verified AFTER its explicit filesystem sync.
            // A successful readback is still not a writer-epoch lease.
            var durableProof = SavedGrantDeliveryFileReadback.verify(
                    discover(), ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
            if (durableProof.receipt().completedSequence() != grant.sequence()
                    || !BigInteger.valueOf(5000).equals(durableProof.matchedMonetaryValue())
                    || ledger.pending(ID).size() != 1)
                throw new IllegalStateException("Durably synced grant proof invalid before ACK");
            getLogger().info("PAPER_A02_REAL_PLAYERDATA_FILE_AND_DIRECTORY_FORCE_RETURNED");
            ledger.acknowledge(grant, durableProof.receipt().completedSequence());
            if (!ledger.pending(ID).isEmpty()
                    || !BigInteger.valueOf(5000).equals(totalSavedMoney(saved())))
                throw new IllegalStateException("Durable ACK corrupted physical 5000 C proof");
            getLogger().info("PAPER_A02_REAL_LEDGER_ACK_AFTER_PLAYERDATA_SYNC_RETURNED");
            getLogger().info("PAPER_A02_DISPOSABLE_ABRUPT_JVM_HALT_AFTER_ACK");
            // No shutdown hooks or graceful Paper save. The workflow must
            // observe process exit 137 and cold-restart the SAME world.
            Runtime.getRuntime().halt(137);
            throw new AssertionError("Runtime.halt returned");
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("InvSeePlusPlus");
        if (!(plugin instanceof InvseePlusPlus installed)
                || !plugin.isEnabled())
            throw new IllegalStateException("Actual InvSee++ plugin/API unavailable");
        if (vetoInvsee) {
            // Test-only cooperative barrier: InvSee++ notifies before the
            // fake CraftPlayer loads/commits another offline UUID.dat.
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST)
                public void vetoThisOfflinePlayer(SpectatorInventorySaveEvent event) {
                    if (ID.equals(event.getInventory().getSpectatedPlayerId())) {
                        vetoedSaveEvents.incrementAndGet();
                        event.setCancelled(true);
                    }
                }
            }, this);
        }
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
                                        || codec.identify(storage[1]).orElse(null)
                                            != CoinDenomination.FIVE_THOUSAND
                                        || storage[0] == null
                                        || storage[0].getType() != Material.STONE)
                                    throw new IllegalStateException(
                                            "InvSee++ read failed to capture real coin and other item");
                                storage[1] = null;
                                inventory.setStorageContents(storage);
                                getLogger().info("PAPER_A02_INVSEE_REAL_OFFLINE_INVENTORY_CAPTURE_PASSED");
                                api.saveInventory(inventory).whenComplete((saved, saveError) ->
                                        Bukkit.getScheduler().runTaskLater(this, () -> {
                                            if (saveError != null) {
                                                fail(saveError);
                                                return;
                                            }
                                            try {
                                                if (vetoInvsee) {
                                                    if (saved == null || saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "InvSee++ did not acknowledge cancelled save");
                                                    verifyCancelledWriterLeavesProofIntact();
                                                } else {
                                                    if (saved == null || !saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "InvSee++ offline save did not complete");
                                                    verifyCompetingWriterAfterProof();
                                                }
                                            } catch (Throwable error) {
                                                fail(error);
                                            }
                                        }, 2L));
                            } catch (Throwable error) {
                                fail(error);
                            }
                        }));
    }

    /**
     * Positive counterpart to the deliberately stale-proof negative test.
     * The bounded InvSee++ event veto must return notSaved BEFORE it can
     * write any playerdata. No admission/grant coordinator is installed.
     */
    private void verifyCancelledWriterLeavesProofIntact() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || vetoedSaveEvents.get() != 1)
            throw new IllegalStateException("InvSee++ cooperative veto missing or not isolated");
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(saved()))
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Cancelled InvSee++ event still changed disk or journal");
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(
                saved(), ledger, ID);
        if (receipt.completedSequence() != verifiedSequence || receipt.active())
            throw new IllegalStateException("Veto lost original persisted receipt");
        var again = SavedGrantDeliveryFileReadback.verify(
                discover(), ledger, ID, staged, priorRoot,
                MAX_COMPRESSED, MAX_INFLATED);
        if (!BigInteger.valueOf(5000).equals(again.matchedMonetaryValue())
                || again.receipt().completedSequence() != verifiedSequence)
            throw new IllegalStateException("Veto returned invalid saved proof");
        getLogger().info("PAPER_A02_INVSEE_SAVE_EVENT_CANCELLED_BEFORE_FILE_WRITE");
        getLogger().info("PAPER_A02_INVSEE_CANCELLED_SAVE_PRESERVES_VALID_COIN_RECEIPT_PROOF");
        getLogger().info("PAPER_A02_INVSEE_CANCELLED_SAVE_LEAVES_LEDGER_PENDING");
        Bukkit.shutdown();
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
                    && stack.get("Slot") instanceof Number slot && slot.intValue() == 0))
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
