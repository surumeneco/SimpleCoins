package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** A01 bounded actual Paper test-only Hopper OUTPUT writer observation. */
public final class InventoryHopperOutputWriterProbe {
    private InventoryHopperOutputWriterProbe() {}

    private static void check(boolean ok, String reason) {
        if (!ok) throw new IllegalStateException(reason);
    }

    private static int count(Inventory inventory, CoinItemCodec codec) {
        int coins = 0;
        for (ItemStack item : inventory.getContents()) {
            if (codec.identify(item).filter(d -> d == CoinDenomination.FIVE_THOUSAND).isPresent())
                coins = Math.addExact(coins, item.getAmount());
            else if (item != null && !item.getType().isAir())
                throw new IllegalStateException("Unexpected item in Hopper source/destination");
        }
        return coins;
    }

    private static void fail(Throwable failure) {
        failure.printStackTrace(System.err);
        System.err.println("PAPER_A01_REAL_HOPPER_OUTPUT_PROBE_FAILED");
        System.err.flush();
        Runtime.getRuntime().halt(2);
    }

    public static void begin(Plugin probe) throws Exception {
        begin(probe, false);
    }

    /** Demand actual pre-body protection only after the expected-reason Java25 Red. */
    public static void beginRequirePrebody(Plugin probe) throws Exception {
        begin(probe, true);
    }

