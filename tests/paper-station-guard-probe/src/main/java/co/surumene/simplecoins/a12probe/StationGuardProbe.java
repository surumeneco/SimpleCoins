package co.surumene.simplecoins.a12probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import io.papermc.paper.event.player.CartographyItemEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CI-only actual Paper 26.2 event-bus characterization.
 *
 * The CartographyItemEvent is explicitly constructed with a proxy InventoryView:
 * no network click or actual currency consumption is claimed.
 * MerchantRecipe registration is a separate inventory API observation, NOT a
 * trading GUI transaction or proof that a tagged PAPER item is accepted.
 */
public final class StationGuardProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!"127.0.0.1".equals(Bukkit.getServer().getIp())
                || !Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().severe("A12_STATION_INVALID_ISOLATION");
            Bukkit.shutdown();
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                runProbe();
                getLogger().info("A12_STATION_DISPOSABLE_PROBE_COMPLETE");
            } catch (Throwable error) {
                getLogger().log(Level.SEVERE, "A12_STATION_PROBE_FAILED", error);
            } finally {
                Bukkit.shutdown();
            }
        }, 50L);
    }

    private void runProbe() throws Exception {
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack money = codec.create(CoinDenomination.FIVE_THOUSAND, 1);
        if (codec.identify(money).orElseThrow() != CoinDenomination.FIVE_THOUSAND)
            throw new IllegalStateException("Currency identity lost before probe");

        // The actual Paper inventory is real, while the view and click are
        // deliberately synthetic so no player is admitted or paid.
        Inventory cartography = Bukkit.createInventory(null, InventoryType.CARTOGRAPHY);
        if (cartography.getType() != InventoryType.CARTOGRAPHY || cartography.getSize() != 3)
            throw new IllegalStateException("Paper did not create a cartography inventory");
        Player detached = detachedPlayer();
        if (detached.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
            throw new IllegalStateException("Probe player unexpectedly connected");

        AtomicInteger delivered = new AtomicInteger();
        Listener monitor = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observe(CartographyItemEvent event) {
                if (event.getView().getTopInventory() == cartography)
                    delivered.incrementAndGet();
            }
        };
        Bukkit.getPluginManager().registerEvents(monitor, this);
        try {
            for (int coinSlot : new int[]{0, 1}) {
                cartography.clear();
                cartography.setItem(coinSlot, money.clone());
                cartography.setItem(1 - coinSlot, new ItemStack(Material.MAP, 1));
                cartography.setItem(2, new ItemStack(Material.FILLED_MAP, 1));
                if (!codec.identify(cartography.getItem(coinSlot)).isPresent())
                    throw new IllegalStateException("Actual Paper inventory lost coin marker");
                InventoryView view = viewFor(cartography, detached);
                CartographyItemEvent attempt = new CartographyItemEvent(
                        view, InventoryType.SlotType.RESULT, 2,
                        ClickType.LEFT, InventoryAction.PICKUP_ALL);
                Bukkit.getPluginManager().callEvent(attempt);
                if (attempt.isCancelled())
                    throw new IllegalStateException(
                            "Product or other plugin already rejects this synthetic event");
                if (codec.identify(cartography.getItem(coinSlot)).isEmpty())
                    throw new IllegalStateException("Probe dispatch consumed test money");
                getLogger().info("A12_CARTOGRAPHY_SYNTHETIC_RESULT_CLICK_UNCANCELLED"
                        + " inputSlot=" + coinSlot
                        + " resultSlot=2"
                        + " realPaperInventory=true syntheticView=true"
                        + " physicalConsumption=false");
            }
            if (delivered.get() != 2)
                throw new IllegalStateException("CartographyItemEvent not delivered twice");
        } finally {
            HandlerList.unregisterAll(monitor);
            cartography.clear();
        }

        // Merchant inventories require a trader/viewer; test only the real
        // MerchantRecipe object and registration, not input matching or
        // physical consumption.
        Merchant merchant = Bukkit.createMerchant(Component.text("A12ProbeMerchant"));
        MerchantRecipe paperTrade = new MerchantRecipe(
                new ItemStack(Material.EMERALD, 1), 10);
        paperTrade.addIngredient(new ItemStack(Material.PAPER, 1));
        merchant.setRecipes(List.of(paperTrade));
        List<MerchantRecipe> registered = merchant.getRecipes();
        if (registered.size() != 1 || registered.getFirst().getIngredients().size() != 1
                || registered.getFirst().getIngredients().getFirst().getType() != Material.PAPER)
            throw new IllegalStateException("Real Paper merchant recipe registry changed");
        getLogger().info("A12_MERCHANT_PAPER_INGREDIENT_REGISTERED"
                + " tradePerformed=false"
                + " taggedCoinAccepted=unverified"
                + " physicalConsumption=false");
        verifyNativeMerchantOffer(registered.getFirst(), codec, money);
    }

    /**
     * Directly interrogate Paper's real NMS merchant matcher and its take
     * primitive. NMS ItemStack inputs are private copies, not a player trade.
     * Reflection is intentional: this disposable plugin compiles against the
     * supported Paper API, while the runtime provides the native implementation.
     */
    private void verifyNativeMerchantOffer(MerchantRecipe trade, CoinItemCodec codec,
                                            ItemStack sourceCoin) throws ReflectiveOperationException {
        Class<?> craftRecipeType =
                Class.forName("org.bukkit.craftbukkit.inventory.CraftMerchantRecipe");
        Object craftRecipe = craftRecipeType
                .getMethod("fromBukkit", MerchantRecipe.class).invoke(null, trade);
        Object offer = craftRecipeType.getMethod("toMinecraft").invoke(craftRecipe);

        Class<?> craftStackType =
                Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
        Class<?> nmsStackType = Class.forName("net.minecraft.world.item.ItemStack");
        var toNms = craftStackType.getMethod("asNMSCopy", ItemStack.class);
        var toBukkit = craftStackType.getMethod("asBukkitCopy", nmsStackType);
        var match = offer.getClass()
                .getMethod("satisfiedBy", nmsStackType, nmsStackType);
        var take = offer.getClass().getMethod("take", nmsStackType, nmsStackType);
        var count = nmsStackType.getMethod("getCount");
        Object empty = nmsStackType.getField("EMPTY").get(null);

        Object paperInput = toNms.invoke(null, new ItemStack(Material.PAPER, 1));
        Object stoneInput = toNms.invoke(null, new ItemStack(Material.STONE, 1));
        Object taggedInput = toNms.invoke(null, sourceCoin.clone());
        ItemStack rehydrated = (ItemStack) toBukkit.invoke(null, taggedInput);
        if (codec.identify(rehydrated).orElseThrow()
                != CoinDenomination.FIVE_THOUSAND)
            throw new IllegalStateException("NMS conversion lost the PDC/model identity");

        boolean paperAccepted = (boolean) match.invoke(offer, paperInput, empty);
        boolean stoneAccepted = (boolean) match.invoke(offer, stoneInput, empty);
        boolean taggedAccepted = (boolean) match.invoke(offer, taggedInput, empty);
        if (!paperAccepted || stoneAccepted)
            throw new IllegalStateException(
                    "Native merchant cost controls disagree with PAPER/STONE");

        boolean paperTaken = (boolean) take.invoke(offer, paperInput, empty);
        if (!paperTaken || ((Number) count.invoke(paperInput)).intValue() != 0)
            throw new IllegalStateException("Native ordinary PAPER cost did not consume one");
        getLogger().info("A12_NMS_MERCHANT_PAPER_TAKE_CONTROL_ACCEPTED");

        // Only the detached NMS copy changes. If actual Minecraft accepts
        // tagged PAPER, observe physical primitive consumption instead of
        // assuming a gameplay trade has happened.
        boolean taggedTaken = (boolean) take.invoke(offer, taggedInput, empty);
        int remaining = ((Number) count.invoke(taggedInput)).intValue();
        if (taggedTaken != taggedAccepted || remaining != (taggedAccepted ? 0 : 1))
            throw new IllegalStateException("Native tagged cost match/take disagreed");
        if (codec.identify(sourceCoin).orElseThrow()
                != CoinDenomination.FIVE_THOUSAND || sourceCoin.getAmount() != 1)
            throw new IllegalStateException("NMS matching altered the actual source coin");

        getLogger().info("A12_NMS_MERCHANT_PAPER_COST_MATCH"
                + " plain=true stone=false taggedCoin=" + taggedAccepted
                + " itemModelAndPdcPreserved=true");
        getLogger().info("A12_NMS_MERCHANT_TAGGED_COIN_TAKE"
                + " accepted=" + taggedAccepted
                + " consumed=" + taggedTaken
                + " detachedOnly=true tradePerformed=false");
    }

    /** A proxy view is sufficient for a deliberately synthetic event. */
    private static InventoryView viewFor(Inventory top, Player player) {
        return (InventoryView) Proxy.newProxyInstance(
                InventoryView.class.getClassLoader(),
                new Class<?>[]{InventoryView.class},
                (proxy, method, args) -> {
                    String n = method.getName();
                    return switch (n) {
                        case "getTopInventory" -> top;
                        case "getBottomInventory" -> player.getInventory();
                        case "getPlayer" -> player;
                        case "getType" -> InventoryType.CARTOGRAPHY;
                        case "getItem" -> {
                            int raw = (Integer) args[0];
                            yield raw < top.getSize() ? top.getItem(raw)
                                    : player.getInventory().getItem(raw - top.getSize());
                        }
                        case "getSlotType" -> ((Integer) args[0]) == 2
                                ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CRAFTING;
                        case "countSlots" -> top.getSize() + player.getInventory().getSize();
                        case "convertSlot" -> args[0];
                        case "getTitle", "getOriginalTitle" -> "A12 Probe";
                        case "title" -> Component.text("A12 Probe");
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "A12SyntheticCartographyView";
                        default -> throw new UnsupportedOperationException(
                                "Unsupported synthetic InventoryView call: " + n);
                    };
                });
    }

    /** Detached synthetic player, no login, socket, inventory grant or admission. */
    private static Player detachedPlayer() throws ReflectiveOperationException {
        Object craftServer = Bukkit.getServer();
        Object minecraft = craftServer.getClass().getMethod("getServer").invoke(craftServer);
        Class<?> mcType = Class.forName("net.minecraft.server.MinecraftServer");
        Object world = mcType.getMethod("overworld").invoke(minecraft);
        Class<?> levelType = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class)
                .newInstance(UUID.fromString("00000000-0000-0000-0000-00000000a122"),
                        "A12StationProbe");
        Class<?> infoType = Class.forName("net.minecraft.server.level.ClientInformation");
        Object info = infoType.getMethod("createDefault").invoke(null);
        Class<?> playerType = Class.forName("net.minecraft.server.level.ServerPlayer");
        Constructor<?> ctor = playerType.getConstructor(
                mcType, levelType, profileType, infoType);
        Object serverPlayer = ctor.newInstance(minecraft, world, profile, info);
        Object bukkitPlayer = playerType.getMethod("getBukkitEntity").invoke(serverPlayer);
        if (!(bukkitPlayer instanceof Player player))
            throw new IllegalStateException("Cannot obtain Paper detached player");
        return player;
    }
}
