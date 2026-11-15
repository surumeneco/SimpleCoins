package co.surumene.simplecoins.a02probe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test-only, isolated Paper 26.2 proof. No networking, real player, production
 * grant, world data or admission flag is ever modified. A synthetic NMS player
 * exercises CraftPlayer.saveData/loadData against the real Paper playerIo.
 * This is intentionally NOT a proof of OS-level crash durability or G3.
 */
public final class PaperSaveProbe extends JavaPlugin {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000a002");
    private static final NamespacedKey RECEIPT =
            new NamespacedKey("simplecoins", "grant_receipt_v1");
    private static final int PAPER_COUNT = 3;

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

    private byte[] expectedReceipt() throws ReflectiveOperationException {
        ClassLoader owner = getServer().getPluginManager().getPlugin("SimpleCoins")
                .getClass().getClassLoader();
        Class<?> receiptType = Class.forName(
                "co.surumene.simplecoins.domain.GrantReceiptState", true, owner);
        Class<?> codec = Class.forName(
                "co.surumene.simplecoins.paper.PaperGrantReceiptPdc", true, owner);
        Object empty = receiptType.getMethod("empty").invoke(null);
        return (byte[]) codec.getMethod("toSavedBytes", receiptType).invoke(null, empty);
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

    /** Call the production read-only compressed NBT decoder, never fake NBT. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readDisk(Path file) throws ReflectiveOperationException {
        ClassLoader owner = getServer().getPluginManager().getPlugin("SimpleCoins")
                .getClass().getClassLoader();
        Class<?> reader = Class.forName(
                "co.surumene.simplecoins.domain.PlayerDataReader", true, owner);
        Object result = reader.getMethod("read", Path.class, int.class, int.class)
                .invoke(null, file, 1024 * 1024, 16 * 1024 * 1024);
        return (Map<String, Object>) result;
    }

    private void assertSavedPair(Path file, byte[] expected) throws Exception {
        if (!Files.isRegularFile(file))
            throw new IllegalStateException("Real Paper playerdata file missing: " + file);
        Map<String, Object> root = readDisk(file);
        Object rawValues = root.get("BukkitValues");
        if (!(rawValues instanceof Map<?, ?> values))
            throw new IllegalStateException("Missing BukkitValues: " + root.keySet());
        Object savedReceipt = values.get(RECEIPT.toString());
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
        byte[] receipt = expectedReceipt();
        player.getInventory().setItem(0, new ItemStack(Material.PAPER, PAPER_COUNT));
        player.getPersistentDataContainer().set(RECEIPT, PersistentDataType.BYTE_ARRAY, receipt);

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
        getLogger().info("PAPER_A02_REAL_SAVE_INVENTORY_RECEIPT_PAIR_PASSED");

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
    }

    private void reloadPhase(Player player) throws Exception {
        Path file = dataFile();
        byte[] receipt = expectedReceipt();
        assertSavedPair(file, receipt);
        player.loadData(); // real CraftPlayer -> PlayerDataStorage.load
        ItemStack item = player.getInventory().getItem(0);
        if (item == null || item.getType() != Material.PAPER
                || item.getAmount() != PAPER_COUNT)
            throw new IllegalStateException("Saved inventory was not restored");
        byte[] pdc = player.getPersistentDataContainer().get(
                RECEIPT, PersistentDataType.BYTE_ARRAY);
        if (!Arrays.equals(receipt, pdc))
            throw new IllegalStateException("Saved receipt PDC was not restored");
        getLogger().info("PAPER_A02_RESTART_REAL_LOAD_PAIR_PASSED");
    }
}
