package co.surumene.simplecoins.a02probe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.domain.SavedPlayerAssets;
import co.surumene.simplecoins.domain.WorldScope;
import co.surumene.simplecoins.domain.WorldStorageDiscovery;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.paper.SavedGrantDeliveryReadback;
import java.lang.reflect.InvocationTargetException;
import co.surumene.simplecoins.paper.CoinItemCodec;
import co.surumene.simplecoins.paper.PaperGrantInventorySlotPlan;
import co.surumene.simplecoins.paper.PaperGrantInventoryMaterializer;
import co.surumene.simplecoins.paper.SavedGrantDeliveryFileReadback;
import co.surumene.simplecoins.paper.SavedGrantReceiptNbt;
import co.surumene.simplecoins.paper.PaperGrantReceiptPdc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test-only, isolated Paper 26.2 proof. No networking, real player, production
 * grant, world data or admission flag is ever modified. A synthetic NMS player
 * exercises CraftPlayer.saveData/loadData against the real Paper playerIo.
 * This is intentionally NOT a proof of OS-level crash durability or G3.
 */
public final class PaperSaveProbe extends JavaPlugin {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000a002");
    private static final int PAPER_COUNT = 1;
    private static final long ISSUED_VALUE = 5000;

    @Override public void onEnable() {
        if (!"127.0.0.1".equals(getServer().getIp()) || !getServer().getOnlinePlayers().isEmpty()) {
            throw new IllegalStateException("Disposable loopback/no-online-players CI only");
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            String phase = System.getProperty("a02.probe.phase", "save");
            try {
                Player player = createDetachedPlayer();
                if ("save".equals(phase)) savePhase(player);
                else if ("reload".equals(phase)) reloadPhase(player);
                else if ("acked-reload".equals(phase)) ackedReloadPhase(player);
                else if ("verified-delivery".equals(phase)) verifiedDeliveryPhase(player);
                else if ("product-coordinator-full".equals(phase)) productCoordinatorFullPhase(player);
                else if ("product-coordinator-acked-reload".equals(phase))
                    productCoordinatorAckedReloadPhase(player);
                else if ("partial-save".equals(phase)) partialSavePhase(player);
                else if ("partial-resume".equals(phase)) partialResumePhase(player);
                else if ("final-ack-recovery".equals(phase)) finalAckRecoveryPhase(player);
                else if ("protected-final-acked-reload".equals(phase)) protectedFinalAckedReloadPhase(player);
                else if ("before-save-crash".equals(phase)) beforeSaveCrashPhase(player);
                else if ("recover-unsaved".equals(phase)) recoverUnsavedPhase(player);
                else if ("unsaved-acked-reload".equals(phase)) unsavedAckedReloadPhase(player);
                else throw new IllegalArgumentException("Unknown probe phase " + phase);
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "PAPER_A02_PROBE_FAILED", failure);
                Bukkit.shutdown();
                return;
            }
            // The CI harness owns SIGKILL after this marker. Do not run
            // Paper's shutdown hooks before the save-before-ack crash check.
            if (Boolean.getBoolean("a02.probe.awaitSigkill")) {
                if ("save".equals(phase)) {
                    getLogger().info("PAPER_A02_UNACKED_CRASH_POINT_READY");
                    return;
                }
                if ("reload".equals(phase)) {
                    getLogger().info("PAPER_A02_ACKED_CRASH_POINT_READY");
                    return;
                }
                if ("partial-save".equals(phase)) {
                    // The preceding phase read back both physical money and
                    // the partial receipt while the journal was still pending.
                    // Force-kill this actual Paper JVM instead of relying on
                    // a clean server shutdown to flush playerdata.
                    getLogger().info("PAPER_A02_PARTIAL_SAVED_BEFORE_ACK_CRASH_POINT_READY");
                    return;
                }
                if ("partial-resume".equals(phase)) {
                    // Full final receipt + item proof is saved, but the journal
                    // still says pending; SIGKILL the JVM before any ACK.
                    getLogger().info("PAPER_A02_FINAL_RECEIPT_SAVED_PRE_ACK_CRASH_POINT_READY");
                    return;
                }
                if ("before-save-crash".equals(phase)) {
                    getLogger().info("PAPER_A02_PRE_SAVE_SIGKILL_POINT_READY");
                    return;
                }
                throw new IllegalArgumentException("Unexpected SIGKILL phase " + phase);
            }
            Bukkit.shutdown();
        // The product's asynchronous saved-source/receipt audit must have
        // finished before this disposable test begins issuing and saving.
        }, 120L);
    }

    private Path dataFile() {
        return getServer().getWorldContainer().toPath().toAbsolutePath().normalize()
                .resolve("world/players/data/" + ID + ".dat");
    }

    private Path grantDirectory() {
        // The product's guarded startup audits THIS journal against Paper's
        // real playerdata. An unrelated test journal creates a spurious
        // unknown receipt and makes the second JVM correctly fail closed.
        var product = getServer().getPluginManager().getPlugin("SimpleCoins");
        if (product == null || !product.isEnabled())
            throw new IllegalStateException("Product grant journal is not available");
        return product.getDataFolder().toPath().resolve("grants");
    }

    /** Exercise the actual active issuance ledger, not a competing writer. */
    private GrantLedger activeProductLedger() throws ReflectiveOperationException {
        var product = getServer().getPluginManager().getPlugin("SimpleCoins");
        if (product == null || !product.isEnabled())
            throw new IllegalStateException("Product grant ledger unavailable");
        Field field = product.getClass().getDeclaredField("issuanceGrants");
        field.setAccessible(true);
        Object value = field.get(product);
        if (!(value instanceof GrantLedger ledger))
            throw new IllegalStateException("Product did not initialize authentic GrantLedger");
        return ledger;
    }

    private static GrantReceiptState completedReceipt(GrantLedger.Grant grant) {
        // The physical item and this completed cursor will be persisted
        // together BEFORE the durable ledger is acknowledged.
        return new GrantReceiptState(grant.sequence(), 0, null, 0);
    }

    private static byte[] expectedReceipt(GrantLedger.Grant grant) {
        return PaperGrantReceiptPdc.toSavedBytes(completedReceipt(grant));
    }

    private Player createDetachedPlayer() throws ReflectiveOperationException {
        Object bukkitServer = getServer();
        Object mc = bukkitServer.getClass().getMethod("getServer").invoke(bukkitServer);
        Class<?> minecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
        Object overworld = minecraftServer.getMethod("overworld").invoke(mc);
        Class<?> serverLevel = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> gameProfile = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = gameProfile.getConstructor(UUID.class, String.class)
                .newInstance(ID, "ProbePaperSave");
        Class<?> clientType = Class.forName("net.minecraft.server.level.ClientInformation");
        Object client = clientType.getMethod("createDefault").invoke(null);
        Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
        Constructor<?> constructor = serverPlayer.getConstructor(
                minecraftServer, serverLevel, gameProfile, clientType);
        Object detached = constructor.newInstance(mc, overworld, profile, client);
        Object wrapper = serverPlayer.getMethod("getBukkitEntity").invoke(detached);
        if (!(wrapper instanceof Player player))
            throw new IllegalStateException("NMS player lacks CraftPlayer");
        if (player.isOnline())
            throw new IllegalStateException("Synthetic player unexpectedly online");
        return player;
    }

    /** Call the actual production read-only compressed NBT decoder. */
    private Map<String, Object> readDisk(Path file) throws Exception {
        return PlayerDataReader.read(file, 1024 * 1024, 16 * 1024 * 1024);
    }

    private void assertSavedPair(Path file, byte[] expected) throws Exception {
        if (!Files.isRegularFile(file))
            throw new IllegalStateException("Real Paper playerdata file missing: " + file);
        Map<String, Object> root = readDisk(file);
        Object rawValues = root.get("BukkitValues");
        if (!(rawValues instanceof Map<?, ?> values))
            throw new IllegalStateException("Missing BukkitValues: " + root.keySet());
        Object savedReceipt = values.get(PaperGrantReceiptPdc.RECEIPT.toString());
        if (!(savedReceipt instanceof byte[] bytes) || !Arrays.equals(bytes, expected))
            throw new IllegalStateException("PDC receipt mismatch in real playerdata");
        Object rawInv = root.get("Inventory");
        if (!(rawInv instanceof List<?> inventory))
            throw new IllegalStateException("Inventory absent from real playerdata: " + root.keySet());
        boolean matches = false;
        boolean preserved = false;
        for (Object raw : inventory) {
            if (raw instanceof Map<?, ?> item
                    && item.get("count") instanceof Number count) {
                if ("minecraft:paper".equals(item.get("id"))
                        && count.intValue() == PAPER_COUNT) matches = true;
                if ("minecraft:stone".equals(item.get("id"))
                        && count.intValue() == 2
                        && item.get("Slot") instanceof Number slot
                        && slot.intValue() == 9) preserved = true;
            }
        }
        if (!matches || !preserved)
            throw new IllegalStateException("Saved coin or unrelated item lost: " + inventory);
    }

    private void savePhase(Player player) throws Exception {
        Path file = dataFile();
        if (Files.exists(file))
            throw new IllegalStateException("Probe requires fresh UUID playerdata");
        GrantLedger ledger = activeProductLedger();
        GrantLedger.Grant grant = ledger.reserve(ID, ISSUED_VALUE, "a02-ci");
        if (grant.sequence() != 1)
            throw new IllegalStateException("Expected first reserved grant");
        // Real Paper storage with no vacant slot must not stage a grant
        // or alter any unrelated original item.
        ItemStack[] full = new ItemStack[PaperGrantInventorySlotPlan.STORAGE_SLOTS];
        for (int i = 0; i < full.length; i++) full[i] = new ItemStack(Material.STONE, 64);
        player.getInventory().setStorageContents(full);
        ItemStack[] unchanged = player.getInventory().getStorageContents();
        var denied = PaperGrantInventorySlotPlan.stage(
                ledger, ID, grant, GrantReceiptState.empty(), unchanged);
        if (denied.isPresent())
            throw new IllegalStateException("Full Paper inventory unexpectedly permits a grant");
        if (!Arrays.equals(unchanged, player.getInventory().getStorageContents()))
            throw new IllegalStateException("Full inventory preflight altered stored items");
        if (ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Full-inventory preflight acknowledged grant");
        getLogger().info("PAPER_A02_FULL_INVENTORY_NO_DELIVERY_PASSED");
        player.getInventory().setStorageContents(
                new ItemStack[PaperGrantInventorySlotPlan.STORAGE_SLOTS]);

        byte[] receipt = expectedReceipt(grant);
        ItemStack coin = new CoinItemCodec(CoinItemCodec.defaultNames())
                .create(CoinDenomination.fromValue((int) ISSUED_VALUE), PAPER_COUNT);
        player.getInventory().setItem(0, coin);
        player.getInventory().setItem(9, new ItemStack(Material.STONE, 2));
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), completedReceipt(grant));

        // Paper's real playerIo.save returns silently under this Spigot setting.
        Field disabled = Class.forName("org.spigotmc.SpigotConfig")
                .getField("disablePlayerDataSaving");
        boolean previous = disabled.getBoolean(null);
        try {
            disabled.setBoolean(null, true);
            player.saveData();
            if (Files.exists(file))
                throw new IllegalStateException("Save-disabled Paper unexpectedly wrote playerdata");
            getLogger().info("PAPER_A02_SAVE_DISABLED_VOID_RETURN_PASSED");
        } finally {
            disabled.setBoolean(null, previous);
        }

        player.saveData();
        assertSavedPair(file, receipt);
        if (ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Saved grant was acknowledged too early");
        getLogger().info("PAPER_A02_REAL_SAVE_INVENTORY_RECEIPT_PAIR_PASSED");
        getLogger().info("PAPER_A02_REAL_COIN_RECEIPT_UNACKED_PASSED");

        // A subsequent disabled save must not change this authoritative disk generation.
        player.getInventory().setItem(0, new ItemStack(Material.STONE, 1));
        disabled.setBoolean(null, true);
        try {
            player.saveData();
        } finally {
            disabled.setBoolean(null, previous);
        }
        assertSavedPair(file, receipt);
        getLogger().info("PAPER_A02_STALE_DISK_AFTER_VOID_SAVE_PASSED");

        // Paper may move a blocking UUID.dat directory aside and still save.
        // Instead remove ALL write permissions from the real parent directory
        // while saving. This Linux CI probe must be a non-root user.
        Path parent = file.getParent();
        Set<PosixFilePermission> oldMode = Files.getPosixFilePermissions(parent);
        Set<PosixFilePermission> readOnly = EnumSet.copyOf(oldMode);
        readOnly.remove(PosixFilePermission.OWNER_WRITE);
        readOnly.remove(PosixFilePermission.GROUP_WRITE);
        readOnly.remove(PosixFilePermission.OTHERS_WRITE);
        byte[] beforeFailure = Files.readAllBytes(file);
        try {
            Files.setPosixFilePermissions(parent, readOnly);
            if (Files.isWritable(parent))
                throw new IllegalStateException("CI directory is still writable; fault not injected");
            player.saveData();
            if (!Arrays.equals(beforeFailure, Files.readAllBytes(file)))
                throw new IllegalStateException("Failed save unexpectedly changed playerdata bytes");
            if (ledger.pending(ID).size() != 1)
                throw new IllegalStateException("Failed save acknowledged a grant");
        } finally {
            Files.setPosixFilePermissions(parent, oldMode);
        }
        assertSavedPair(file, receipt);
        getLogger().info("PAPER_A02_SAVE_EXCEPTION_SWALLOWED_NO_ACK_PASSED");
    }

    private void reloadPhase(Player player) throws Exception {
        Path file = dataFile();
        GrantLedger ledger = activeProductLedger();
        List<GrantLedger.Grant> pending = ledger.pending(ID);
        if (pending.size() != 1)
            throw new IllegalStateException("Expected one saved-but-unacknowledged grant");
        GrantLedger.Grant grant = pending.getFirst();
        byte[] receipt = expectedReceipt(grant);
        assertSavedPair(file, receipt);
        player.loadData(); // real CraftPlayer -> PlayerDataStorage.load
        ItemStack item = player.getInventory().getItem(0);
        if (item == null || item.getType() != Material.PAPER
                || item.getAmount() != PAPER_COUNT)
            throw new IllegalStateException("Saved inventory was not restored");
        if (!new CoinItemCodec(CoinItemCodec.defaultNames()).identify(item)
                .filter(d -> d.value() == ISSUED_VALUE).isPresent())
            throw new IllegalStateException("Restored paper has invalid real coin PDC/model");
        ItemStack retained = player.getInventory().getItem(9);
        if (retained == null || retained.getType() != Material.STONE || retained.getAmount() != 2)
            throw new IllegalStateException("Unrelated existing storage was not restored");
        getLogger().info("PAPER_A02_EXISTING_STORAGE_RESTART_PRESERVED_PASSED");
        GrantReceiptState pdc = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (!pdc.equals(completedReceipt(grant)))
            throw new IllegalStateException("Saved receipt PDC was not restored");
        getLogger().info("PAPER_A02_RESTART_REAL_LOAD_PAIR_PASSED");

        // A real save-before-ack crash boundary: on a fresh process the
        // physical receipt is already persisted, but the ledger is pending.
        if (!ledger.wasApplied(grant, pdc.completedSequence()))
            throw new IllegalStateException("Persisted receipt not matched to reservation");
        ledger.acknowledge(grant, pdc.completedSequence());
        if (!new GrantLedger(grantDirectory()).pending(ID).isEmpty())
            throw new IllegalStateException("Recovered acknowledgement did not persist");
        getLogger().info("PAPER_A02_RESTART_RECOVER_ACK_PASSED");
    }

    /**
     * Third independent Paper process. The second process was SIGKILLed
     * immediately after durable ACK; neither coin nor journal may duplicate.
     * This probe never calls reserve(), stages or saves a new physical item.
     */
    private void ackedReloadPhase(Player player) throws Exception {
        Path file = dataFile();
        Path ledgerPath = grantDirectory();
        GrantLedger ledger = new GrantLedger(ledgerPath);
        List<GrantLedger.Grant> history = ledger.historySnapshot();
        if (history.size() != 1)
            throw new IllegalStateException("Expected one and only one issuance reservation");
        GrantLedger.Grant delivered = history.getFirst();
        if (!delivered.player().equals(ID) || delivered.sequence() != 1
                || !delivered.delivered() || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Crash after ACK rolled back journal or reopened pending grant");
        getLogger().info("PAPER_A02_THIRD_BOOT_DURABLE_ACK_NO_PENDING_PASSED");

        byte[] playerBefore = Files.readAllBytes(file);
        byte[] grantBefore = Files.readAllBytes(ledgerPath.resolve("1.grant"));
        assertSavedPair(file, expectedReceipt(delivered));

        // The third Paper JVM owns a real playerdata file with a real coin
        // and persistent receipt, written under the shared level root.
        // Inspect the *existing production* world-root-only discovery
        // independently, without fabricating a Manifest.
        var paperWorld = getServer().getWorld("world");
        if (paperWorld == null) throw new IllegalStateException("Missing actual Paper test world");
        Path paperDimension = paperWorld.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path realPlayerData = file.toAbsolutePath().normalize();
        if (realPlayerData.startsWith(paperDimension))
            throw new IllegalStateException("Test world did not use shared Paper playerdata root");
        var observedSourceSet = WorldStorageDiscovery.discover(
                Map.of(paperWorld.getName(), paperDimension),
                new WorldScope(true, Set.of(), Set.of()), 100000);
        if (observedSourceSet.origin() == null)
            throw new IllegalStateException("Production saved discovery omitted provenance");
        boolean indexed = realPlayerData.equals(observedSourceSet.playerDataFiles().get(ID));
        getLogger().info("PAPER_A01_REAL_PLAYERDATA_DISCOVERY_BOUNDARY_OBSERVED"
                + " existingMoneySaved=true"
                + " playerDataOutsideWorldFolder=true"
                + " playerIndexed=" + indexed
                + " observedPlayerSources=" + observedSourceSet.playerDataFiles().size());
        if (!indexed)
            getLogger().warning("PAPER_A01_SHARED_PLAYERDATA_OMISSION_OBSERVED"
                    + " authenticSavedMoney=5000"
                    + " manifestPlayerSources=" + observedSourceSet.playerDataFiles().size());
        else
            getLogger().info("PAPER_A01_SHARED_PLAYERDATA_INCLUDED_IN_MANIFEST");

        // Actual Paper 26.2 physical playerdata lives above the dimension
        // folder. Feed that observed root independently from WorldScope and
        // prove the new manifest uses the *real* already-persisted UUID.dat.
        Path sharedLevelRoot = realPlayerData.getParent().getParent().getParent();
        if (!paperDimension.startsWith(sharedLevelRoot)
                || !Files.isRegularFile(realPlayerData))
            throw new IllegalStateException("Invalid real Paper shared-playerdata witness");
        var sharedManifest = WorldStorageDiscovery.discover(
                Map.of(paperWorld.getName(), paperDimension),
                new WorldScope(true, Set.of(), Set.of(paperWorld.getName())),
                100000, Set.of(sharedLevelRoot));
        if (!realPlayerData.equals(sharedManifest.playerDataFiles().get(ID))
                || sharedManifest.playerDataFiles().size() != 1
                || !sharedManifest.worldFiles().isEmpty())
            throw new IllegalStateException("Shared playerdata was not independently "
                    + "included by real-root source discovery under excluded WorldScope");
        sharedManifest.requireUnchangedDiscovery();
        getLogger().info("PAPER_A01_REAL_SHARED_PLAYERDATA_INCLUDED_IN_MANIFEST"
                + " actualSavedCurrency=5000 playerIndexed=true"
                + " scopedWorldExcluded=true playerSources=1");

        player.loadData();
        ItemStack coin = player.getInventory().getItem(0);
        if (coin == null || coin.getType() != Material.PAPER || coin.getAmount() != PAPER_COUNT
                || new CoinItemCodec(CoinItemCodec.defaultNames())
                        .identify(coin).filter(d -> d.value() == ISSUED_VALUE).isEmpty())
            throw new IllegalStateException("Third process lost or duplicated real coin value");
        ItemStack unrelated = player.getInventory().getItem(9);
        if (unrelated == null || unrelated.getType() != Material.STONE
                || unrelated.getAmount() != 2)
            throw new IllegalStateException("Third process modified existing item in other slot");
        GrantReceiptState receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (!receipt.equals(completedReceipt(delivered))
                || !ledger.wasApplied(delivered, receipt.completedSequence()))
            throw new IllegalStateException("Third process receipt differs from delivered record");
        getLogger().info("PAPER_A02_THIRD_BOOT_COIN_RECEIPT_STORAGE_PASSED");

        // Calling with an authentic already-completed reservation must not
        // reissue physical money or rewrite the durable grant record.
        ledger.acknowledge(delivered, receipt.completedSequence());
        if (!Arrays.equals(grantBefore, Files.readAllBytes(ledgerPath.resolve("1.grant")))
                || !Arrays.equals(playerBefore, Files.readAllBytes(file)))
            throw new IllegalStateException("Idempotent acknowledgement rewrote disk evidence");
        GrantLedger check = new GrantLedger(ledgerPath);
        if (check.historySnapshot().size() != 1
                || !check.historySnapshot().getFirst().delivered()
                || !check.pending(ID).isEmpty())
            throw new IllegalStateException("Repeated ACK altered durable issuance");
        getLogger().info("PAPER_A02_THIRD_BOOT_REPEATED_ACK_IDEMPOTENT_PASSED");
    }
    /**
     * An additional, fourth Paper JVM proves existing production components
     * can be composed on a REAL detached CraftPlayer and an existing saved
     * UUID.dat: stage -> physical ItemStack -> receipt -> saveData -> saved
     * file transition proof -> journal ACK, with no connected network player.
     *
     * This is a CI-only caller, NOT an enabled production delivery path.
     */
    /**
     * Test-only invocation of the REAL unregistered product saved-stage
     * classes. All writer admission proof here is fixture-local: these
     * barriers were NOT installed around Paper's preceding save operation.
     */
    private SavedGrantDeliveryReadback.Proof protectedProductSavedStage(
            String productClass, String operation,
            WorldStorageDiscovery.Manifest savedManifest, GrantLedger ledger,
            GrantLedger.Grant grant, PaperGrantInventorySlotPlan stage,
            Map<String, Object> savedBefore) throws Exception {
        if (!Bukkit.isPrimaryThread() || playerCountIsNonzero())
            throw new IllegalStateException("Product-stage Paper probe is not isolated");
        WriterEpochFence boundedTestOnly = new WriterEpochFence();
        boundedTestOnly.arm();
        WriterEpochFence.Seal seal = boundedTestOnly.seal();
        Class<?> stageClass = Class.forName(productClass, true,
                PaperSaveProbe.class.getClassLoader());
        var finish = stageClass.getDeclaredMethod(operation, WriterEpochFence.class,
                WriterEpochFence.Seal.class, WorldStorageDiscovery.Manifest.class,
                GrantLedger.class, UUID.class, GrantLedger.Grant.class,
                PaperGrantInventorySlotPlan.class, Map.class, int.class, int.class);
        finish.setAccessible(true);
        final SavedGrantDeliveryReadback.Proof result;
        try {
            result = (SavedGrantDeliveryReadback.Proof) finish.invoke(null,
                    boundedTestOnly, seal, savedManifest, ledger, ID, grant, stage,
                    savedBefore, 16 * 1024 * 1024, 64 * 1024 * 1024);
        } catch (InvocationTargetException failure) {
            Throwable underlying = failure.getCause();
            if (underlying instanceof Exception error) throw error;
            if (underlying instanceof Error error) throw error;
            throw new IllegalStateException("Unknown product protected stage failure", underlying);
        }
        if (boundedTestOnly.state() != WriterEpochFence.State.SEALED)
            throw new IllegalStateException("Bounded product stage did not retain sealed epoch");
        boundedTestOnly.requireDrained(seal);
        return result;
    }

    private static boolean playerCountIsNonzero() {
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    /**
     * Alternate fourth JVM on a copy of the actual third-boot baseline: this
     * must exercise the PRODUCT coordinator from plan through real Paper
     * saveData, sealed fresh readback and durable ACK, not test glue.
     */
    private void productCoordinatorFullPhase(Player player) throws Exception {
        player.loadData();
        if (player.isOnline() || playerCountIsNonzero())
            throw new IllegalStateException("Coordinator requires detached player and denied admission");
        GrantLedger ledger = activeProductLedger();
        var prior = ledger.historySnapshot();
        if (prior.size() != 1 || !prior.getFirst().delivered()
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Coordinator input is not previous durable grant1");
        var receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (receipt.active() || receipt.completedSequence() != 1)
            throw new IllegalStateException("Coordinator prior PDC not completed seq1");
        var before = readDisk(dataFile());
        if (!SavedGrantReceiptNbt.readAndVerify(before, ledger, ID).equals(receipt))
            throw new IllegalStateException("Coordinator persisted receipt not authentic");
        var originalCoin = player.getInventory().getItem(0);
        var stone = player.getInventory().getItem(9);
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        if (originalCoin == null || originalCoin.getAmount() != 1
                || codec.identify(originalCoin).orElse(null) != CoinDenomination.FIVE_THOUSAND
                || stone == null || stone.getType() != Material.STONE || stone.getAmount() != 2
                || player.getInventory().getItem(1) != null)
            throw new IllegalStateException("Coordinator prior inventory incorrect");
        var grant = ledger.reserveCoins(ID, CoinDenomination.FIVE_THOUSAND, 1,
                "a02-product-coordinator-real-save");
        WriterEpochFence boundedTestOnly = new WriterEpochFence();
        boundedTestOnly.arm(); // Isolated test only. NOT installed full writer closure.
        var manifest = actualPlayerManifest();
        var coordinatorClass = Class.forName(
                "co.surumene.simplecoins.paper.PaperGrantSavedBatchCoordinator",
                true, PaperSaveProbe.class.getClassLoader());
        var entry = coordinatorClass.getDeclaredMethod("saveAndFinalizeFirstBatch",
                Player.class, GrantLedger.class, GrantLedger.Grant.class,
                CoinItemCodec.class, WriterEpochFence.class,
                WorldStorageDiscovery.Manifest.class, java.util.concurrent.Callable.class,
                int.class, int.class);
        entry.setAccessible(true);
        final Object result;
        try {
            result = entry.invoke(null, player, ledger, grant, codec, boundedTestOnly,
                    manifest, (java.util.concurrent.Callable<WorldStorageDiscovery.Manifest>)
                        this::actualPlayerManifest, 16 * 1024 * 1024, 64 * 1024 * 1024);
        } catch (InvocationTargetException failure) {
            Throwable root = failure.getCause();
            if (root instanceof Exception cause) throw cause;
            if (root instanceof Error cause) throw cause;
            throw new IllegalStateException("Unexpected coordinator failure", root);
        }
        if (!(result instanceof java.util.Optional<?> option) || option.isEmpty()
                || !(option.orElseThrow() instanceof SavedGrantDeliveryReadback.Proof proof)
                || proof.receipt().active() || proof.receipt().completedSequence() != 2
                || !java.math.BigInteger.valueOf(5000).equals(proof.matchedMonetaryValue())
                || boundedTestOnly.state() != WriterEpochFence.State.SEALED
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Product coordinator failed to physically save/ACK");
        GrantLedger reopened = new GrantLedger(grantDirectory());
        var saved = readDisk(dataFile());
        var money = SavedPlayerAssets.extract(saved);
        if (reopened.historySnapshot().size() != 2
                || reopened.historySnapshot().stream().anyMatch(g -> !g.delivered())
                || !reopened.pending(ID).isEmpty()
                || !java.math.BigInteger.valueOf(5000).equals(money.get("inventory:0"))
                || !java.math.BigInteger.valueOf(5000).equals(money.get("inventory:1"))
                || !SavedGrantReceiptNbt.readAndVerify(saved, reopened, ID)
                    .equals(proof.receipt())
                || player.getInventory().getItem(9).getAmount() != 2)
            throw new IllegalStateException("Product coordinator ACK disagrees with saved NBT");
        getLogger().info("PAPER_A02_PRODUCT_COORDINATOR_SAVED_ACK_PASSED"
                + " physical=10000 completed=2 grants=2 pending=0 TEST_ONLY_FENCE");
    }

    /** New independent JVM readback on the copied real Paper saved world. */
    private void productCoordinatorAckedReloadPhase(Player player) throws Exception {
        byte[] sourceBefore = Files.readAllBytes(dataFile());
        byte[] journalBefore = Files.readAllBytes(grantDirectory().resolve("2.grant"));
        player.loadData();
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 2 || history.stream().anyMatch(g -> !g.delivered())
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Coordinator durable ACK missing after real restart");
        var receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (receipt.active() || receipt.completedSequence() != 2
                || !SavedGrantReceiptNbt.readAndVerify(readDisk(dataFile()), ledger, ID)
                        .equals(receipt))
            throw new IllegalStateException("Coordinator PDC missing after real restart");
        CoinItemCodec coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        for (int slot : new int[] {0, 1}) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getAmount() != 1
                    || coins.identify(item).orElse(null) != CoinDenomination.FIVE_THOUSAND)
                throw new IllegalStateException("Coordinator physical coin missing after reboot");
        }
        var unrelated = player.getInventory().getItem(9);
        if (unrelated == null || unrelated.getType() != Material.STONE
                || unrelated.getAmount() != 2
                || !java.math.BigInteger.valueOf(10_000).equals(
                        SavedPlayerAssets.extract(readDisk(dataFile())).values().stream()
                            .reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add))
                || !Arrays.equals(sourceBefore, Files.readAllBytes(dataFile()))
                || !Arrays.equals(journalBefore,
                        Files.readAllBytes(grantDirectory().resolve("2.grant"))))
            throw new IllegalStateException("Coordinator restarted money or unrelated bytes changed");
        getLogger().info("PAPER_A02_PRODUCT_COORDINATOR_REBOOT_NO_REISSUE_PASSED"
                + " total=10000 grants=2 pending=0");
    }

    private void verifiedDeliveryPhase(Player player) throws Exception {
        Path file = dataFile();
        if (!Files.isRegularFile(file))
            throw new IllegalStateException("Missing authoritative previous Paper playerdata");
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Real grant verification requires detached player");

        GrantLedger ledger = activeProductLedger();
        var previous = ledger.historySnapshot();
        if (previous.size() != 1 || !previous.getFirst().delivered()
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Earlier saved+acknowledged grant not stable");
        GrantReceiptState originalReceipt =
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (originalReceipt.completedSequence() != previous.getFirst().sequence()
                || originalReceipt.active())
            throw new IllegalStateException("Reloaded receipt does not match first durable grant");
        var coins = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack initialCoin = player.getInventory().getItem(0);
        ItemStack unrelated = player.getInventory().getItem(9);
        if (initialCoin == null || initialCoin.getAmount() != 1
                || coins.identify(initialCoin).orElse(null) != CoinDenomination.FIVE_THOUSAND
                || unrelated == null || unrelated.getType() != Material.STONE
                || unrelated.getAmount() != 2)
            throw new IllegalStateException("First money and unrelated item did not survive prior JVM");

        Map<String, Object> before = PlayerDataReader.read(file, 16 * 1024 * 1024,
                64 * 1024 * 1024);
        GrantLedger.Grant nextGrant = ledger.reserveCoins(ID, CoinDenomination.FIVE_THOUSAND,
                1, "a02-verified-plan");
        if (nextGrant.sequence() != 2)
            throw new IllegalStateException("Second durable grant sequence is not 2");
        var stage = PaperGrantInventorySlotPlan.stage(ledger, ID, nextGrant,
                originalReceipt, player.getInventory().getStorageContents()).orElseThrow(
                        () -> new IllegalStateException("Second coin had no vacant storage slot"));
        if (stage.placements().size() != 1 || stage.placements().getFirst().index() != 1
                || stage.nextReceipt().completedSequence() != nextGrant.sequence())
            throw new IllegalStateException("Unexpected second saved physical grant plan");

        World world = getServer().getWorld("world");
        if (world == null) throw new IllegalStateException("Actual Paper overworld missing");
        Path dimension = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path levelRoot = file.getParent().getParent().getParent().toAbsolutePath().normalize();
        var manifest = WorldStorageDiscovery.discover(
                Map.of(world.getName(), dimension),
                new WorldScope(true, Set.of(), Set.of(world.getName())),
                100000, Set.of(levelRoot));
        if (!file.toAbsolutePath().normalize().equals(manifest.playerDataFiles().get(ID))
                || manifest.playerDataFiles().size() != 1)
            throw new IllegalStateException("Actual shared level playerdata not discovered");

        // Negative control BEFORE writing money+receipt: a valid reservation
        // alone can NEVER constitute an ACK-eligible physical saved grant.
        boolean denied = false;
        try {
            SavedGrantDeliveryFileReadback.verify(manifest, ledger, ID, stage, before,
                    16 * 1024 * 1024, 64 * 1024 * 1024);
        } catch (java.io.IOException notSaved) {
            denied = true;
        }
        if (!denied || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Unsaved grant was eligible for acknowledgement");
        getLogger().info("PAPER_A02_UNSAVED_PLAN_FILE_READBACK_REJECTED_PASSED");

        ItemStack[] candidate = PaperGrantInventoryMaterializer.buildCopy(stage,
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer()),
                player.getInventory().getStorageContents(), coins);
        stage.requireUnchanged(PaperGrantReceiptPdc.read(player.getPersistentDataContainer()),
                player.getInventory().getStorageContents());
        player.getInventory().setStorageContents(candidate);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), stage.nextReceipt());
        player.saveData(); // real Paper PlayerDataStorage, not an in-memory mock

        // Re-discover AFTER the physical save so the proof and witness bind to
        // the latest on-disk generation. Void saveData return is not evidence.
        var afterManifest = WorldStorageDiscovery.discover(
                Map.of(world.getName(), dimension),
                new WorldScope(true, Set.of(), Set.of(world.getName())),
                100000, Set.of(levelRoot));
        var proof = SavedGrantDeliveryFileReadback.verify(afterManifest, ledger, ID,
                stage, before, 16 * 1024 * 1024, 64 * 1024 * 1024);
        if (!proof.receipt().equals(stage.nextReceipt()) || proof.matchedStacks() != 1
                || !proof.matchedMonetaryValue().equals(java.math.BigInteger.valueOf(5000)))
            throw new IllegalStateException("Physical saved proof did not match planned 5000 coin");
        if (ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Second grant ACK happened before verified readback");
        getLogger().info("PAPER_A02_REAL_PLAN_MATERIALIZER_SAVE_FILE_PROOF_PASSED"
                + " amount=" + proof.matchedMonetaryValue()
                + " retainedPriorCoin=true retainedOtherStorage=true"
                + " savedReceipt=" + proof.receipt().completedSequence());

        // Use the actual package-private product saved+ACK implementation
        // instead of a direct low-level acknowledge from a stale Proof.
        var actualProductProof = protectedProductSavedStage(
                "co.surumene.simplecoins.paper.SavedGrantProtectedAckStage",
                "finish", afterManifest, ledger, nextGrant, stage, before);
        if (!actualProductProof.equals(proof)
                || ledger.pending(ID).size() != 0)
            throw new IllegalStateException("Protected real Paper 5000C proof/ACK was inconsistent");
        getLogger().info("PAPER_A02_PRODUCT_PROTECTED_FULL_REAL_SAVE_5000_ACK_PASSED"
                + " TEST_ONLY_NO_INSTALLED_WRITER_CLOSURE");
        GrantLedger fresh = new GrantLedger(grantDirectory());
        if (fresh.historySnapshot().size() != 2 || !fresh.pending(ID).isEmpty()
                || fresh.historySnapshot().stream().anyMatch(g -> !g.delivered()))
            throw new IllegalStateException("Verified readback ACK not durable or duplicated");
        getLogger().info("PAPER_A02_VERIFIED_FILE_PROOF_BEFORE_ACK_PASSED"
                + " durableGrants=2 pending=0");
    }


    /**
     * Fifth REAL Paper JVM: leave exactly two physical storage slots free
     * and save a grant whose canonical physical distribution needs three.
     * Neither partial progress nor a full inventory may ACK that grant.
     */
    private void partialSavePhase(Player player) throws Exception {
        Path file = dataFile();
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Partial grant probe requires disconnected CraftPlayer");
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 2 || !ledger.pending(ID).isEmpty()
                || history.stream().anyMatch(g -> !g.delivered()))
            throw new IllegalStateException("Previous grants not fully ACKed");
        GrantReceiptState receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (receipt.completedSequence() != 2 || receipt.active())
            throw new IllegalStateException("Unexpected initial completed grant receipt");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (i == 3 || i == 4) {
                if (storage[i] != null && !storage[i].getType().isAir())
                    throw new IllegalStateException("Test must preserve two free slots");
                continue;
            }
            if (storage[i] == null || storage[i].getType().isAir())
                storage[i] = new ItemStack(Material.STONE, 1);
        }
        player.getInventory().setStorageContents(storage);
        player.saveData(); // The fixture's inventory-full precondition is saved first.
        Map<String, Object> before = readDisk(file);

        GrantLedger.Grant grant = ledger.reserveCoins(ID, CoinDenomination.FIVE_THOUSAND,
                130, "a02-partial-canonical");
        if (grant.sequence() != 3 || grant.amount() != 650_000)
            throw new IllegalStateException("Unexpected large coin grant");
        var stage = PaperGrantInventorySlotPlan.stage(ledger, ID, grant, receipt,
                player.getInventory().getStorageContents()).orElseThrow();
        if (stage.placements().size() != 2
                || stage.placements().get(0).index() != 3
                || stage.placements().get(1).index() != 4
                || stage.placements().stream().anyMatch(slot -> slot.count() != 64
                        || slot.denomination() != CoinDenomination.FIVE_THOUSAND)
                || stage.nextReceipt().completedSequence() != 2
                || stage.nextReceipt().activeSequence() != 3
                || !grant.id().equals(stage.nextReceipt().activeId())
                || stage.nextReceipt().partialValue() != 640_000)
            throw new IllegalStateException("Incorrect first-batch two-stack plan");

        // Use the actual PRODUCT batch orchestration, not test-owned
        // placement/save/readback steps. Reuse the independently precomputed
        // plan only as an oracle for the resulting authoritative UUID.dat.
        WriterEpochFence firstBatchEpoch = new WriterEpochFence();
        firstBatchEpoch.arm(); // test-only, still NOT installed whole-writer proof
        var paperCoordinator = Class.forName(
                "co.surumene.simplecoins.paper.PaperGrantSavedBatchCoordinator",
                true, PaperSaveProbe.class.getClassLoader());
        var realBatch = paperCoordinator.getDeclaredMethod("saveAndFinalizeFirstBatch",
                Player.class, GrantLedger.class, GrantLedger.Grant.class,
                CoinItemCodec.class, WriterEpochFence.class,
                WorldStorageDiscovery.Manifest.class, java.util.concurrent.Callable.class,
                int.class, int.class);
        realBatch.setAccessible(true);
        final SavedGrantDeliveryReadback.Proof productPartial;
        try {
            Object result = realBatch.invoke(null, player, ledger, grant, codec,
                    firstBatchEpoch, actualPlayerManifest(),
                    (java.util.concurrent.Callable<WorldStorageDiscovery.Manifest>)
                        this::actualPlayerManifest, 16 * 1024 * 1024, 64 * 1024 * 1024);
            if (!(result instanceof java.util.Optional<?> option) || option.isEmpty()
                    || !(option.orElseThrow() instanceof SavedGrantDeliveryReadback.Proof))
                throw new IllegalStateException("Actual Paper first-batch coordinator refused grant");
            productPartial = (SavedGrantDeliveryReadback.Proof) option.orElseThrow();
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception error) throw error;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Unexpected partial product failure", cause);
        }

        var partialManifest = actualPlayerManifest();
        var proof = SavedGrantDeliveryFileReadback.verify(partialManifest, ledger, ID,
                stage, before, 16 * 1024 * 1024, 64 * 1024 * 1024);
        if (!proof.equals(productPartial)
                || !proof.receipt().equals(stage.nextReceipt()) || !proof.receipt().active()
                || proof.matchedStacks() != 2
                || !proof.matchedMonetaryValue()
                        .equals(java.math.BigInteger.valueOf(640_000))
                || firstBatchEpoch.state() != WriterEpochFence.State.SEALED
                || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getLast().delivered())
            throw new IllegalStateException("Product first partial saved batch prematurely ACKed");
        getLogger().info("PAPER_A02_PRODUCT_PROTECTED_PARTIAL_REAL_SAVE_640000_PENDING_PASSED"
                + " TEST_ONLY_NO_INSTALLED_WRITER_CLOSURE");
        getLogger().info("PAPER_A02_PRODUCT_COORDINATOR_FIRST_PARTIAL_REAL_SAVE_640000_PENDING_PASSED");
        if (PaperGrantInventorySlotPlan.stage(ledger, ID, grant,
                stage.nextReceipt(), player.getInventory().getStorageContents()).isPresent())
            throw new IllegalStateException("Full storage permitted a duplicate extra batch");

        boolean deniedAck = false;
        try {
            ledger.acknowledge(grant, proof.receipt().completedSequence());
        } catch (IllegalStateException uncompleted) {
            deniedAck = true;
        }
        if (!deniedAck || ledger.pending(ID).size() != 1
                || ledger.historySnapshot().getLast().delivered())
            throw new IllegalStateException("Partially delivered grant was acknowledged");
        getLogger().info("PAPER_A02_PARTIAL_640000_SAVED_NO_ACK_PASSED"
                + " activeSequence=" + proof.receipt().activeSequence()
                + " completed=" + proof.receipt().completedSequence()
                + " slots=" + proof.matchedStacks());
    }

    /** Sixth independent JVM: resume only the two missing coins after freeing one slot. */
    private void partialResumePhase(Player player) throws Exception {
        Path file = dataFile();
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Partial replay must not have connected players");
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 3 || history.getLast().delivered()
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Pending partial grant lost across JVM restart");
        GrantLedger.Grant grant = history.getLast();
        GrantReceiptState savedReceipt =
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (savedReceipt.completedSequence() != 2
                || savedReceipt.activeSequence() != 3
                || !grant.id().equals(savedReceipt.activeId())
                || savedReceipt.partialValue() != 640_000)
            throw new IllegalStateException("Partial saved cursor did not survive Paper reload");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        for (int index : new int[]{3, 4}) {
            ItemStack issued = player.getInventory().getItem(index);
            if (issued == null || issued.getAmount() != 64
                    || codec.identify(issued).orElse(null) != CoinDenomination.FIVE_THOUSAND)
                throw new IllegalStateException("Saved partial currency stack missing at " + index);
        }
        if (PaperGrantInventorySlotPlan.stage(ledger, ID, grant, savedReceipt,
                player.getInventory().getStorageContents()).isPresent())
            throw new IllegalStateException("Full inventory should block continued delivery");
        if (ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Full inventory consumed pending partial grant");
        getLogger().info("PAPER_A02_PARTIAL_RESTART_FULL_STORAGE_NO_DUPLICATE_PASSED");

        ItemStack filler = player.getInventory().getItem(2);
        if (filler == null || filler.getType() != Material.STONE || filler.getAmount() != 1)
            throw new IllegalStateException("Missing disposable nonmonetary filler");
        player.getInventory().setItem(2, null);
        player.saveData(); // Simulated pre-delivery disposal; saved and independently read.
        Map<String, Object> before = readDisk(file);

        var stage = PaperGrantInventorySlotPlan.stage(ledger, ID, grant, savedReceipt,
                player.getInventory().getStorageContents()).orElseThrow();
        if (stage.placements().size() != 1
                || stage.placements().getFirst().index() != 2
                || stage.placements().getFirst().count() != 2
                || stage.placements().getFirst().denomination()
                        != CoinDenomination.FIVE_THOUSAND
                || stage.nextReceipt().active()
                || stage.nextReceipt().completedSequence() != 3)
            throw new IllegalStateException("Incorrect remaining two-coin delivery plan");

        ItemStack[] candidate = PaperGrantInventoryMaterializer.buildCopy(stage,
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer()),
                player.getInventory().getStorageContents(), codec);
        player.getInventory().setStorageContents(candidate);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), stage.nextReceipt());
        player.saveData();

        var proof = SavedGrantDeliveryFileReadback.verify(actualPlayerManifest(), ledger, ID,
                stage, before, 16 * 1024 * 1024, 64 * 1024 * 1024);
        if (!proof.receipt().equals(stage.nextReceipt()) || proof.receipt().active()
                || proof.matchedStacks() != 1
                || !proof.matchedMonetaryValue()
                        .equals(java.math.BigInteger.valueOf(10_000))
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Final physical stack and receipt not verified");
        getLogger().info("PAPER_A02_PARTIAL_RESTART_FINAL_SAVED_PROOF_PASSED"
                + " newValue=" + proof.matchedMonetaryValue()
                + " completed=" + proof.receipt().completedSequence());

        if (Boolean.getBoolean("a02.probe.awaitSigkill")) {
            // The harness will force-stop this JVM after the verified final
            // physical inventory + completed receipt reached disk. No ACK.
            return;
        }
        var productFinal = protectedProductSavedStage(
                "co.surumene.simplecoins.paper.SavedGrantProtectedAckStage",
                "finishLastPartialBatch", actualPlayerManifest(), ledger, grant, stage, before);
        if (!productFinal.equals(proof) || ledger.pending(ID).size() != 0)
            throw new IllegalStateException("Protected resumed final batch failed actual Paper proof/ACK");
        getLogger().info("PAPER_A02_PRODUCT_PROTECTED_FINAL_PARTIAL_REAL_SAVE_10000_ACK_PASSED"
                + " TEST_ONLY_NO_INSTALLED_WRITER_CLOSURE");
        GrantLedger reopened = new GrantLedger(grantDirectory());
        if (reopened.historySnapshot().size() != 3
                || reopened.historySnapshot().stream().anyMatch(g -> !g.delivered())
                || !reopened.pending(ID).isEmpty())
            throw new IllegalStateException("Final durable ACK was lost or re-opened");
        getLogger().info("PAPER_A02_PARTIAL_TWO_JVM_EXACT_STACK_PREFIX_ACK_PASSED"
                + " delivered=3 pending=0");
    }


    /**
     * Seventh independent real Paper JVM: the final completed playerdata
     * survived SIGKILL, but the authentic issuance journal is still pending.
     * Confirm all 130 coins and existing unrelated possessions BEFORE
     * recovering only the ACK; never grant additional ItemStacks in this phase.
     */
    private void finalAckRecoveryPhase(Player player) throws Exception {
        Path file = dataFile();
        if (!Files.isRegularFile(file))
            throw new IllegalStateException("Completed saved playerdata is missing");
        byte[] playerBeforeAck = Files.readAllBytes(file);
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Final ACK recovery requires detached player");

        GrantLedger ledger = activeProductLedger();
        var grants = ledger.historySnapshot();
        if (grants.size() != 3 || !grants.get(0).delivered()
                || !grants.get(1).delivered() || grants.get(2).delivered()
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Final saved receipt did not leave grant3 pending");
        GrantLedger.Grant third = grants.get(2);
        if (third.sequence() != 3 || third.coinCount() != 130
                || third.denomination() != CoinDenomination.FIVE_THOUSAND)
            throw new IllegalStateException("Wrong authentic partial reservation");

        Map<String, Object> savedRoot = readDisk(file);
        GrantReceiptState diskReceipt = SavedGrantReceiptNbt.readAndVerify(
                savedRoot, ledger, ID);
        GrantReceiptState loadedReceipt =
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (!diskReceipt.equals(loadedReceipt) || diskReceipt.active()
                || diskReceipt.completedSequence() != 3)
            throw new IllegalStateException("Completed receipt not saved coherently");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        int[] moneySlots = {0, 1, 2, 3, 4};
        int[] amounts = {1, 1, 2, 64, 64};
        var savedAssets = SavedPlayerAssets.extract(savedRoot);
        long totalPhysicalValue = 0;
        for (int i = 0; i < moneySlots.length; i++) {
            int slot = moneySlots[i], count = amounts[i];
            ItemStack physical = player.getInventory().getItem(slot);
            if (physical == null || physical.getAmount() != count
                    || codec.identify(physical).orElse(null)
                            != CoinDenomination.FIVE_THOUSAND
                    || !java.math.BigInteger.valueOf(5000L * count).equals(
                            savedAssets.get("inventory:" + slot)))
                throw new IllegalStateException("Missing/different real saved coin in slot " + slot);
            totalPhysicalValue += 5000L * count;
        }
        ItemStack originalStone = player.getInventory().getItem(9);
        if (originalStone == null || originalStone.getType() != Material.STONE
                || originalStone.getAmount() != 2 || totalPhysicalValue != 660_000)
            throw new IllegalStateException("Nonmonetary storage or total money was lost");
        if (!ledger.wasApplied(third, diskReceipt.completedSequence())
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Completed receipt not ACK-recoverable");
        getLogger().info("PAPER_A02_FINAL_SAVED_RECEIPT_CRASH_RECOVERED_WITHOUT_ISSUE_PASSED"
                + " physicalValue=" + totalPhysicalValue
                + " receipt=" + diskReceipt.completedSequence()
                + " pendingBeforeAck=" + ledger.pending(ID).size());

        ledger.acknowledge(third, diskReceipt.completedSequence());
        GrantLedger reopened = new GrantLedger(grantDirectory());
        if (reopened.historySnapshot().size() != 3
                || reopened.historySnapshot().stream().anyMatch(g -> !g.delivered())
                || !reopened.pending(ID).isEmpty()
                || !Arrays.equals(playerBeforeAck, Files.readAllBytes(file)))
            throw new IllegalStateException("ACK-only recovery changed saved inventory or journal state");
        byte[] durableAck = Files.readAllBytes(grantDirectory().resolve("3.grant"));
        ledger.acknowledge(third, diskReceipt.completedSequence());
        if (!Arrays.equals(durableAck, Files.readAllBytes(
                grantDirectory().resolve("3.grant")))
                || !Arrays.equals(playerBeforeAck, Files.readAllBytes(file)))
            throw new IllegalStateException("Repeated ACK rewrote physical money or journal");
        getLogger().info("PAPER_A02_FINAL_SAVED_PRE_ACK_SIGKILL_SEVENTH_BOOT_ACK_ONLY_PASSED"
                + " durableGrants=3 pending=0 originalMoneyPreserved=true");
    }



    /**
     * Alternate independent Paper boot on a COPY of the fifth-JVM saved
     * partial-state: verify that protected final ACK persisted across a real
     * restart without ever adding a second physical coin batch.
     */
    private void protectedFinalAckedReloadPhase(Player player) throws Exception {
        Path file = dataFile();
        byte[] original = Files.readAllBytes(file);
        player.loadData();
        if (player.isOnline() || playerCountIsNonzero())
            throw new IllegalStateException("Protected final ACK recovery requires detached player");
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 3 || history.stream().anyMatch(g -> !g.delivered())
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Protected final durable ACK missing on actual Paper reboot");
        GrantReceiptState saved = SavedGrantReceiptNbt.readAndVerify(readDisk(file), ledger, ID);
        if (saved.active() || saved.completedSequence() != 3
                || !saved.equals(PaperGrantReceiptPdc.read(player.getPersistentDataContainer())))
            throw new IllegalStateException("Protected final saved receipt mismatched on restart");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        int[] indices = {0, 1, 2, 3, 4};
        int[] counts = {1, 1, 2, 64, 64};
        long savedTotal = 0;
        var assets = SavedPlayerAssets.extract(readDisk(file));
        for (int i = 0; i < indices.length; i++) {
            ItemStack item = player.getInventory().getItem(indices[i]);
            if (item == null || item.getAmount() != counts[i]
                    || codec.identify(item).orElse(null) != CoinDenomination.FIVE_THOUSAND
                    || !java.math.BigInteger.valueOf(5000L * counts[i])
                            .equals(assets.get("inventory:" + indices[i])))
                throw new IllegalStateException("Protected final saved money missing at " + indices[i]);
            savedTotal += 5000L * counts[i];
        }
        if (savedTotal != 660000
                || player.getInventory().getItem(9) == null
                || player.getInventory().getItem(9).getType() != Material.STONE
                || player.getInventory().getItem(9).getAmount() != 2
                || !Arrays.equals(original, Files.readAllBytes(file)))
            throw new IllegalStateException("Protected final ACK recovery lost saved unrelated items");
        getLogger().info("PAPER_A02_PRODUCT_FINAL_PARTIAL_REBOOT_ACKED_NO_REISSUE_PASSED"
                + " total=660000 completed=3 pending=0 durableGrants=3");
    }

    /**
     * Eighth actual Paper JVM: reserve the fourth grant and mutate a detached
     * player's memory, but intentionally DO NOT save the new coin or receipt.
     * A separate CI process then sends SIGKILL before normal shutdown.
     */
    private void beforeSaveCrashPhase(Player player) throws Exception {
        Path file = dataFile();
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Pre-save SIGKILL requires a detached player");

        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 3 || history.stream().anyMatch(g -> !g.delivered())
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Previous three grants must be durable and ACKed");
        GrantReceiptState previousReceipt =
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (previousReceipt.completedSequence() != 3 || previousReceipt.active())
            throw new IllegalStateException("Previous receipt is not fully completed");

        // Previous partial-batch test left all 36 storage slots occupied. Make
        // an authentic, persisted vacancy BEFORE the new reservation/crash window.
        ItemStack filler = player.getInventory().getItem(5);
        if (filler == null || filler.getType() != Material.STONE || filler.getAmount() != 1)
            throw new IllegalStateException("Expected existing nonmonetary filler in slot 5");
        player.getInventory().setItem(5, null);
        player.saveData();
        byte[] savedBeforeStage = Files.readAllBytes(file);
        Map<String, Object> before = readDisk(file);
        if (!SavedGrantReceiptNbt.readAndVerify(before, ledger, ID).equals(previousReceipt)
                || SavedPlayerAssets.extract(before).containsKey("inventory:5"))
            throw new IllegalStateException("Persisted vacancy setup is not authoritative");

        GrantLedger.Grant pending = ledger.reserveCoins(ID,
                CoinDenomination.FIVE_THOUSAND, 1, "a02-presave-sigkill");
        if (pending.sequence() != 4 || pending.amount() != 5000
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Wrong fourth durable reservation");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        var staged = PaperGrantInventorySlotPlan.stage(ledger, ID, pending,
                previousReceipt, player.getInventory().getStorageContents()).orElseThrow();
        if (staged.placements().size() != 1
                || staged.placements().getFirst().index() != 5
                || staged.nextReceipt().active()
                || staged.nextReceipt().completedSequence() != 4)
            throw new IllegalStateException("Unexpected fourth grant pre-save plan");

        ItemStack[] candidate = PaperGrantInventoryMaterializer.buildCopy(staged,
                previousReceipt, player.getInventory().getStorageContents(), codec);
        player.getInventory().setStorageContents(candidate);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), staged.nextReceipt());
        if (player.getInventory().getItem(5) == null
                || PaperGrantReceiptPdc.read(player.getPersistentDataContainer())
                        .completedSequence() != 4)
            throw new IllegalStateException("Detached in-memory candidate was not applied");
        // No saveData() is allowed after the preceding memory mutation.
        if (!Arrays.equals(savedBeforeStage, Files.readAllBytes(file))
                || !SavedGrantReceiptNbt.readAndVerify(readDisk(file), ledger, ID)
                        .equals(previousReceipt)
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Unsaved memory became durable or ACKed");

        boolean denied = false;
        try {
            SavedGrantDeliveryFileReadback.verify(actualPlayerManifest(), ledger, ID,
                    staged, before, 16 * 1024 * 1024, 64 * 1024 * 1024);
        } catch (java.io.IOException expectedUnsaved) {
            denied = true;
        }
        if (!denied || !Arrays.equals(savedBeforeStage, Files.readAllBytes(file)))
            throw new IllegalStateException("Unsaved candidate passed authoritative proof");
        getLogger().info("PAPER_A02_PRE_SAVE_ABORT_UNPERSISTED_COIN_RECEIPT_PASSED"
                + " pending=1 savedReceipt=3 stagedReceipt=4");
    }

    /** Ninth Paper JVM: aborted in-memory money must not survive the crash. */
    private void recoverUnsavedPhase(Player player) throws Exception {
        Path file = dataFile();
        player.loadData();
        if (player.isOnline() || !getServer().getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Uncommitted grant recovery requires a detached player");
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 4 || history.subList(0, 3).stream().anyMatch(g -> !g.delivered())
                || history.getLast().delivered() || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("Crash lost reservation or acknowledged it prematurely");
        GrantLedger.Grant grant = history.getLast();
        if (grant.sequence() != 4 || grant.denomination() != CoinDenomination.FIVE_THOUSAND
                || grant.coinCount() != 1)
            throw new IllegalStateException("Pending reservation identity changed");
        GrantReceiptState priorReceipt =
                PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        Map<String, Object> before = readDisk(file);
        if (priorReceipt.completedSequence() != 3 || priorReceipt.active()
                || !SavedGrantReceiptNbt.readAndVerify(before, ledger, ID).equals(priorReceipt)
                || player.getInventory().getItem(5) != null
                || SavedPlayerAssets.extract(before).containsKey("inventory:5"))
            throw new IllegalStateException("Aborted in-memory grant survived as a phantom coin");

        java.math.BigInteger physicalBefore = SavedPlayerAssets.extract(before).values()
                .stream().reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
        if (!physicalBefore.equals(java.math.BigInteger.valueOf(660_000)))
            throw new IllegalStateException("Previously saved 660000 C disappeared or duplicated");
        ItemStack unrelated = player.getInventory().getItem(9);
        if (unrelated == null || unrelated.getType() != Material.STONE
                || unrelated.getAmount() != 2)
            throw new IllegalStateException("Unrelated saved storage changed after crash");
        getLogger().info("PAPER_A02_PRESAVE_CRASH_NO_PHANTOM_MONEY_PASSED"
                + " oldValue=660000 savedReceipt=3 pending=1");

        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        var staged = PaperGrantInventorySlotPlan.stage(ledger, ID, grant, priorReceipt,
                player.getInventory().getStorageContents()).orElseThrow();
        if (staged.placements().size() != 1 || staged.placements().getFirst().index() != 5
                || staged.nextReceipt().active()
                || staged.nextReceipt().completedSequence() != 4)
            throw new IllegalStateException("Recovered plan tried to issue an incorrect batch");
        ItemStack[] candidate = PaperGrantInventoryMaterializer.buildCopy(staged,
                priorReceipt, player.getInventory().getStorageContents(), codec);
        player.getInventory().setStorageContents(candidate);
        PaperGrantReceiptPdc.write(player.getPersistentDataContainer(), staged.nextReceipt());
        player.saveData();
        var proof = SavedGrantDeliveryFileReadback.verify(actualPlayerManifest(), ledger, ID,
                staged, before, 16 * 1024 * 1024, 64 * 1024 * 1024);
        if (proof.matchedStacks() != 1
                || !proof.matchedMonetaryValue().equals(java.math.BigInteger.valueOf(5000))
                || proof.receipt().active() || proof.receipt().completedSequence() != 4
                || ledger.pending(ID).size() != 1)
            throw new IllegalStateException("New physical coin + receipt were not saved together");
        java.math.BigInteger physicalAfter = SavedPlayerAssets.extract(readDisk(file)).values()
                .stream().reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
        if (!physicalAfter.equals(java.math.BigInteger.valueOf(665_000)))
            throw new IllegalStateException("Saved recovery gained/lost unplanned money");

        ledger.acknowledge(grant, proof.receipt().completedSequence());
        GrantLedger reopened = new GrantLedger(grantDirectory());
        if (reopened.historySnapshot().size() != 4
                || reopened.historySnapshot().stream().anyMatch(g -> !g.delivered())
                || !reopened.pending(ID).isEmpty())
            throw new IllegalStateException("Recovered grant ACK is not durable");
        getLogger().info("PAPER_A02_PRESAVE_RECOVERY_SAVED_PROOF_ACK_PASSED"
                + " total=665000 pending=0");
    }

    /** Tenth independent Paper boot: verify ACK idempotency and physical stability. */
    private void unsavedAckedReloadPhase(Player player) throws Exception {
        Path file = dataFile();
        byte[] savedBefore = Files.readAllBytes(file);
        byte[] journalBefore = Files.readAllBytes(grantDirectory().resolve("4.grant"));
        player.loadData();
        GrantLedger ledger = activeProductLedger();
        var history = ledger.historySnapshot();
        if (history.size() != 4 || history.stream().anyMatch(g -> !g.delivered())
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Tenth boot journal re-opened a delivered grant");
        GrantReceiptState receipt = PaperGrantReceiptPdc.read(player.getPersistentDataContainer());
        if (receipt.active() || receipt.completedSequence() != 4
                || !SavedGrantReceiptNbt.readAndVerify(readDisk(file), ledger, ID).equals(receipt))
            throw new IllegalStateException("Tenth boot physical receipt lost");
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack fourth = player.getInventory().getItem(5);
        if (fourth == null || fourth.getAmount() != 1
                || codec.identify(fourth).orElse(null) != CoinDenomination.FIVE_THOUSAND)
            throw new IllegalStateException("Fourth saved physical coin absent after reboot");
        ItemStack unrelated = player.getInventory().getItem(9);
        if (unrelated == null || unrelated.getType() != Material.STONE
                || unrelated.getAmount() != 2)
            throw new IllegalStateException("Tenth boot lost unrelated storage");
        java.math.BigInteger total = SavedPlayerAssets.extract(readDisk(file)).values()
                .stream().reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
        if (!total.equals(java.math.BigInteger.valueOf(665_000)))
            throw new IllegalStateException("Tenth boot gained or lost currency");
        ledger.acknowledge(history.getLast(), receipt.completedSequence());
        if (!Arrays.equals(savedBefore, Files.readAllBytes(file))
                || !Arrays.equals(journalBefore, Files.readAllBytes(
                        grantDirectory().resolve("4.grant")))
                || !ledger.pending(ID).isEmpty())
            throw new IllegalStateException("Idempotent ACK rewrote playerdata or grant journal");
        getLogger().info("PAPER_A02_PRESAVE_TENTH_BOOT_ACK_IDEMPOTENT_PASSED"
                + " total=665000 durableGrants=4 pending=0");
    }

    /** Only Paper's genuine shared level saved UUID file can supply the proof. */
    private WorldStorageDiscovery.Manifest actualPlayerManifest() throws Exception {
        World world = getServer().getWorld("world");
        if (world == null) throw new IllegalStateException("Real Paper world missing");
        Path file = dataFile().toAbsolutePath().normalize();
        Path dimension = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path levelRoot = file.getParent().getParent().getParent();
        var manifest = WorldStorageDiscovery.discover(
                Map.of(world.getName(), dimension),
                new WorldScope(true, Set.of(), Set.of(world.getName())),
                100000, Set.of(levelRoot));
        if (!file.equals(manifest.playerDataFiles().get(ID))
                || manifest.playerDataFiles().size() != 1)
            throw new IllegalStateException("Real shared Paper playerdata not discovered");
        return manifest;
    }
}
