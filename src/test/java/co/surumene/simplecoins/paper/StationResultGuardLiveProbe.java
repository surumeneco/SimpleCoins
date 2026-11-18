package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Disposable test-only event dispatch. The observed Paper listener is the
 * product registration, NOT a test-installed cancellation listener.
 * Deliberately synthetic view/click: never claim a socket, authorized player
 * or physical NMS result consumption was tested by this fixture.
 */
public final class StationResultGuardLiveProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("A12_PRODUCT_STATION_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                runProbe();
                getLogger().info("A12_PRODUCT_STATION_GUARD_PASSED"
                        + " dispatch=realPaperEventBus syntheticView=true"
                        + " socket=false connectedPlayer=false");
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "A12_PRODUCT_STATION_GUARD_FAILED", error);
            } finally {
                Bukkit.shutdown();
            }
        }, 35L);
    }

    private void runProbe() throws Exception {
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        Player viewer = detachedPlayer();
        if (viewer.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Unexpected online player");
        ItemStack coin = codec.create(CoinDenomination.FIVE_THOUSAND, 1);
        ItemStack malformed = coin.clone();
        var meta = malformed.getItemMeta();
        meta.getPersistentDataContainer().set(
                CoinItemCodec.DENOMINATION_KEY, PersistentDataType.STRING, "invalid");
        malformed.setItemMeta(meta);
        if (!codec.hasCoinMarker(malformed))
            throw new IllegalStateException("Malformed marker lost");
        int passed = 0;
        for (InventoryType type : new InventoryType[]{
                InventoryType.MERCHANT, InventoryType.CARTOGRAPHY}) {
            verify(viewer, type, null, null, 2, false, "empty");
            verify(viewer, type, new ItemStack(Material.EMERALD, 1),
                    new ItemStack(Material.PAPER, 1), 2, false, "ordinaryPaper");
            passed += 2;
            for (int slot : new int[]{0, 1}) {
                verify(viewer, type, slot == 0 ? coin : null,
                        slot == 1 ? coin : null, 2, true, "coinSlot" + slot);
                verify(viewer, type, slot == 0 ? malformed : null,
                        slot == 1 ? malformed : null, 2, true, "malformedSlot" + slot);
                passed += 2;
            }
            verify(viewer, type, coin, null, 0, false, "inputMove");
            verify(viewer, type, coin, null, 3, false, "lowerMove");
            passed += 2;
        }
        if (passed != 16)
            throw new IllegalStateException("Incomplete station verification");
        getLogger().info("A12_PRODUCT_STATION_EVENT_CASES=" + passed);
    }

    private static void verify(Player viewer, InventoryType type, ItemStack first,
                               ItemStack second, int raw, boolean deny, String label) {
        // A controlled three-slot proxy allows checking the production
        // handler's result-vs-input discrimination for both window types.
        Inventory top = Bukkit.createInventory(null, InventoryType.CARTOGRAPHY);
        if (first != null) top.setItem(0, first.clone());
        if (second != null) top.setItem(1, second.clone());
        InventoryView view = (InventoryView) Proxy.newProxyInstance(
                InventoryView.class.getClassLoader(), new Class<?>[]{InventoryView.class},
                (p, method, args) -> switch (method.getName()) {
                    case "getType" -> type;
                    case "getTopInventory" -> top;
                    case "getBottomInventory" -> viewer.getInventory();
                    case "getPlayer" -> viewer;
                    case "getItem" -> ((Integer) args[0]) < 3
                            ? top.getItem((Integer) args[0])
                            : viewer.getInventory().getItem(((Integer) args[0]) - 3);
                    case "getSlotType" -> (Integer) args[0] == 2
                            ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CONTAINER;
                    case "getTitle", "getOriginalTitle" -> "A12 product guard probe";
                    case "title" -> Component.text("A12 product guard probe");
                    case "equals" -> p == args[0];
                    case "hashCode" -> System.identityHashCode(p);
                    case "toString" -> "A12Synthetic" + type + "View";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        InventoryClickEvent event = new InventoryClickEvent(view,
                raw == 2 ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CONTAINER,
                raw, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() != deny)
            throw new IllegalStateException(type + " " + label
                    + " expected cancelled=" + deny + " actual=" + event.isCancelled());
    }

    private static Player detachedPlayer() throws Exception {
        Object server = Bukkit.getServer().getClass().getMethod("getServer")
                .invoke(Bukkit.getServer());
        Class<?> mc = Class.forName("net.minecraft.server.MinecraftServer");
        Object world = mc.getMethod("overworld").invoke(server);
        Class<?> level = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profile = Class.forName("com.mojang.authlib.GameProfile");
        Object gameProfile = profile.getConstructor(UUID.class, String.class)
                .newInstance(UUID.fromString("00000000-0000-0000-0000-00000000a129"),
                        "A12ProductProbe");
        Class<?> info = Class.forName("net.minecraft.server.level.ClientInformation");
        Object settings = info.getMethod("createDefault").invoke(null);
        Class<?> player = Class.forName("net.minecraft.server.level.ServerPlayer");
        Constructor<?> constructor = player.getConstructor(mc, level, profile, info);
        Object entity = constructor.newInstance(server, world, gameProfile, settings);
        return (Player) player.getMethod("getBukkitEntity").invoke(entity);
    }
}
