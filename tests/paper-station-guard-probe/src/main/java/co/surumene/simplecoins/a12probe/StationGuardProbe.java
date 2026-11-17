package co.surumene.simplecoins.a12probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.paper.CoinItemCodec;
import io.papermc.paper.event.player.CartographyItemEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.map.MapView;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        characterizeMerchantMenuAbi();
        verifyNativeCartographyResult(codec, money, detached);
        characterizePacketClickEntryPoints();
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

    /** Inspect the exact Paper build's merchant GUI and offer state APIs. */
    private void characterizeMerchantMenuAbi() throws ReflectiveOperationException {
        String[] types = {
                "net.minecraft.world.inventory.MerchantMenu",
                "net.minecraft.world.inventory.MerchantContainer",
                "net.minecraft.world.item.trading.Merchant",
                "net.minecraft.world.entity.npc.ClientSideMerchant",
                "org.bukkit.craftbukkit.inventory.CraftMerchant"
        };
        for (String name : types) {
            Class<?> kind;
            try { kind = Class.forName(name); }
            catch (ClassNotFoundException missing) {
                getLogger().info("A12_MERCHANT_MENU_ABI_CLASS_ABSENT " + name);
                continue;
            }
            getLogger().info("A12_MERCHANT_MENU_ABI_CLASS " + kind.getName()
                    + " isInterface=" + kind.isInterface());
            for (Constructor<?> ctor : kind.getDeclaredConstructors())
                getLogger().info("A12_MERCHANT_MENU_ABI_CTOR " + ctor.toGenericString());
            for (Method method : kind.getDeclaredMethods()) {
                String n = method.getName().toLowerCase(java.util.Locale.ROOT);
                if (n.contains("offer") || n.contains("trade") || n.contains("recipe")
                        || n.contains("merchant") || n.contains("selection")
                        || n.contains("container") || n.contains("bukkitview"))
                    getLogger().info("A12_MERCHANT_MENU_ABI_METHOD " + method.toGenericString());
            }
        }
        getLogger().info("A12_MERCHANT_MENU_ABI_CAPTURED actualTrade=false"
                + " networkPacket=false playerOnline=false");
    }

    /**
     * Real NMS CartographyTableMenu's actual result calculation, with genuine
     * MapView saved data. No click, onTake, packet or inventory delivery.
     * Each case owns a fresh isolated menu to avoid stale output.
     */
    private void verifyNativeCartographyResult(CoinItemCodec codec,
                                              ItemStack sourceCoin, Player detached)
            throws ReflectiveOperationException {
        World world = Bukkit.getWorlds().getFirst();
        MapView view = Bukkit.createMap(world);
        view.setScale(MapView.Scale.CLOSE);
        ItemStack filledMap = new ItemStack(Material.FILLED_MAP, 1);
        MapMeta mapMeta = (MapMeta) filledMap.getItemMeta();
        if (mapMeta == null) throw new IllegalStateException("MapMeta unavailable");
        mapMeta.setMapView(view);
        filledMap.setItemMeta(mapMeta);
        if (filledMap.getType() != Material.FILLED_MAP || !mapMeta.hasMapView())
            throw new IllegalStateException("Paper did not create a real filled map");

        int x = 46, z = 46;
        int y = Math.max(world.getMinHeight() + 20,
                Math.min(world.getMaxHeight() - 4,
                        world.getHighestBlockYAt(x, z) + 4));
        Block block = world.getBlockAt(x, y, z);
        // Terrain/foliage can exceed the HeightMap position by four blocks.
        // This fixture owns a disposable world: find a true air position
        // instead of assuming a fixed offset is unoccupied.
        while (!block.getType().isAir() && y + 1 < world.getMaxHeight() - 1) {
            block = world.getBlockAt(x, ++y, z);
        }
        if (!block.getType().isAir())
            throw new IllegalStateException("No free disposable NMS cartography location");
        getLogger().info("A12_NMS_CARTOGRAPHY_STATION_AIR_POSITION y=" + y);

        // Access needs an actual world/position to resolve the real map data.
        Class<?> nmsInventory = Class.forName("net.minecraft.world.entity.player.Inventory");
        Class<?> nmsLevel = Class.forName("net.minecraft.world.level.Level");
        Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos");
        Class<?> accessType = Class.forName("net.minecraft.world.inventory.ContainerLevelAccess");
        Class<?> menuType = Class.forName("net.minecraft.world.inventory.CartographyTableMenu");
        Class<?> nmsStackType = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> craftStackType = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
        Object playerHandle = detached.getClass().getMethod("getHandle").invoke(detached);
        Object playerInventory = playerHandle.getClass().getMethod("getInventory")
                .invoke(playerHandle);
        Object levelHandle = world.getClass().getMethod("getHandle").invoke(world);
        Object pos = blockPos.getConstructor(int.class, int.class, int.class)
                .newInstance(x, y, z);
        Object access = accessType.getMethod("create", nmsLevel, blockPos)
                .invoke(null, levelHandle, pos);
        var ctor = menuType.getConstructor(int.class, nmsInventory, accessType);
        Method nmsConvert = craftStackType.getMethod("asNMSCopy", ItemStack.class);
        Method bukkitConvert = craftStackType.getMethod("asBukkitCopy", nmsStackType);
        Method getSlot = menuType.getMethod("getSlot", int.class);

        try {
            block.setType(Material.CARTOGRAPHY_TABLE);
            ItemStack resultPlain = nativeCartographyResult(ctor, getSlot, nmsConvert,
                    bukkitConvert, playerInventory, access, 101,
                    filledMap, new ItemStack(Material.PAPER, 1));
            ItemStack resultCoinSecond = nativeCartographyResult(ctor, getSlot, nmsConvert,
                    bukkitConvert, playerInventory, access, 102,
                    filledMap, sourceCoin.clone());
            ItemStack resultCoinFirst = nativeCartographyResult(ctor, getSlot, nmsConvert,
                    bukkitConvert, playerInventory, access, 103,
                    sourceCoin.clone(), new ItemStack(Material.PAPER, 1));
            ItemStack resultMissingMap = nativeCartographyResult(ctor, getSlot, nmsConvert,
                    bukkitConvert, playerInventory, access, 104,
                    null, sourceCoin.clone());

            Material plain = resultPlain == null ? Material.AIR : resultPlain.getType();
            Material coinSecond = resultCoinSecond == null
                    ? Material.AIR : resultCoinSecond.getType();
            Material coinFirst = resultCoinFirst == null
                    ? Material.AIR : resultCoinFirst.getType();
            Material noMap = resultMissingMap == null
                    ? Material.AIR : resultMissingMap.getType();
            if (plain != Material.FILLED_MAP
                    || coinFirst != Material.AIR || noMap != Material.AIR)
                throw new IllegalStateException(
                        "NMS cartography control mismatch plain=" + plain
                                + " coinFirst=" + coinFirst + " noMap=" + noMap);
            if (codec.identify(sourceCoin).orElseThrow()
                    != CoinDenomination.FIVE_THOUSAND || sourceCoin.getAmount() != 1)
                throw new IllegalStateException("Native cartography changed source money");
            getLogger().info("A12_NMS_CARTOGRAPHY_NATIVE_RESULT_CONTROLS"
                    + " plain=FILLED_MAP coinFirst=AIR noMap=AIR");
            verifyNativeCartographyOnTake(ctor, getSlot, nmsConvert, bukkitConvert,
                    playerInventory, access, playerHandle, filledMap, sourceCoin, codec);
            verifyNativeCartographyDirectClick(ctor, getSlot, nmsConvert,
                    bukkitConvert, access, filledMap, sourceCoin, codec);
            verifyServerHandlerPacketClick(ctor, getSlot, nmsConvert,
                    bukkitConvert, access, block, filledMap, sourceCoin, codec);
            getLogger().info("A12_NMS_CARTOGRAPHY_NATIVE_RESULT_COIN_SECOND"
                    + " output=" + coinSecond
                    + " detachedOnly=true actualClick=false physicalConsumption=false");
        } finally {
            block.setType(Material.AIR);
        }
    }

    private static ItemStack nativeCartographyResult(
            Constructor<?> ctor, Method getSlot, Method toNms, Method toBukkit,
            Object playerInventory, Object access, int id, ItemStack map, ItemStack addition)
            throws ReflectiveOperationException {
        Object menu = ctor.newInstance(id, playerInventory, access);
        Object mapSlot = getSlot.invoke(menu, 0);
        Object additionalSlot = getSlot.invoke(menu, 1);
        Class<?> nmsStackType = Class.forName("net.minecraft.world.item.ItemStack");
        Method slotSet = mapSlot.getClass().getMethod("set", nmsStackType);
        if (map != null) slotSet.invoke(mapSlot, toNms.invoke(null, map));
        if (addition != null) slotSet.invoke(additionalSlot, toNms.invoke(null, addition));
        Object resultSlot = getSlot.invoke(menu, 2);
        Object result = resultSlot.getClass().getMethod("getItem").invoke(resultSlot);
        return (ItemStack) toBukkit.invoke(null, result);
    }

    /**
     * Real CartographyTableMenu's RESULT Slot.onTake callback, with a detached
     * NMS player and entirely private input stacks. It is not the server's
     * network-click handler nor an InventoryClickEvent/Craft transaction.
     */
    private void verifyNativeCartographyOnTake(
            Constructor<?> ctor, Method getSlot, Method toNms, Method toBukkit,
            Object playerInventory, Object access, Object playerHandle,
            ItemStack filledMap, ItemStack sourceCoin, CoinItemCodec codec)
            throws ReflectiveOperationException {
        Class<?> nmsStack = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> nmsPlayer = Class.forName("net.minecraft.world.entity.player.Player");
        Class<?> baseSlot = Class.forName("net.minecraft.world.inventory.Slot");
        Method set = baseSlot.getMethod("set", nmsStack);
        Method getItem = baseSlot.getMethod("getItem");
        Method count = nmsStack.getMethod("getCount");
        Method onTake = baseSlot.getMethod("onTake", nmsPlayer, nmsStack);
        int[] before = new int[2];
        int[] after = new int[2];

        for (int index = 0; index < 2; index++) {
            boolean coinCase = index == 1;
            ItemStack ingredient = coinCase
                    ? sourceCoin.clone() : new ItemStack(Material.PAPER, 1);
            Object menu = ctor.newInstance(111 + index, playerInventory, access);
            Object slot0 = getSlot.invoke(menu, 0);
            Object slot1 = getSlot.invoke(menu, 1);
            Object slot2 = getSlot.invoke(menu, 2);
            set.invoke(slot0, toNms.invoke(null, filledMap));
            set.invoke(slot1, toNms.invoke(null, ingredient));
            Object output = getItem.invoke(slot2);
            ItemStack outputBukkit = (ItemStack) toBukkit.invoke(null, output);
            if (outputBukkit.getType() != Material.FILLED_MAP)
                throw new IllegalStateException("Actual Cartography NMS take setup did not yield map");

            before[index] = ((Number) count.invoke(getItem.invoke(slot1))).intValue();
            if (before[index] != 1)
                throw new IllegalStateException("Unexpected NMS cost before callback");
            onTake.invoke(slot2, playerHandle, output);
            after[index] = ((Number) count.invoke(getItem.invoke(slot1))).intValue();
            if (after[index] < 0 || after[index] > 1)
                throw new IllegalStateException("Unexpected NMS cost count after callback");
            getLogger().info("A12_NMS_CARTOGRAPHY_RESULT_SLOT_CALLBACK"
                    + " input=" + (coinCase ? "genuine5000C" : "ordinaryPAPER")
                    + " before=" + before[index] + " after=" + after[index]
                    + " detachedOnly=true actualClick=false");
        }
        if (after[0] != 0)
            throw new IllegalStateException("Ordinary PAPER was not consumed by native onTake");
        if (codec.identify(sourceCoin).orElseThrow()
                != CoinDenomination.FIVE_THOUSAND || sourceCoin.getAmount() != 1)
            throw new IllegalStateException("Native onTake modified original source money");
        getLogger().info("A12_NMS_CARTOGRAPHY_NATIVE_ON_TAKE"
                + " plainBefore=" + before[0] + " plainAfter=" + after[0]
                + " coinBefore=" + before[1] + " coinAfter=" + after[1]
                + " sourceCoinIntact=true actualClick=false");
    }

    /**
     * Calls the native menu.click operation directly, not the network packet
     * listener. Captures actual Bukkit event notifications separately and
     * never equates native click with a remote-client InventoryClickEvent.
     */
    private void verifyNativeCartographyDirectClick(
            Constructor<?> ctor, Method getSlot, Method toNms, Method toBukkit,
            Object access, ItemStack filledMap, ItemStack sourceCoin,
            CoinItemCodec codec) throws ReflectiveOperationException {
        Class<?> nmsStackType = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> nmsPlayerType = Class.forName("net.minecraft.world.entity.player.Player");
        Class<?> slotType = Class.forName("net.minecraft.world.inventory.Slot");
        // The click enum moved in newer Minecraft mappings; never guess a
        // package here. Resolve the actual Paper runtime native method first.
        Method clicked = java.util.Arrays.stream(
                        ctor.getDeclaringClass().getMethods())
                .filter(method -> method.getName().equals("clicked")
                        && method.getParameterCount() == 4
                        && method.getParameterTypes()[0] == int.class
                        && method.getParameterTypes()[1] == int.class
                        && method.getParameterTypes()[3].isAssignableFrom(nmsPlayerType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No native CartographyTableMenu.clicked(int,int,enum,Player)"));
        Class<?> nmsClickType = clicked.getParameterTypes()[2];
        if (!nmsClickType.isEnum())
            throw new IllegalStateException(
                    "Native click type is not an enum: " + nmsClickType.getName());
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object pickup = Enum.valueOf((Class<? extends Enum>) nmsClickType, "PICKUP");
        getLogger().info("A12_NMS_CARTOGRAPHY_ACTUAL_CLICK_SIGNATURE "
                + clicked.toGenericString());
        Method set = slotType.getMethod("set", nmsStackType);
        Method get = slotType.getMethod("getItem");
        Method count = nmsStackType.getMethod("getCount");
        Method carried = ctor.getDeclaringClass().getMethod("getCarried");
        AtomicInteger clickEvents = new AtomicInteger();
        AtomicInteger cartographyEvents = new AtomicInteger();

        Listener monitor = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void observeAnyClick(InventoryClickEvent event) {
                if (event.getRawSlot() == 2 &&
                        event.getView().getType() == InventoryType.CARTOGRAPHY)
                    clickEvents.incrementAndGet();
            }
            @EventHandler(priority = EventPriority.MONITOR)
            public void observeCartographyClick(CartographyItemEvent event) {
                if (event.getRawSlot() == 2)
                    cartographyEvents.incrementAndGet();
            }
        };
        Bukkit.getPluginManager().registerEvents(monitor, this);
        try {
            for (int i = 0; i < 2; i++) {
                Player detached = detachedPlayer();
                if (detached.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
                    throw new IllegalStateException("Native click tester was not isolated");
                Object playerHandle = detached.getClass().getMethod("getHandle").invoke(detached);
                Object playerInventory = playerHandle.getClass()
                        .getMethod("getInventory").invoke(playerHandle);
                Object menu = ctor.newInstance(121 + i, playerInventory, access);
                Object slot0 = getSlot.invoke(menu, 0);
                Object slot1 = getSlot.invoke(menu, 1);
                Object slot2 = getSlot.invoke(menu, 2);
                set.invoke(slot0, toNms.invoke(null, filledMap));
                set.invoke(slot1, toNms.invoke(null, i == 0
                        ? new ItemStack(Material.PAPER, 1) : sourceCoin.clone()));
                ItemStack initial = (ItemStack) toBukkit.invoke(null, get.invoke(slot2));
                if (initial.getType() != Material.FILLED_MAP)
                    throw new IllegalStateException("Native-click test has no real output");
                int before = ((Number) count.invoke(get.invoke(slot1))).intValue();
                int clicksBefore = clickEvents.get();
                int cartoBefore = cartographyEvents.get();
                clicked.invoke(menu, 2, 0, pickup, playerHandle);
                int after = ((Number) count.invoke(get.invoke(slot1))).intValue();
                ItemStack cursor = (ItemStack) toBukkit.invoke(null, carried.invoke(menu));
                Material carriedType = cursor == null ? Material.AIR : cursor.getType();
                if (before != 1 || after < 0 || after > 1)
                    throw new IllegalStateException("Invalid cartography direct native click counts");
                getLogger().info("A12_NMS_CARTOGRAPHY_DIRECT_CLICK"
                        + " input=" + (i == 0 ? "ordinaryPAPER" : "genuine5000C")
                        + " before=" + before + " after=" + after
                        + " inventoryClickEvents=" + (clickEvents.get() - clicksBefore)
                        + " cartographyItemEvents=" + (cartographyEvents.get() - cartoBefore)
                        + " carried=" + carriedType
                        + " directNative=true networkPacket=false");
                if (i == 0 && after != 0)
                    throw new IllegalStateException("Native click did not consume normal paper");
            }
        } finally {
            HandlerList.unregisterAll(monitor);
        }
        if (codec.identify(sourceCoin).orElseThrow() != CoinDenomination.FIVE_THOUSAND
                || sourceCoin.getAmount() != 1)
            throw new IllegalStateException("NMS direct click modified source coin");
        getLogger().info("A12_NMS_CARTOGRAPHY_DIRECT_CLICK_EVENT_BOUNDARY"
                + " totalInventoryClicks=" + clickEvents.get()
                + " totalCartographyEvents=" + cartographyEvents.get()
                + " sourceCoinIntact=true networkPacket=false");
    }

    /**
     * Exercise the real Paper server packet-handler method, but without a
     * socket or admitted user. This is NOT a network client test and must
     * not be mistaken for a fully connected player transaction.
     */
    private void verifyServerHandlerPacketClick(
            Constructor<?> cartographyCtor, Method getSlot, Method toNms,
            Method toBukkit, Object access, Block stationBlock,
            ItemStack filledMap, ItemStack sourceCoin, CoinItemCodec codec)
            throws ReflectiveOperationException {
        Class<?> mcType = Class.forName("net.minecraft.server.MinecraftServer");
        Object mc = Bukkit.getServer().getClass().getMethod("getServer")
                .invoke(Bukkit.getServer());
        Class<?> serverPlayerType = Class.forName(
                "net.minecraft.server.level.ServerPlayer");
        Class<?> basePlayerType = Class.forName(
                "net.minecraft.world.entity.player.Player");
        java.nio.file.Path basePlayerDump = getDataFolder().toPath().resolve(
                "diagnostic-classes/net/minecraft/world/entity/player/Player.class");
        try (java.io.InputStream code = basePlayerType.getResourceAsStream(
                "/net/minecraft/world/entity/player/Player.class")) {
            if (code == null) throw new IllegalStateException(
                    "Cannot read runtime NMS Player superclass");
            java.nio.file.Files.createDirectories(basePlayerDump.getParent());
            java.nio.file.Files.copy(code, basePlayerDump,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException readFailure) {
            throw new IllegalStateException("Cannot preserve Player superclass", readFailure);
        }
        // Retain exact runtime ServerPlayer implementation for the first
        // isImmobile branch in the actual handler, not mapping-version guesses.
        java.nio.file.Path playerDump = getDataFolder().toPath().resolve(
                "diagnostic-classes/net/minecraft/server/level/ServerPlayer.class");
        try (java.io.InputStream code = serverPlayerType.getResourceAsStream(
                "/net/minecraft/server/level/ServerPlayer.class")) {
            if (code == null) throw new IllegalStateException(
                    "Cannot read runtime ServerPlayer bytecode");
            java.nio.file.Files.createDirectories(playerDump.getParent());
            java.nio.file.Files.copy(code, playerDump,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException readFailure) {
            throw new IllegalStateException("Cannot preserve actual ServerPlayer class", readFailure);
        }
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> packetFlowType = Class.forName(
                "net.minecraft.network.protocol.PacketFlow");
        Class<?> connectionType = Class.forName("net.minecraft.network.Connection");
        Class<?> handlerType = Class.forName(
                "net.minecraft.server.network.ServerGamePacketListenerImpl");
        // Capture exact runtime bytecode for deterministic investigation of
        // handler early-return conditions. Disposable Paper CI only.
        java.nio.file.Path dump = getDataFolder().toPath().resolve(
                "diagnostic-classes/net/minecraft/server/network/"
                + "ServerGamePacketListenerImpl.class");
        try (java.io.InputStream code = handlerType.getResourceAsStream(
                "/net/minecraft/server/network/ServerGamePacketListenerImpl.class")) {
            if (code == null) throw new IllegalStateException(
                    "Cannot load runtime Paper handler bytecode resource");
            java.nio.file.Files.createDirectories(dump.getParent());
            java.nio.file.Files.copy(code, dump,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException readFailure) {
            throw new IllegalStateException(
                    "Cannot preserve real PacketListener bytecode for diagnosis", readFailure);
        }
        Class<?> cookieType = Class.forName(
                "net.minecraft.server.network.CommonListenerCookie");
        Class<?> inputType = Class.forName(
                "net.minecraft.world.inventory.ContainerInput");
        Class<?> packetType = Class.forName(
                "net.minecraft.network.protocol.game.ServerboundContainerClickPacket");
        Class<?> hashedType = Class.forName("net.minecraft.network.HashedStack");
        Class<?> fastMapType = Class.forName(
                "it.unimi.dsi.fastutil.ints.Int2ObjectMap");
        Class<?> nmsStackType = Class.forName("net.minecraft.world.item.ItemStack");
        Class<?> menuType = Class.forName(
                "net.minecraft.world.inventory.AbstractContainerMenu");
        Class<?> nmsPlayerType = Class.forName(
                "net.minecraft.world.entity.player.Player");
        Class<?> slotType = Class.forName("net.minecraft.world.inventory.Slot");
        Method slotSet = slotType.getMethod("set", nmsStackType);
        Method slotGet = slotType.getMethod("getItem");
        Method count = nmsStackType.getMethod("getCount");
        Method handle = handlerType.getMethod("handleContainerClick", packetType);
        Constructor<?> packetCtor = packetType.getConstructor(int.class,
                int.class, short.class, byte.class, inputType, fastMapType, hashedType);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object pickup = Enum.valueOf((Class<? extends Enum>) inputType, "PICKUP");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object serverbound = Enum.valueOf((Class<? extends Enum>) packetFlowType, "SERVERBOUND");
        Object emptyHash = hashedType.getField("EMPTY").get(null);
        java.lang.reflect.Field activeMenu = nmsPlayerType.getField("containerMenu");
        java.lang.reflect.Field playerConnection = serverPlayerType.getField("connection");
        AtomicInteger observedClicks = new AtomicInteger();
        AtomicInteger observedCartography = new AtomicInteger();
        AtomicInteger rejectedMoney = new AtomicInteger();

        Listener temporaryGuard = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void protect(InventoryClickEvent event) {
                if (event.getRawSlot() != 2
                        || event.getView().getType() != InventoryType.CARTOGRAPHY)
                    return;
                ItemStack input = event.getView().getTopInventory().getItem(1);
                if (codec.hasCoinMarker(input)) {
                    event.setCancelled(true);
                    rejectedMoney.incrementAndGet();
                }
            }
            @EventHandler(priority = EventPriority.MONITOR)
            public void observeClick(InventoryClickEvent event) {
                if (event.getRawSlot() == 2
                        && event.getView().getType() == InventoryType.CARTOGRAPHY)
                    observedClicks.incrementAndGet();
            }
            @EventHandler(priority = EventPriority.MONITOR)
            public void observeCartography(CartographyItemEvent event) {
                if (event.getRawSlot() == 2)
                    observedCartography.incrementAndGet();
            }
        };
        Bukkit.getPluginManager().registerEvents(temporaryGuard, this);
        try {
            for (int i = 0; i < 2; i++) {
                boolean coinCase = i == 1;
                Player detached = detachedPlayer();
                if (detached.isOnline() || !Bukkit.getOnlinePlayers().isEmpty())
                    throw new IllegalStateException("A12 handler test player connected");
                Object handlePlayer = detached.getClass().getMethod("getHandle")
                        .invoke(detached);
                // Native container validity checks distance to the station;
                // detached ServerPlayer defaults to the far-away spawn point.
                handlePlayer.getClass().getMethod("setPos", double.class,
                        double.class, double.class).invoke(handlePlayer,
                        stationBlock.getX() + 0.5, stationBlock.getY() + 0.5,
                        stationBlock.getZ() + 0.5);
                Object profile = handlePlayer.getClass().getMethod("getGameProfile")
                        .invoke(handlePlayer);
                Object cookie = cookieType
                        .getMethod("createInitial", profileType, boolean.class)
                        .invoke(null, profile, false);
                Object connection = connectionType.getConstructor(packetFlowType)
                        .newInstance(serverbound);
                Object networkHandler = handlerType.getConstructor(
                        mcType, connectionType, serverPlayerType, cookieType)
                        .newInstance(mc, connection, handlePlayer, cookie);
                playerConnection.set(handlePlayer, networkHandler);
                // Paper's actual Player.isImmobile first checks Entity.valid.
                // Detached synthetic ServerPlayer is never registered with
                // the world, so its valid flag remains false. This flag is
                // changed ONLY on an isolated fake player; no real login.
                java.lang.reflect.Field validFlag = null;
                Class<?> searchOwner = handlePlayer.getClass();
                while (searchOwner != null && validFlag == null) {
                    try { validFlag = searchOwner.getDeclaredField("valid"); }
                    catch (NoSuchFieldException ignored) {
                        searchOwner = searchOwner.getSuperclass();
                    }
                }
                if (validFlag == null)
                    throw new IllegalStateException(
                            "No actual Paper Player/Entity valid field");
                validFlag.setAccessible(true);
                boolean originallyValid = validFlag.getBoolean(handlePlayer);
                validFlag.setBoolean(handlePlayer, true);
                getLogger().info("A12_PACKET_HANDLER_DETACHED_ENTITY_VALIDITY"
                        + " initiallyValid=" + originallyValid
                        + " syntheticValid=true playerOnline=" + detached.isOnline());
                // The isolated Connection has no channel, so isDisconnected()
                // returns true except during ServerPlayer.joining. Use this
                // exact NMS transient state for an in-process handler probe;
                // do not misrepresent it as a connected network user.
                boolean initiallyJoining = serverPlayerType.getField("joining")
                        .getBoolean(handlePlayer);
                serverPlayerType.getField("joining").setBoolean(handlePlayer, true);
                boolean immobileAfter = (boolean) serverPlayerType.getMethod("isImmobile")
                        .invoke(handlePlayer);
                getLogger().info("A12_PACKET_HANDLER_TRANSIENT_JOIN_STATE"
                        + " initiallyJoining=" + initiallyJoining
                        + " joining=true immobile=" + immobileAfter
                        + " channelConnected=false actualClient=false");
                for (String stateCheck : new String[]{"isAlive", "isDeadOrDying",
                        "isRemoved", "isSleeping", "isSpectator", "isImmobile"}) {
                    try {
                        Object state = handlePlayer.getClass().getMethod(stateCheck)
                                .invoke(handlePlayer);
                        getLogger().info("A12_PACKET_HANDLER_PLAYER_STATE " + stateCheck
                                + "=" + state);
                    } catch (NoSuchMethodException unavailable) {
                        getLogger().info("A12_PACKET_HANDLER_PLAYER_STATE "
                                + stateCheck + "=method-absent");
                    }
                }
                if (immobileAfter)
                    throw new IllegalStateException(
                            "Detached player remains immobile despite native joining state");

                Object inventory = handlePlayer.getClass().getMethod("getInventory")
                        .invoke(handlePlayer);
                Object menu = cartographyCtor.newInstance(150 + i, inventory, access);
                activeMenu.set(handlePlayer, menu);
                Object first = getSlot.invoke(menu, 0);
                Object second = getSlot.invoke(menu, 1);
                Object result = getSlot.invoke(menu, 2);
                slotSet.invoke(first, toNms.invoke(null, filledMap));
                slotSet.invoke(second, toNms.invoke(null, coinCase
                        ? sourceCoin.clone() : new ItemStack(Material.PAPER, 1)));
                ItemStack resultStack = (ItemStack) toBukkit.invoke(null,
                        slotGet.invoke(result));
                if (resultStack.getType() != Material.FILLED_MAP)
                    throw new IllegalStateException("Packet handler test has no result");
                int before = ((Number) count.invoke(slotGet.invoke(second))).intValue();
                int clickBefore = observedClicks.get();
                int cartoBefore = observedCartography.get();
                int blockBefore = rejectedMoney.get();
                int id = ((Number) menuType.getField("containerId").get(menu)).intValue();
                int state = ((Number) menuType.getMethod("getStateId")
                        .invoke(menu)).intValue();
                Object changed = Class.forName(
                        "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap")
                        .getConstructor().newInstance();
                Object packet = packetCtor.newInstance(id, state, (short) 2,
                        (byte) 0, pickup, changed, emptyHash);
                boolean valid = (boolean) menuType.getMethod("stillValid", nmsPlayerType)
                        .invoke(menu, handlePlayer);
                getLogger().info("A12_PACKET_HANDLER_MENU_VALIDITY valid=" + valid
                        + " station=" + stationBlock.getLocation().toVector());
                boolean immobile = (boolean) serverPlayerType.getMethod("isImmobile")
                        .invoke(handlePlayer);
                getLogger().info("A12_PACKET_HANDLER_IMMOBILE_GATE isImmobile="
                        + immobile + " playerOnline=" + detached.isOnline());

                if (!valid) throw new IllegalStateException(
                        "Detached player's native menu is not usable by server handler");
                getLogger().info("A12_PACKET_HANDLER_DIRECT_INVOKE_BEGIN"
                        + " input=" + (coinCase ? "genuine5000C" : "ordinaryPAPER")
                        + " container=" + id + " state=" + state
                        + " realServerHandler=true socket=false");
                try {
                    handle.invoke(networkHandler, packet);
                } catch (java.lang.reflect.InvocationTargetException thrown) {
                    throw new IllegalStateException("NMS packet handler rejected "
                            + (coinCase ? "coin" : "paper") + " test invocation",
                            thrown.getCause());
                }
                int after = ((Number) count.invoke(slotGet.invoke(second))).intValue();
                getLogger().info("A12_PACKET_HANDLER_DIRECT_INVOKE_RESULT"
                        + " input=" + (coinCase ? "genuine5000C" : "ordinaryPAPER")
                        + " before=" + before + " after=" + after
                        + " inventoryEvents=" + (observedClicks.get() - clickBefore)
                        + " cartographyEvents=" + (observedCartography.get() - cartoBefore)
                        + " blocked=" + (rejectedMoney.get() - blockBefore)
                        + " socket=false");
                if (before != 1 || observedClicks.get() == clickBefore)
                    throw new IllegalStateException(
                            "Actual server handler did not dispatch an inventory click");
                if (coinCase && (after != 1 || rejectedMoney.get() != blockBefore + 1))
                    throw new IllegalStateException(
                            "Test-only packet click guard did not preserve tagged PAPER");
                if (!coinCase && (after != 0 || rejectedMoney.get() != blockBefore))
                    throw new IllegalStateException(
                            "Plain PAPER was not accepted by packet click handler");
            }
            getLogger().info("A12_PACKET_HANDLER_TEST_ONLY_COIN_CANCELLATION_PASSED"
                    + " socket=false onlinePlayers=0"
                    + " realHandler=true actualNetworkClient=false");
        } finally {
            HandlerList.unregisterAll(temporaryGuard);
        }
    }

    /**
     * Inspect the actual Paper build's packet-handler ABI before attempting a
     * network-path experiment. This never constructs a packet, admits a player
     * or claims that an InventoryClickEvent is dispatched.
     */
    private void characterizePacketClickEntryPoints() throws ReflectiveOperationException {
        Class<?> handler = Class.forName(
                "net.minecraft.server.network.ServerGamePacketListenerImpl");
        Class<?> packet = Class.forName(
                "net.minecraft.network.protocol.game.ServerboundContainerClickPacket");
        Class<?> nmsConnection = Class.forName("net.minecraft.network.Connection");
        Class<?> serverPlayer = Class.forName(
                "net.minecraft.server.level.ServerPlayer");
        java.util.List<Method> targets = java.util.Arrays.stream(handler.getMethods())
                .filter(method -> method.getName().equals("handleContainerClick")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == packet)
                .toList();
        if (targets.size() != 1)
            throw new IllegalStateException(
                    "NMS packet handler entrypoint is not unique: " + targets);
        getLogger().info("A12_PACKET_PATH_HANDLER " + targets.getFirst().toGenericString());
        for (Constructor<?> ctor : handler.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_HANDLER_CONSTRUCTOR " + ctor.toGenericString());
        for (Constructor<?> ctor : packet.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_PACKET_CONSTRUCTOR " + ctor.toGenericString());
        for (Constructor<?> ctor : nmsConnection.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_CONNECTION_CONSTRUCTOR " + ctor.toGenericString());
        for (java.lang.reflect.Field field : serverPlayer.getFields()) {
            if (field.getType() == handler || field.getType() == nmsConnection
                    || field.getName().toLowerCase(java.util.Locale.ROOT).contains("containermenu"))
                getLogger().info("A12_PACKET_PATH_PLAYER_FIELD " + field.toGenericString());
        }
        Class<?> cookie = Class.forName(
                "net.minecraft.server.network.CommonListenerCookie");
        for (Constructor<?> ctor : cookie.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_COOKIE_CONSTRUCTOR " + ctor.toGenericString());
        for (Method method : cookie.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    && (method.getName().toLowerCase(java.util.Locale.ROOT).contains("create")
                        || method.getName().toLowerCase(java.util.Locale.ROOT).contains("initial")))
                getLogger().info("A12_PACKET_PATH_COOKIE_FACTORY " + method.toGenericString());
        }
        Class<?> containerInput = Class.forName("net.minecraft.world.inventory.ContainerInput");
        getLogger().info("A12_PACKET_PATH_CONTAINER_INPUT type=" + containerInput.getName()
                + " isEnum=" + containerInput.isEnum()
                + " enumConstants=" + java.util.Arrays.toString(containerInput.getEnumConstants()));
        for (Constructor<?> ctor : containerInput.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_INPUT_CONSTRUCTOR " + ctor.toGenericString());
        for (java.lang.reflect.Field field : containerInput.getDeclaredFields())
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                getLogger().info("A12_PACKET_PATH_INPUT_STATIC " + field.toGenericString());
        Class<?> hashed = Class.forName("net.minecraft.network.HashedStack");
        for (Constructor<?> ctor : hashed.getDeclaredConstructors())
            getLogger().info("A12_PACKET_PATH_HASHED_CONSTRUCTOR " + ctor.toGenericString());
        for (java.lang.reflect.Field field : hashed.getDeclaredFields())
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                getLogger().info("A12_PACKET_PATH_HASHED_STATIC " + field.toGenericString());
        for (Method method : hashed.getDeclaredMethods())
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                getLogger().info("A12_PACKET_PATH_HASHED_FACTORY " + method.toGenericString());
        for (java.lang.reflect.Field field : serverPlayer.getDeclaredFields())
            if (field.getName().toLowerCase(java.util.Locale.ROOT).contains("connection")
                    || field.getName().toLowerCase(java.util.Locale.ROOT).contains("container"))
                getLogger().info("A12_PACKET_PATH_PLAYER_DECLARED_FIELD " + field.toGenericString());
        for (Method method : serverPlayer.getMethods())
            if (method.getName().equals("initMenu"))
                getLogger().info("A12_PACKET_PATH_PLAYER_INIT_MENU " + method.toGenericString());
        getLogger().info("A12_PACKET_PATH_ABI_INSPECTED entrypoints=1"
                + " eventDispatch=unverified networkPacketSent=false"
                + " detachedOnly=true");
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
