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
import co.surumene.simplecoins.paper.PaperSavedSourceDiscovery;
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
import java.nio.channels.FileLock;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
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
    // Test-only composition of cooperative writer veto with physical fsync + durable ACK.
    private final boolean vetoAndDurableAck =
            Boolean.getBoolean("a02.invsee.vetoFsyncAck");
    private final boolean testOldInodeFileLock = Boolean.getBoolean("a02.invsee.lockHeldDuringSave");
    private FileChannel lockedPlayerdataChannel;
    private FileLock heldPlayerdataLock;
    private Object lockedPlayerdataFileKey;
    private final boolean selectedMainhandProbe = Boolean.getBoolean("a02.invsee.probeSelectedMainhand");
    private final boolean durableFsyncAndHalt = Boolean.getBoolean("a02.invsee.fsyncThenHalt");
    private final boolean syncBeforeAckHalt = Boolean.getBoolean("a02.invsee.fsyncThenHaltBeforeAck");
    private final boolean durableRecoveryCheck = Boolean.getBoolean("a02.invsee.checkAfterHalt");
    private final boolean recoverPendingAfterHalt = Boolean.getBoolean("a02.invsee.recoverPendingAfterHalt");
    private final AtomicInteger vetoedSaveEvents = new AtomicInteger();
    private int startupDiscoveryWaitTicks;
    private final boolean testPastEventCheckpoint =
            Boolean.getBoolean("a02.invsee.probePastEventCheckpoint");
    // Strictly one KNOWN InvSee Future tracked after first event admission,
    // not a real Paper/all-plugin writer coordinator.
    private final boolean testAdmittedWriterDrainThenAck =
            Boolean.getBoolean("a02.invsee.drainAdmittedThenAck");
    // Negative counterpart to the ONE known InvSee Future drain: a native
    // Paper save is deliberately inserted AFTER a valid proof, BEFORE ACK.
    private final boolean testNativePostDrainFalseAck =
            Boolean.getBoolean("a02.invsee.nativePostDrainFalseAck");
    private final boolean replayNativePostDrainFalseAck =
            Boolean.getBoolean("a02.invsee.checkNativePostDrainFalseAck");
    // A separate genuinely executed Paper.saveData path that never calls
    // InvSee's SpectatorInventorySaveEvent. No production writer fence.
    private final boolean testPaperDirectAfterInvseeVeto =
            Boolean.getBoolean("a02.invsee.paperSaveBypassesVeto");
    private final boolean recoverPaperDirectAfterHalt =
            Boolean.getBoolean("a02.invsee.checkPaperDirectAfterHalt");
    private final boolean testNativeAgentEntry =
            Boolean.getBoolean("a02.invsee.nativeAgentEntryTest");
    private final boolean recoverNativeAgentAfterHalt =
            Boolean.getBoolean("a02.invsee.checkNativeAgentAfterHalt");
    private Player originalDetachedPaperWriter;
    private final AtomicBoolean lateFenceClosed = new AtomicBoolean();
    private final AtomicInteger alreadyAdmittedSaveEvents = new AtomicInteger();

    @Override
    public void onEnable() {
        if (!"127.0.0.1".equals(getServer().getIp())
                || !getServer().getOnlinePlayers().isEmpty()) {
            throw new IllegalStateException("Disposable loopback/no-online-players CI only");
        }
        // Do NOT create a fresh UUID.dat concurrently with the product's
        // initial one-shot asynchronous saved-file discovery/receipt audit.
        // A prior fixed 55-tick wait was nondeterministic under CI load and
        // caused the product to (correctly) fail closed on directory drift.
        Bukkit.getScheduler().runTaskLater(
                this, this::startAfterObservedProductDiscovery, 55L);
    }

    private void startAfterObservedProductDiscovery() {
        try {
            if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty())
                throw new IllegalStateException("Discovery preflight must run on isolated main thread");
            Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
            if (product == null || !product.isEnabled())
                throw new IllegalStateException("Product terminated during startup discovery");
            Field field = product.getClass().getDeclaredField("sourceDiscovery");
            field.setAccessible(true);
            if (!(field.get(product) instanceof PaperSavedSourceDiscovery discovery))
                throw new IllegalStateException("Product source discovery is not initialized");
            if (discovery.failure().isPresent())
                throw new IllegalStateException(
                        "Product startup saved source audit failed", discovery.failure().get());
            if (discovery.observed().isEmpty()) {
                if (++startupDiscoveryWaitTicks > 20 * 170)
                    throw new IllegalStateException(
                            "Product startup audit has not observed a source manifest");
                Bukkit.getScheduler().runTaskLater(
                        this, this::startAfterObservedProductDiscovery, 1L);
                return;
            }
            getLogger().info("PAPER_A02_SOURCE_DISCOVERY_OBSERVED_BEFORE_WRITER");
            if (durableRecoveryCheck) verifyForcedSyncAfterAbruptRestart();
            else if (recoverPendingAfterHalt) verifyPendingAfterForcedSyncCrash();
            else if (recoverPaperDirectAfterHalt) verifyPaperDirectAfterHalt();
            else if (recoverNativeAgentAfterHalt) verifyNativeAgentAfterHalt();
            else if (replayNativePostDrainFalseAck) verifyNativePostDrainFalseAckOnColdRestart();
            else initializePhysicalProofThenInvokeActualPlugin();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void fail(Throwable error) {
        getLogger().log(Level.SEVERE, "PAPER_A02_INVSEE_PROBE_FAILED", error);
        try { releasePlayerdataLock(); }
        catch (Exception cleanup) { getLogger().log(Level.SEVERE, "Probe lock cleanup failed", cleanup); }
        Bukkit.shutdown();
    }

    @Override
    public void onDisable() {
        try { releasePlayerdataLock(); }
        catch (Exception failure) { getLogger().log(Level.SEVERE, "Probe lock disable cleanup failed", failure); }
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

    /**
     * A separate cold JVM observes a forced playerdata save that survived
     * WITHOUT any grant ACK. It may complete the existing journal record,
     * but MUST NOT re-materialize the coin or save the player again.
     */
    private void verifyPendingAfterForcedSyncCrash() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Files.isRegularFile(dataFile(),
                LinkOption.NOFOLLOW_LINKS) || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Recovery requires genuine isolated playerdata");
        GrantLedger journal = actualProductLedger();
        var pending = journal.pending(ID);
        if (pending.size() != 1 || journal.historySnapshot().size() != 1)
            throw new IllegalStateException("Pending single original reservation lost after crash");
        GrantLedger.Grant original = pending.getFirst();
        if (original.sequence() != 1 || original.amount() != 5000
                || original.delivered()
                || original.denomination() != CoinDenomination.FIVE_THOUSAND
                || original.coinCount() != 1)
            throw new IllegalStateException("Recovered original durable grant mutated");

        Map<String, Object> disk = saved();
        GrantReceiptState savedReceipt = SavedGrantReceiptNbt.readAndVerify(disk, journal, ID);
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(disk))
                || savedReceipt.active() || savedReceipt.completedSequence() != 1)
            throw new IllegalStateException("Recovered completed physical coin+receipt disagree");
        Object inventory = disk.get("Inventory");
        if (!(inventory instanceof java.util.List<?> items)
                || items.stream().noneMatch(raw -> raw instanceof Map<?, ?> item
                    && item.get("Slot") instanceof Number number
                    && number.intValue() == 0
                    && "minecraft:stone".equals(item.get("id"))))
            throw new IllegalStateException("Unrelated player storage item lost");
        getLogger().info("PAPER_A02_RECOVERED_SAVED_5000C_AND_RECEIPT_WITH_UNACKED_LEDGER");

        // Reconstruct the EMPTY original recipient slot geometry for this
        // isolated experiment, not a general proof of arbitrary historical
        // partially completed grants or a production-replay design.
        var historicalSlots = new java.util.ArrayList<String>(
                java.util.Collections.nCopies(PaperGrantInventorySlotPlan.STORAGE_SLOTS, null));
        historicalSlots.set(0, "original-stone-occupied");
        PaperGrantInventorySlotPlan previous = PaperGrantInventorySlotPlan.stage(
                journal, ID, original, GrantReceiptState.empty(),
                new PaperGrantInventorySlotPlan.StorageSnapshot(historicalSlots))
                .orElseThrow(() -> new IllegalStateException("Cannot reconstruct isolated baseline"));
        if (previous.placements().size() != 1
                || previous.placements().getFirst().index() != 1)
            throw new IllegalStateException("Wrong physical grant destination on replay");
        var readback = SavedGrantDeliveryReadback.verify(disk, journal, ID, previous);
        if (!BigInteger.valueOf(5000).equals(readback.matchedMonetaryValue())
                || readback.receipt().completedSequence() != original.sequence())
            throw new IllegalStateException("Persisted grant evidence not authentic");
        getLogger().info("PAPER_A02_RECOVERY_READBACK_EXISTING_PHYSICAL_COIN_NOT_REISSUED");

        var savedBytesBefore = Files.readAllBytes(dataFile());
        journal.acknowledge(original, readback.receipt().completedSequence());
        var after = new GrantLedger(Bukkit.getPluginManager()
                .getPlugin("SimpleCoins").getDataFolder().toPath().resolve("grants"));
        if (after.historySnapshot().size() != 1
                || !after.historySnapshot().getFirst().delivered()
                || !after.pending(ID).isEmpty()
                || !java.util.Arrays.equals(savedBytesBefore, Files.readAllBytes(dataFile())))
            throw new IllegalStateException("ACK-only durable replay changed actual physical playerdata");
        // Idempotent second ACK must not rewrite or duplicate physical money.
        journal.acknowledge(original, readback.receipt().completedSequence());
        if (!java.util.Arrays.equals(savedBytesBefore, Files.readAllBytes(dataFile()))
                || !journal.pending(ID).isEmpty())
            throw new IllegalStateException("Idempotent ACK reissued physical coin");
        getLogger().info("PAPER_A02_RECOVERED_ONLY_DURABLE_ACK_WITHOUT_PLAYERDATA_WRITE");
        getLogger().info("PAPER_A02_FSYNC_PREACK_CRASH_GUARD_REMAINS_CLOSED");
        Bukkit.shutdown();
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
        if (testPaperDirectAfterInvseeVeto || testNativePostDrainFalseAck)
            originalDetachedPaperWriter = player;
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
        if (testOldInodeFileLock) acquirePlayerdataLockOnVerifiedFile();
        if (durableFsyncAndHalt || syncBeforeAckHalt) {
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
            if (syncBeforeAckHalt) {
                if (ledger.pending(ID).size() != 1
                        || ledger.historySnapshot().getFirst().delivered())
                    throw new IllegalStateException("Before-ACK crash branch already issued ACK");
                getLogger().info("PAPER_A02_FSYNC_COMPLETE_BEFORE_ACK_PENDING_ONE");
                getLogger().info("PAPER_A02_ABRUPT_HALT_BEFORE_DURABLE_ACK");
                Runtime.getRuntime().halt(138);
                throw new AssertionError("Before-ACK halt returned");
            }
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
        if (testPastEventCheckpoint || testAdmittedWriterDrainThenAck
                || testNativePostDrainFalseAck) {
            // A deliberately TOO-LATE fence: the FIRST actual save event is
            // admitted at LOWEST. MONITOR closes new admission only after
            // that first writer crossed its event veto checkpoint. The real
            // InvSee implementation still has its saveData work outstanding.
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST)
                public void gateInvSeeSave(SpectatorInventorySaveEvent event) {
                    if (!ID.equals(event.getInventory().getSpectatedPlayerId())) return;
                    if (lateFenceClosed.get()) {
                        vetoedSaveEvents.incrementAndGet();
                        event.setCancelled(true);
                    } else {
                        alreadyAdmittedSaveEvents.incrementAndGet();
                    }
                }
                @EventHandler(priority = EventPriority.MONITOR)
                public void afterEventAdmission(SpectatorInventorySaveEvent event) {
                    if (ID.equals(event.getInventory().getSpectatedPlayerId())
                            && alreadyAdmittedSaveEvents.get() == 1
                            && !event.isCancelled()
                            && lateFenceClosed.compareAndSet(false, true)) {
                        getLogger().info(
                                "PAPER_A02_INVSEE_LATE_VETO_CLOSED_AFTER_FIRST_EVENT_ADMITTED");
                    }
                }
            }, this);
        }
        if (vetoInvsee || vetoAndDurableAck || testPaperDirectAfterInvseeVeto) {
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
                                if (!testAdmittedWriterDrainThenAck
                                        && !testNativePostDrainFalseAck) {
                                    // The existing NEGATIVE control destroys money
                                    // after the original positive grant readback.
                                    storage[1] = null;
                                    inventory.setStorageContents(storage);
                                }
                                // The new POSITIVE control saves the exact
                                // unmodified 5000C + unrelated STONE inventory.
                                getLogger().info("PAPER_A02_INVSEE_REAL_OFFLINE_INVENTORY_CAPTURE_PASSED");
                                api.saveInventory(inventory).whenComplete((saved, saveError) ->
                                        Bukkit.getScheduler().runTaskLater(this, () -> {
                                            if (saveError != null) {
                                                fail(saveError);
                                                return;
                                            }
                                            try {
                                                if (testAdmittedWriterDrainThenAck
                                                        || testNativePostDrainFalseAck) {
                                                    if (saved == null || !saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "Admitted preserving InvSee writer never completed");
                                                    verifyDrainedAdmittedWriterBeforeAck(api, inventory);
                                                } else if (testPastEventCheckpoint) {
                                                    if (saved == null || !saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "First already-admitted InvSee save was not completed");
                                                    verifyAlreadyAdmittedWriterThenDenyNext(api, inventory);
                                                } else if (vetoInvsee || vetoAndDurableAck
                                                        || testPaperDirectAfterInvseeVeto) {
                                                    if (saved == null || saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "InvSee++ did not acknowledge cancelled save");
                                                    if (testPaperDirectAfterInvseeVeto)
                                                        verifyPaperDirectWriterBypassesInvseeEvent();
                                                    else
                                                        verifyCancelledWriterLeavesProofIntact();
                                                } else {
                                                    if (saved == null || !saved.isSaved())
                                                        throw new IllegalStateException(
                                                                "InvSee++ offline save did not complete");
                                                    if (testOldInodeFileLock) verifyAdvisoryLockIsNotWriterFence();
                                                    else verifyCompetingWriterAfterProof();
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
     * InvSee's real asynchronous save was admitted before the fence closed
     * inside the event dispatch. It must still be accounted as in-flight.
     * This method does NOT ACK a stale proof or introduce a product fence.
     */
    private void verifyAlreadyAdmittedWriterThenDenyNext(
            InvseeAPI api, MainSpectatorInventory inventory) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !testPastEventCheckpoint || !lateFenceClosed.get()
                || alreadyAdmittedSaveEvents.get() != 1
                || vetoedSaveEvents.get() != 0)
            throw new IllegalStateException("Late-fence fixture did not admit exactly one event");
        if (!BigInteger.ZERO.equals(totalSavedMoney(saved())))
            throw new IllegalStateException(
                    "Writer admitted before fence did not remove physical saved 5000C");
        var receipt = SavedGrantReceiptNbt.readAndVerify(saved(), ledger, ID);
        if (receipt.active() || receipt.completedSequence() != grant.sequence()
                || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException(
                    "First post-fence write changed receipt or durable grant state");
        var rootInventory = saved().get("Inventory");
        if (!(rootInventory instanceof java.util.List<?> all)
                || all.stream().noneMatch(raw -> raw instanceof Map<?, ?> item
                    && "minecraft:stone".equals(item.get("id"))
                    && item.get("Slot") instanceof Number slot && slot.intValue() == 0))
            throw new IllegalStateException("First post-fence write lost unrelated STONE");
        getLogger().info("PAPER_A02_INVSEE_FIRST_ADMITTED_SAVE_COMPLETED_AFTER_LATE_FENCE");
        getLogger().info("PAPER_A02_INVSEE_SAVED_COIN_REMOVED_RECEIPT_RETAINED_LEDGER_PENDING");

        api.saveInventory(inventory).whenComplete((second, failure) ->
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        if (failure != null)
                            throw new IllegalStateException(
                                    "Second InvSee save failed unexpectedly", failure);
                        if (second == null || second.isSaved()
                                || !lateFenceClosed.get()
                                || alreadyAdmittedSaveEvents.get() != 1
                                || vetoedSaveEvents.get() != 1)
                            throw new IllegalStateException(
                                    "Later save was not cancelled by late event fence");
                        if (!BigInteger.ZERO.equals(totalSavedMoney(saved()))
                                || ledger.pending(ID).size() != 1)
                            throw new IllegalStateException(
                                    "Later veto altered the first writer's saved physical state");
                        boolean rejected = false;
                        try {
                            SavedGrantDeliveryFileReadback.verify(
                                    discover(), ledger, ID, staged, priorRoot,
                                    MAX_COMPRESSED, MAX_INFLATED);
                        } catch (java.io.IOException expected) {
                            rejected = true;
                        }
                        if (!rejected)
                            throw new IllegalStateException(
                                    "Fresh grant readback approved missing saved money");
                        getLogger().info(
                                "PAPER_A02_INVSEE_SECOND_SAVE_EVENT_VETOED_AFTER_FENCE");
                        getLogger().info(
                                "PAPER_A02_INVSEE_FRESH_PROOF_REJECTS_LOST_MONEY_NO_ACK");
                        getLogger().info(
                                "PAPER_A02_INVSEE_POST_EVENT_VETO_NEEDS_IN_FLIGHT_DRAIN");
                        Bukkit.shutdown();
                    } catch (Throwable error) {
                        fail(error);
                    }
                }, 2L));
    }

    /**
     * POSITIVE real-Paper control for the complete observed life of ONE known
     * InvSee++ save future. The event veto closes AFTER its first admission.
     * It is unsafe to ACK at that instant; only after SaveResponse.saved
     * has completed can this fixture request fresh source proof.
     *
     * This does not authorize claims about other Paper / filesystem writers.
     */
    private void verifyDrainedAdmittedWriterBeforeAck(
            InvseeAPI api, MainSpectatorInventory inventory) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !(testAdmittedWriterDrainThenAck || testNativePostDrainFalseAck)
                || !lateFenceClosed.get()
                || alreadyAdmittedSaveEvents.get() != 1
                || vetoedSaveEvents.get() != 0
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException(
                    "Cannot begin post-future safe phase before one known writer completes");
        // The first real InvSee Future has resolved; verify physical effect,
        // not only the API's success flag. In contrast to the destructive
        // control, this writer must have preserved existing issued money.
        Map<String, Object> afterWriter = saved();
        var observedReceipt = SavedGrantReceiptNbt.readAndVerify(
                afterWriter, ledger, ID);
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(afterWriter))
                || observedReceipt.active()
                || observedReceipt.completedSequence() != grant.sequence()
                || ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException(
                    "Admitted writer completed but changed physical coin/receipt");
        getLogger().info("PAPER_A02_INVSEE_ADMITTED_PRESERVING_WRITER_DRAINED_BEFORE_ACK");

        // Prove that a later save event is indeed refused while the guard
        // remains closed. The first writer's resolved Future is known, but
        // the event by itself never furnished a global in-flight count.
        api.saveInventory(inventory).whenComplete((second, failure) ->
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    try {
                        if (failure != null)
                            throw new IllegalStateException(
                                    "Second real InvSee save failed unexpectedly", failure);
                        if (second == null || second.isSaved()
                                || !lateFenceClosed.get()
                                || alreadyAdmittedSaveEvents.get() != 1
                                || vetoedSaveEvents.get() != 1)
                            throw new IllegalStateException(
                                    "Closed event fence accepted a second SaveData writer");
                        if (ledger.pending(ID).size() != 1
                                || !BigInteger.valueOf(5000).equals(totalSavedMoney(saved())))
                            throw new IllegalStateException(
                                    "Source changed during the rejected second save");
                        getLogger().info("PAPER_A02_INVSEE_SECOND_SAVE_VETOED_BEFORE_ACK");

                        // Paper saved the actual UUID.dat in the first real
                        // InvSee Future; request Linux file+parent force and
                        // get a NEW authenticated same-root transition readback.
                        forceActualPlayerdataAndContainingDirectory();
                        byte[] savedBeforeAck = Files.readAllBytes(dataFile());
                        var proof = SavedGrantDeliveryFileReadback.verify(
                                discover(), ledger, ID, staged, priorRoot,
                                MAX_COMPRESSED, MAX_INFLATED);
                        if (proof.receipt().active()
                                || proof.receipt().completedSequence() != grant.sequence()
                                || !BigInteger.valueOf(5000).equals(
                                        proof.matchedMonetaryValue())
                                || !lateFenceClosed.get()
                                || !Arrays.equals(savedBeforeAck, Files.readAllBytes(dataFile())))
                            throw new IllegalStateException(
                                    "Drained known-writer source readback is invalid");
                        getLogger().info("PAPER_A02_INVSEE_DRAIN_FILE_DIR_FSYNC_FRESH_PROOF");
                        if (testNativePostDrainFalseAck) {
                            verifyNativeSaveAfterKnownDrainBeforeAck(proof);
                            return;
                        }

                        ledger.acknowledge(grant, proof.receipt().completedSequence());
                        var reopened = new GrantLedger(Bukkit.getPluginManager()
                                .getPlugin("SimpleCoins").getDataFolder().toPath()
                                .resolve("grants"));
                        if (!reopened.pending(ID).isEmpty()
                                || reopened.historySnapshot().size() != 1
                                || !reopened.historySnapshot().getFirst().delivered()
                                || !ledger.pending(ID).isEmpty()
                                || vetoedSaveEvents.get() != 1
                                || !lateFenceClosed.get()
                                || !BigInteger.valueOf(5000).equals(totalSavedMoney(saved()))
                                || !Arrays.equals(savedBeforeAck, Files.readAllBytes(dataFile())))
                            throw new IllegalStateException(
                                    "Durable ACK of verified saved coin is inconsistent");
                        ledger.acknowledge(grant, proof.receipt().completedSequence());
                        if (!Arrays.equals(savedBeforeAck, Files.readAllBytes(dataFile())))
                            throw new IllegalStateException("Repeated ACK rewrote the playerdata");
                        getLogger().info("PAPER_A02_INVSEE_ONE_DURABLE_ACK_AFTER_KNOWN_WRITER_DRAIN");
                        getLogger().info("PAPER_A02_INVSEE_DRAIN_GUARD_STILL_DEFAULT_DENY");
                        getLogger().info("PAPER_A02_INVSEE_DRAIN_ABRUPT_HALT_AFTER_ACK");
                        // Actual playerdata, directory and ledger must survive
                        // a cold second Paper JVM without shutdown hooks.
                        Runtime.getRuntime().halt(140);
                        throw new AssertionError("Expected abrupt halt did not terminate JVM");
                    } catch (Throwable error) {
                        fail(error);
                    }
                }, 2L));
    }

    /**
     * NEGATIVE: the one InvSee++ future has already completed, second InvSee++
     * save was vetoed and a genuine full-price/receipt readback succeeded.
     * Before committing its old sequence, insert an unrelated NATIVE Paper
     * writer, which never asks InvSee++ for permission. This is an intentionally
     * UNSAFE low-level ACK proof, not an authorized issuance implementation.
     */
    private void verifyNativeSaveAfterKnownDrainBeforeAck(
            SavedGrantDeliveryReadback.Proof oldProof) throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !testNativePostDrainFalseAck
                || originalDetachedPaperWriter == null
                || alreadyAdmittedSaveEvents.get() != 1
                || vetoedSaveEvents.get() != 1
                || !lateFenceClosed.get()
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Known InvSee drain did not precede native writer");
        if (!BigInteger.valueOf(5000).equals(oldProof.matchedMonetaryValue())
                || oldProof.receipt().completedSequence() != grant.sequence())
            throw new IllegalStateException("Old receipt/physical source was not valid");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_OLD_SAVED_PROOF_WAS_VALID");
        Player nativeWriter = originalDetachedPaperWriter;
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack[] original = nativeWriter.getInventory().getStorageContents();
        if (original.length != PaperGrantInventorySlotPlan.STORAGE_SLOTS
                || codec.identify(original[1]).orElse(null)
                    != CoinDenomination.FIVE_THOUSAND
                || original[0] == null
                || original[0].getType() != Material.STONE)
            throw new IllegalStateException("Native Paper writer original items changed");
        nativeWriter.getInventory().setItem(1, null);
        nativeWriter.saveData();
        forceActualPlayerdataAndContainingDirectory();
        Map<String, Object> after = saved();
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(after, ledger, ID);
        if (!BigInteger.ZERO.equals(totalSavedMoney(after))
                || receipt.active()
                || receipt.completedSequence() != grant.sequence()
                || vetoedSaveEvents.get() != 1
                || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getFirst().delivered()
                || !(after.get("Inventory") instanceof java.util.List<?> items)
                || items.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))
                    && stack.get("Slot") instanceof Number index
                    && index.intValue() == 0))
            throw new IllegalStateException(
                    "Native save did not preserve signed receipt/STONE or did trigger InvSee");
        boolean rejected = false;
        try {
            SavedGrantDeliveryFileReadback.verify(
                    discover(), ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
        } catch (java.io.IOException expected) {
            rejected = true;
        }
        if (!rejected)
            throw new IllegalStateException("Fresh readback accepted absent 5000C");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_WRITER_BYPASSED_INVSEE_EVENT");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_FRESH_PROOF_REJECTED_STALE_COIN");

        // Deliberately expose the insufficient LOW-LEVEL GrantLedger API.
        // The validated value at oldProof's observation no longer exists.
        byte[] coinlessBytes = Files.readAllBytes(dataFile());
        ledger.acknowledge(grant, oldProof.receipt().completedSequence());
        GrantLedger reread = new GrantLedger(Bukkit.getPluginManager()
                .getPlugin("SimpleCoins").getDataFolder().toPath().resolve("grants"));
        if (reread.historySnapshot().size() != 1
                || !reread.historySnapshot().getFirst().delivered()
                || !reread.pending(ID).isEmpty()
                || !BigInteger.ZERO.equals(totalSavedMoney(saved()))
                || !Arrays.equals(coinlessBytes, Files.readAllBytes(dataFile()))
                || vetoedSaveEvents.get() != 1)
            throw new IllegalStateException("Expected old-proof false ACK not durable");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_STALE_PROOF_FALSE_ACK_DURABLE");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_GUARD_STILL_DEFAULT_DENY");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_ABRUPT_HALT_AFTER_FALSE_ACK");
        Runtime.getRuntime().halt(142);
        throw new AssertionError("False-ACK negative probe halt returned");
    }

    /** Real second JVM replays a falsely acknowledged original grant. */
    private void verifyNativePostDrainFalseAckOnColdRestart() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !replayNativePostDrainFalseAck
                || !Files.isRegularFile(dataFile(), LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Invalid native post-drain restart");
        GrantLedger reread = actualProductLedger();
        Map<String, Object> after = saved();
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(after, reread, ID);
        if (!BigInteger.ZERO.equals(totalSavedMoney(after))
                || receipt.active() || receipt.completedSequence() != 1
                || reread.historySnapshot().size() != 1
                || !reread.historySnapshot().getFirst().delivered()
                || !reread.pending(ID).isEmpty()
                || !(after.get("Inventory") instanceof java.util.List<?> items)
                || items.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))))
            throw new IllegalStateException(
                    "False ACK, lost currency or signed receipt not reproduced");
        getLogger().info(
                "PAPER_A02_NATIVE_POST_DRAIN_FALSE_ACK_COLD_RESTART_COINLESS_DELIVERED_ONE");
        getLogger().info("PAPER_A02_NATIVE_POST_DRAIN_RESTART_GUARD_STILL_DEFAULT_DENY");
        Bukkit.shutdown();
    }

    /**
     * NEGATIVE actual-Paper test: rejecting every new InvSee++ save event
     * does not interpose on Bukkit's native Player.saveData() entrypoint.
     * The retained detached CraftPlayer is the SAME original instance that
     * produced the authentic signed coin + receipt in UUID.dat.
     */
    private void verifyPaperDirectWriterBypassesInvseeEvent() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !testPaperDirectAfterInvseeVeto || vetoedSaveEvents.get() != 1
                || originalDetachedPaperWriter == null
                || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("InvSee event veto not verified before Paper direct save");
        var before = SavedGrantDeliveryFileReadback.verify(
                discover(), ledger, ID, staged, priorRoot,
                MAX_COMPRESSED, MAX_INFLATED);
        if (!BigInteger.valueOf(5000).equals(before.matchedMonetaryValue())
                || before.receipt().completedSequence() != grant.sequence())
            throw new IllegalStateException("Missing original saved currency before direct Paper writer");
        getLogger().info("PAPER_A02_INVSEE_VETO_FIRST_REAL_SAVE_CANCELLED_5000C_VALID");

        Player writer = originalDetachedPaperWriter;
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack[] originalStorage = writer.getInventory().getStorageContents();
        if (originalStorage.length != PaperGrantInventorySlotPlan.STORAGE_SLOTS
                || codec.identify(originalStorage[1]).orElse(null)
                    != CoinDenomination.FIVE_THOUSAND
                || originalStorage[0] == null
                || originalStorage[0].getType() != Material.STONE)
            throw new IllegalStateException("Actual original Paper player lacks grant or preserved STONE");
        writer.getInventory().setItem(1, null);
        if (testNativeAgentEntry) {
            // A JVM startup agent, not an InvSee event, intercepts the actual
            // CraftPlayer.saveData() bytecode ENTRY. This test has a genuine
            // before-save file and one detached Paper player; no product ACK.
            System.setProperty("a02.nativeAgent.closed", "true");
            boolean denied = false;
            try {
                writer.saveData();
            } catch (IllegalStateException expected) {
                denied = expected.getMessage() != null
                        && expected.getMessage().contains("test-only Paper native save");
            }
            if (!denied)
                throw new IllegalStateException("Paper native save entry not intercepted");
            if (vetoedSaveEvents.get() != 1)
                throw new IllegalStateException("Native entry rejection entered InvSee event path");
            Map<String, Object> still = saved();
            var receipt = SavedGrantReceiptNbt.readAndVerify(still, ledger, ID);
            if (!BigInteger.valueOf(5000).equals(totalSavedMoney(still))
                    || receipt.active()
                    || receipt.completedSequence() != grant.sequence()
                    || ledger.pending(ID).size() != 1
                    || ledger.historySnapshot().getFirst().delivered())
                throw new IllegalStateException(
                        "Real playerdata or original pending grant changed under agent gate");
            if (!(still.get("Inventory") instanceof java.util.List<?> retained)
                    || retained.stream().noneMatch(item ->
                        item instanceof Map<?, ?> stack
                        && "minecraft:stone".equals(stack.get("id"))
                        && stack.get("Slot") instanceof Number index
                        && index.intValue() == 0))
                throw new IllegalStateException("Native entry gate lost unrelated STONE");
            SavedGrantDeliveryFileReadback.verify(
                    discover(), ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
            getLogger().info("PAPER_A02_AGENT_REAL_NATIVE_SAVEDATA_REJECTED_UNCHANGED_COIN");
            forceActualPlayerdataAndContainingDirectory();
            getLogger().info("PAPER_A02_AGENT_REAL_NATIVE_SAVEDATA_FSYNC_BEFORE_ABRUPT_HALT");
            Runtime.getRuntime().halt(143);
            throw new AssertionError("Native agent proof halt returned");
        }
        // Original negative baseline: native save bypasses InvSee veto.
        writer.saveData();
        if (vetoedSaveEvents.get() != 1)
            throw new IllegalStateException("Paper.saveData unexpectedly fired InvSee save event");

        Map<String, Object> after = saved();
        if (!BigInteger.ZERO.equals(totalSavedMoney(after)))
            throw new IllegalStateException("Direct Paper save did not replace saved monetary state");
        GrantReceiptState savedReceipt = SavedGrantReceiptNbt.readAndVerify(after, ledger, ID);
        if (savedReceipt.active()
                || savedReceipt.completedSequence() != grant.sequence()
                || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("Paper direct save changed receipt or issued false ACK");
        if (!(after.get("Inventory") instanceof java.util.List<?> inventory)
                || inventory.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))
                    && stack.get("Slot") instanceof Number index
                    && index.intValue() == 0))
            throw new IllegalStateException("Paper direct save lost unrelated STONE");
        boolean rejected = false;
        try {
            SavedGrantDeliveryFileReadback.verify(
                    discover(), ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
        } catch (java.io.IOException expected) {
            rejected = true;
        }
        if (!rejected)
            throw new IllegalStateException("Fresh grant proof approved direct Paper coin loss");
        getLogger().info("PAPER_A02_NATIVE_PLAYER_SAVEDATA_DID_NOT_INVOKE_INVSEE_VETO");
        getLogger().info("PAPER_A02_NATIVE_PLAYER_SAVEDATA_ERASED_5000C_PRESERVED_RECEIPT_STONE");
        getLogger().info("PAPER_A02_NATIVE_WRITER_FRESH_PROOF_REJECTED_LEDGER_PENDING");
        // Evidence must survive abrupt JVM termination, not just memory.
        forceActualPlayerdataAndContainingDirectory();
        if (vetoedSaveEvents.get() != 1 || !BigInteger.ZERO.equals(totalSavedMoney(saved())))
            throw new IllegalStateException("Direct Paper source changed before abrupt halt");
        getLogger().info("PAPER_A02_NATIVE_SAVE_BYPASS_FSYNC_COMPLETE_ABRUPT_HALT");
        Runtime.getRuntime().halt(141);
        throw new AssertionError("Paper direct save halt returned");
    }

    /**
     * A new Paper JVM without the Agent must still see the original durable
     * 5000C coin, signed receipt and pending grant from the rejected write.
     */
    private void verifyNativeAgentAfterHalt() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !recoverNativeAgentAfterHalt)
            throw new IllegalStateException("Native agent restart probe not isolated");
        GrantLedger journal = actualProductLedger();
        Map<String, Object> disk = saved();
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(disk, journal, ID);
        if (!BigInteger.valueOf(5000).equals(totalSavedMoney(disk))
                || receipt.active() || receipt.completedSequence() != 1
                || journal.pending(ID).size() != 1
                || journal.historySnapshot().size() != 1
                || journal.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("Native agent cold restart changed saved grant");
        if (!(disk.get("Inventory") instanceof java.util.List<?> retained)
                || retained.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))
                    && stack.get("Slot") instanceof Number index
                    && index.intValue() == 0))
            throw new IllegalStateException("Native agent cold restart lost STONE");
        getLogger().info("PAPER_A02_NATIVE_AGENT_COLD_RESTART_COIN_RECEIPT_PENDING");
        Bukkit.shutdown();
    }

    /** Cold Paper JVM must observe the same actual unACKed, coinless file. */
    private void verifyPaperDirectAfterHalt() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !recoverPaperDirectAfterHalt)
            throw new IllegalStateException("Invalid native-save cold-recovery probe");
        GrantLedger reopened = actualProductLedger();
        if (reopened.pending(ID).size() != 1
                || reopened.historySnapshot().size() != 1
                || reopened.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("Original unACKed reservation was lost after direct save");
        Map<String, Object> after = saved();
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(after, reopened, ID);
        if (!BigInteger.ZERO.equals(totalSavedMoney(after))
                || receipt.active() || receipt.completedSequence() != 1
                || !(after.get("Inventory") instanceof java.util.List<?> inventory)
                || inventory.stream().noneMatch(item ->
                    item instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))))
            throw new IllegalStateException("Native Paper saved overwrite not replayed after cold restart");
        getLogger().info("PAPER_A02_NATIVE_SAVE_BYPASS_COLD_RESTART_COINLESS_RECEIPT_PENDING");
        getLogger().info("PAPER_A02_NATIVE_SAVE_BYPASS_GUARD_STILL_DEFAULT_DENY");
        Bukkit.shutdown();
    }

    /** Lock the OLD target inode, without changing playerdata. */
    private void acquirePlayerdataLockOnVerifiedFile() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || heldPlayerdataLock != null)
            throw new IllegalStateException("File lock probe must use one isolated main-thread UUID");
        Path file = dataFile();
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file))
            throw new IllegalStateException("Invalid playerdata file for advisory lock probe");
        lockedPlayerdataChannel = FileChannel.open(file, StandardOpenOption.READ,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        heldPlayerdataLock = lockedPlayerdataChannel.tryLock();
        if (heldPlayerdataLock == null || !heldPlayerdataLock.isValid())
            throw new IllegalStateException("Exclusive advisory lock unavailable");
        lockedPlayerdataFileKey = Files.readAttributes(
                file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
        getLogger().info("PAPER_A02_ACTUAL_UUID_DAT_OS_ADVISORY_EXCLUSIVE_LOCK_HELD");
    }

    private void releasePlayerdataLock() throws Exception {
        try {
            if (heldPlayerdataLock != null && heldPlayerdataLock.isValid())
                heldPlayerdataLock.release();
        } finally {
            if (lockedPlayerdataChannel != null) lockedPlayerdataChannel.close();
            heldPlayerdataLock = null;
            lockedPlayerdataChannel = null;
        }
    }

    /** A genuine InvSee++ save on the SAME path must not be mistaken for an ACK. */
    private void verifyAdvisoryLockIsNotWriterFence() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || heldPlayerdataLock == null || !heldPlayerdataLock.isValid()
                || lockedPlayerdataChannel == null || !lockedPlayerdataChannel.isOpen())
            throw new IllegalStateException("OS advisory lock did not remain held during InvSee save");
        Map<String, Object> current = saved();
        if (!BigInteger.ZERO.equals(totalSavedMoney(current)))
            throw new IllegalStateException("Real InvSee++ could not replace physical saved coin");
        GrantReceiptState receipt = SavedGrantReceiptNbt.readAndVerify(current,
                ledger, ID);
        if (receipt.active() || receipt.completedSequence() != verifiedSequence)
            throw new IllegalStateException("Unrelated InvSee write damaged original signed receipt");
        var items = current.get("Inventory");
        if (!(items instanceof java.util.List<?> list)
                || list.stream().noneMatch(raw -> raw instanceof Map<?, ?> stack
                    && "minecraft:stone".equals(stack.get("id"))
                    && stack.get("Slot") instanceof Number slot && slot.intValue() == 0))
            throw new IllegalStateException("Unrelated STONE was lost");
        if (ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("Lock test unexpectedly acknowledged an undelivered grant");
        boolean refused = false;
        try {
            SavedGrantDeliveryFileReadback.verify(
                    manifest, ledger, ID, staged, priorRoot,
                    MAX_COMPRESSED, MAX_INFLATED);
        } catch (java.io.IOException expected) {
            refused = true;
        }
        if (!refused)
            throw new IllegalStateException("Fresh readback approved invalidated old proof");
        Object newKey = Files.readAttributes(
                dataFile(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).fileKey();
        getLogger().info("PAPER_A02_OS_LOCK_REMAINED_VALID_AFTER_INVSEE_SAVE"
                + " originalFileKey=" + lockedPlayerdataFileKey
                + " newFileKey=" + newKey
                + " replacedPath=" + !java.util.Objects.equals(lockedPlayerdataFileKey, newKey));
        getLogger().info("PAPER_A02_INVSEE_WRITER_OVERWROTE_SAVED_COIN_DESPITE_OLD_FILE_LOCK");
        getLogger().info("PAPER_A02_ADVISORY_UUID_DAT_LOCK_NOT_SUFFICIENT_WRITER_FENCE");
        getLogger().info("PAPER_A02_ADVISORY_LOCK_TEST_LEDGER_STILL_PENDING_NO_ACK");
        releasePlayerdataLock();
        Bukkit.shutdown();
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
        if (vetoAndDurableAck) verifyProtectedFsyncAndAck();
        Bukkit.shutdown();
    }


    /**
     * Bounded positive integration: the REAL InvSee++ writer was vetoed
     * while this isolated observer held the logical recipient barrier.
     * Only then force the actual saved file+directory, re-read from a genuine
     * manifest and commit the original reservation. Other asynchronous or
     * uncooperative writers are NOT fenced by this event handler.
     */
    private void verifyProtectedFsyncAndAck() throws Exception {
        if (!Bukkit.isPrimaryThread() || !Bukkit.getOnlinePlayers().isEmpty()
                || !vetoAndDurableAck || vetoedSaveEvents.get() != 1
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Cooperative ACK cannot proceed without veto");
        byte[] beforeAck = Files.readAllBytes(dataFile());
        forceActualPlayerdataAndContainingDirectory();
        var proof = SavedGrantDeliveryFileReadback.verify(
                discover(), ledger, ID, staged, priorRoot,
                MAX_COMPRESSED, MAX_INFLATED);
        if (proof.receipt().active()
                || proof.receipt().completedSequence() != grant.sequence()
                || !BigInteger.valueOf(5000).equals(proof.matchedMonetaryValue())
                || !Arrays.equals(beforeAck, Files.readAllBytes(dataFile())))
            throw new IllegalStateException("Forced saved grant changed before ACK");
        getLogger().info("PAPER_A02_COOPERATIVE_VETO_FILE_DIR_FSYNC_AND_FRESH_PROOF");
        ledger.acknowledge(grant, proof.receipt().completedSequence());
        GrantLedger reopened = new GrantLedger(Bukkit.getPluginManager()
                .getPlugin("SimpleCoins").getDataFolder().toPath().resolve("grants"));
        if (reopened.historySnapshot().size() != 1
                || !reopened.historySnapshot().getFirst().delivered()
                || !reopened.pending(ID).isEmpty()
                || !ledger.pending(ID).isEmpty()
                || !Arrays.equals(beforeAck, Files.readAllBytes(dataFile()))
                || !BigInteger.valueOf(5000).equals(totalSavedMoney(saved())))
            throw new IllegalStateException("ACK was not durable or physical coin changed");
        // A repeated acknowledgement must not materialize a second coin
        // or rewrite the already committed playerdata.
        ledger.acknowledge(grant, proof.receipt().completedSequence());
        if (!Arrays.equals(beforeAck, Files.readAllBytes(dataFile()))
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Repeated ACK altered saved grant state");
        getLogger().info("PAPER_A02_COOPERATIVE_VETO_ONE_DURABLE_ACK_NO_ITEM_CHANGE");
        getLogger().info("PAPER_A02_COOPERATIVE_VETO_GUARD_REMAINS_CLOSED");
        getLogger().info("PAPER_A02_COOPERATIVE_VETO_ABRUPT_HALT_AFTER_DURABLE_ACK");
        // A separate Paper JVM must find the exact original physical grant
        // and one durable ACK despite this process losing shutdown hooks.
        Runtime.getRuntime().halt(139);
        throw new AssertionError("Protected ACK halt returned");
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