    private static void begin(Plugin probe, boolean requireProtected) throws Exception {
        check(Bukkit.isPrimaryThread() && Bukkit.getOnlinePlayers().isEmpty()
                && "127.0.0.1".equals(Bukkit.getServer().getIp()), "Observer not isolated");
        Plugin product = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        Plugin guard = Bukkit.getPluginManager().getPlugin("SimpleCoinsGuard");
        Plugin wwc = Bukkit.getPluginManager().getPlugin("WhatAWonderfulChicken");
        Plugin www = Bukkit.getPluginManager().getPlugin("WhatAWonderfulWolf");
        check(product != null && product.isEnabled() && guard != null && guard.isEnabled()
                && wwc != null && wwc.isEnabled() && www != null && www.isEnabled(),
                "Missing actual product/guard/owner plugin");
        var world = Bukkit.getWorld("world");
        check(world != null, "Missing actual world");
        var chunk = world.getChunkAt(1, 1);
        chunk.setForceLoaded(true);
        var hopperBlock = world.getBlockAt(30, 120, 30);
        var chestBlock = world.getBlockAt(30, 119, 30);
        hopperBlock.setType(Material.HOPPER, false);
        chestBlock.setType(Material.CHEST, false);
        var hopperData = (org.bukkit.block.data.type.Hopper) hopperBlock.getBlockData();
        hopperData.setFacing(BlockFace.DOWN);
        hopperData.setEnabled(true);
        hopperBlock.setBlockData(hopperData, false);
        check(hopperBlock.getState() instanceof Hopper
                && chestBlock.getState() instanceof Chest, "Real block types absent");
        Inventory hopper = ((Hopper) hopperBlock.getState()).getInventory();
        Inventory chest = ((Chest) chestBlock.getState()).getBlockInventory();
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        hopper.setItem(0, codec.create(CoinDenomination.FIVE_THOUSAND, 2));
        check(count(hopper, codec) == 2 && count(chest, codec) == 0,
                "Invalid physical baseline");
        AtomicInteger matches = new AtomicInteger();
        String[] stack = {""};
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
            public void onMove(InventoryMoveItemEvent event) {
                var source = event.getSource().getLocation();
                var dest = event.getDestination().getLocation();
                if (source == null || dest == null
                        || !source.getBlock().equals(hopperBlock)
                        || !dest.getBlock().equals(chestBlock)) return;
                check(codec.identify(event.getItem())
                        .filter(d -> d == CoinDenomination.FIVE_THOUSAND).isPresent(),
                        "Wrong item in actual Hopper output event");
                if (matches.getAndIncrement() == 0) {
                    stack[0] = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .walk(frames -> frames.limit(80).map(f ->
                                    f.getClassName() + "#" + f.getMethodName()
                                    + f.getDescriptor() + "@" + f.getLineNumber())
                                    .collect(java.util.stream.Collectors.joining(" <- ")));
                    System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_NATIVE_STACK " + stack[0]);
                }
            }
        }, probe);
        // First require a real *PASSIVE* transfer; a sealed no-op is not evidence.
        new org.bukkit.scheduler.BukkitRunnable() {
            private int elapsed;
            @Override public void run() {
            try {
                if (count(hopper, codec) != 0 || count(chest, codec) != 2 || matches.get() == 0) {
                    if (++elapsed < 200) return;
                    throw new IllegalStateException("Passive Hopper output timeout: hopper="
                            + count(hopper, codec) + " chest=" + count(chest, codec)
                            + " events=" + matches.get() + " forceLoaded=" + chunk.isForceLoaded()
                            + " data=" + hopperBlock.getBlockData());
                }
                cancel();
                check(count(hopper, codec) == 0 && count(chest, codec) == 2
                        && matches.get() > 0 && stack[0].contains("HopperBlockEntity#"),
                        "Passive actual Hopper→Chest native output not established");
                System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_PASSIVE_BASELINE"
                        + " hopper=2->0 chest=0->2 events=" + matches.get());
                chest.setItem(0, null);
                hopper.setItem(0, codec.create(CoinDenomination.FIVE_THOUSAND, 2));
                check(count(hopper, codec) == 2 && count(chest, codec) == 0,
                        "Pre-seal source/destination reset failed");
                ClassLoader loader = Bukkit.getServer().getClass().getClassLoader();
                for (String name : new String[] {
                        "org.bukkit.craftbukkit.entity.CraftPlayer",
                        "net.minecraft.world.level.storage.PlayerDataStorage",
                        "org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer",
                        "org.bukkit.craftbukkit.inventory.CraftInventory",
                        "org.bukkit.craftbukkit.inventory.CraftEntityEquipment",
                        "net.minecraft.world.level.block.entity.HopperBlockEntity"})
                    Class.forName(name, false, loader);
                Class.forName("co.surumene.whatawonderfulchicken.service.WonderfulChickenStore",
                        false, wwc.getClass().getClassLoader());
                Class.forName("co.surumene.www.persistence.WonderfulWolfLoadedIndividuals",
                        false, www.getClass().getClassLoader());
                Class<?> hook = Class.forName(
                        "co.surumene.simplecoins.writeragent.WriterEpochHook", true, null);
                @SuppressWarnings("unchecked")
                Set<String> transformed = (Set<String>)
                        hook.getMethod("instrumentedTargets").invoke(null);
                check(transformed.equals(Set.of("CRAFT", "NMS", "PDC", "INVENTORY",
                        "EQUIPMENT", "WWC", "WWW", "HOPPER"))
                        && hook.getMethod("mode").invoke(null).toString().equals("PASSIVE")
                        && ((Number) hook.getMethod("inFlight").invoke(null)).intValue() == 0,
                        "Missing passive exact hook roster or active invocation");
                Field field = product.getClass().getDeclaredField("nativeWriterFence");
                field.setAccessible(true);
                WriterEpochFence fence = (WriterEpochFence) field.get(product);
                check(fence.state() == WriterEpochFence.State.DISARMED, "Product already armed");
                fence.arm();
                hook.getMethod("beginLimitedFamilyProof").invoke(null);
                WriterEpochFence.Seal seal = fence.seal();
                fence.requireDrained(seal);
                int priorEvents = matches.get();
                Bukkit.getScheduler().runTaskLater(probe, () -> {
                    try {
                        int from = count(hopper, codec), to = count(chest, codec);
                        String mode = hook.getMethod("mode").invoke(null).toString();
                        String productState = fence.state().toString();
                        check(from + to == 2, "Actual money lost or duplicated");
                        check(matches.get() > priorEvents || mode.equals("FAULTED"),
                                "No post-seal Hopper writer attempt was observed");
                        if (from < 2 && to > 0 && mode.equals("LIMITED_TRACKING")
                                && productState.equals("SEALED")) {
                            fence.requireDrained(seal);
                            System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_UNCOVERED_WRITER"
                                    + " hopper=" + from + " chest=" + to
                                    + " sameSealDrained=true");
                        } else if (from < 2 && to > 0 && mode.equals("FAULTED")
                                && productState.equals("FAULTED")) {
                            System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_PARTIAL_BEFORE_FAULT"
                                    + " hopper=" + from + " chest=" + to);
                        } else if (from == 2 && to == 0 && mode.equals("FAULTED")
                                && productState.equals("FAULTED")) {
                            System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_EXISTING_HOOK_REFUSED"
                                    + " noPushSpecificPrebodyProof=true");
                        } else {
                            throw new IllegalStateException("Unclassified post-seal push"
                                    + " hopper=" + from + " chest=" + to
                                    + " agent=" + mode + " product=" + productState);
                        }
                        if (requireProtected) {
                            // The original eight-class Agent without an exact output method
                            // was RED: Hopper 2→0/Chest 0→2 while product SEALED.
                            check(from == 2 && to == 0
                                            && mode.equals("FAULTED")
                                            && productState.equals("FAULTED"),
                                    "Real Paper hopperPush still executed under sealed generation"
                                            + " hopper=" + from + " chest=" + to
                                            + " hook=" + mode + " product=" + productState);
                            System.out.println("PAPER_A01_REAL_HOPPER_OUTPUT_PREBODY_NO_MUTATION_PASS"
                                    + " hopperBefore=2 hopperAfter=2"
                                    + " chestBefore=0 chestAfter=0"
                                    + " sharedWriterEpochFaulted=true tickSurvived=true");
                        }
                        System.out.flush();
                        Runtime.getRuntime().halt(0);
                    } catch (Throwable failure) { fail(failure); }
                }, 60L);
            } catch (Throwable failure) { fail(failure); }
            }
        }.runTaskTimer(probe, 1L, 1L);
    }
}
