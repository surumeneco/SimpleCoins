package co.surumene.simplecoins.a02probe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import co.surumene.simplecoins.domain.GrantReceiptState;
import co.surumene.simplecoins.domain.PlayerDataReader;
import co.surumene.simplecoins.paper.CoinItemCodec;
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
            try {
                String phase = System.getProperty("a02.probe.phase", "save");
                Player player = createDetachedPlayer();
                if ("save".equals(phase)) savePhase(player);
                else if ("reload".equals(phase)) reloadPhase(player);
                else throw new IllegalArgumentException("Unknown probe phase " + phase);
            } catch (Throwable failure) {
                getLogger().log(Level.SEVERE, "PAPER_A02_PROBE_FAILED", failure);
                Bukkit.shutdown();
                return;
            }
            Bukkit.shutdown();
        }, 30L);
    }

    private Path dataFile() {
        return getServer().getWorldContainer().toPath().toAbsolutePath().normalize()
                .resolve("world/players/data/" + ID + ".dat");
    }

    private Path grantDirectory() {
        return getServer().getWorldContainer().toPath().toAbsolutePath().normalize()
                .resolve("a02-test-grant-ledger");
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
        for (Object raw : inventory) {
            if (raw instanceof Map<?, ?> item
                    && "minecraft:paper".equals(item.get("id"))
                    && item.get("count") instanceof Number count
                    && count.intValue() == PAPER_COUNT) {
                matches = true;
                break;
            }
        }
        if (!matches)
            throw new IllegalStateException("Saved paper stack not found: " + inventory);
    }

    private void savePhase(Player player) throws Exception {
        Path file = dataFile();
        if (Files.exists(file))
            throw new IllegalStateException("Probe requires fresh UUID playerdata");
        GrantLedger ledger = new GrantLedger(grantDirectory());
        GrantLedger.Grant grant = ledger.reserve(ID, ISSUED_VALUE, "a02-ci");
        if (grant.sequence() != 1)
            throw new IllegalStateException("Expected first reserved grant");
        byte[] receipt = expectedReceipt(grant);
        ItemStack coin = new CoinItemCodec(CoinItemCodec.defaultNames())
                .create(CoinDenomination.fromValue((int) ISSUED_VALUE), PAPER_COUNT);
        player.getInventory().setItem(0, coin);
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
        GrantLedger ledger = new GrantLedger(grantDirectory());
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
}
