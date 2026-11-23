package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Disposable no-player real Paper Hopper tick reachability experiment.
 * NEVER enable test-only writer tracking in production or claim G0 closure.
 */
public final class InventoryHopperTransferClosureProbe {
    private InventoryHopperTransferClosureProbe() {}

    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException(reason);
    }

    private static int coinsIn(Inventory inventory, CoinItemCodec codec) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (codec.identify(item).filter(d -> d == CoinDenomination.FIVE_THOUSAND).isPresent()) {
                count = Math.addExact(count, item.getAmount());
            } else if (item != null && !item.getType().isAir()) {
                throw new IllegalStateException("Unexpected noncurrency inventory item");
            }
        }
        return count;
    }

    public static void begin(Plugin probe) throws Exception {
        begin(probe, false);
    }

    public static void beginEventVeto(Plugin probe) throws Exception {
        begin(probe, true);
    }

    private static void begin(Plugin probe, boolean cancelMoveEvent) throws Exception {
        require(Bukkit.isPrimaryThread() && Bukkit.getOnlinePlayers().isEmpty()
                && "127.0.0.1".equals(Bukkit.getServer().getIp()),
                "Not isolated no-player main-thread real Paper");
        Plugin simple = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        Plugin guard = Bukkit.getPluginManager().getPlugin("SimpleCoinsGuard");
        Plugin chicken = Bukkit.getPluginManager().getPlugin("WhatAWonderfulChicken");
        Plugin wolf = Bukkit.getPluginManager().getPlugin("WhatAWonderfulWolf");
        require(simple != null && simple.isEnabled() && guard != null && guard.isEnabled()
                && chicken != null && chicken.isEnabled() && wolf != null && wolf.isEnabled(),
                "Product/Guard/owner dependency missing");
        World world = Bukkit.getWorld("world");
        require(world != null, "Real overworld missing");
        Chunk chunk = world.getChunkAt(1, 1);
        chunk.setForceLoaded(true);
        require(chunk.isLoaded(), "Hopper test chunk not loaded");

        // No subsequent setup tick: both inventories are populated in this
        // single main-thread task BEFORE test-only entry tracking and seal.
        Block hopperBlock = world.getBlockAt(30, 119, 30);
        Block chestBlock = world.getBlockAt(30, 120, 30);
        hopperBlock.setType(Material.HOPPER, false);
        chestBlock.setType(Material.CHEST, false);
        require(chestBlock.getState() instanceof Chest && hopperBlock.getState() instanceof Hopper,
                "Actual Chest and Hopper not materialized");
        require(hopperBlock.getBlockData() instanceof org.bukkit.block.data.type.Hopper,
                "Actual Hopper BlockData type invalid");
        var hopperData = (org.bukkit.block.data.type.Hopper) hopperBlock.getBlockData();
        require(hopperData.isEnabled(), "Real Hopper is disabled");
        Inventory chest = ((Chest) chestBlock.getState()).getBlockInventory();
        Inventory hopper = ((Hopper) hopperBlock.getState()).getInventory();
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        chest.setItem(0, codec.create(CoinDenomination.FIVE_THOUSAND, 2));
        require(coinsIn(chest, codec) == 2 && coinsIn(hopper, codec) == 0,
                "Actual pre-seal chest/Hopper physical initial value wrong");
        System.out.println("PAPER_A01_REAL_HOPPER_SOURCE"
                + " chest=" + chest.getClass().getName()
                + " hopper=" + hopper.getClass().getName()
                + " block=" + hopperBlock.getBlockData().getAsString()
                + " chunk=" + chunk.getX() + "," + chunk.getZ());

        ClassLoader craftLoader = Bukkit.getServer().getClass().getClassLoader();
        for (String name : new String[]{
                "org.bukkit.craftbukkit.entity.CraftPlayer",
                "net.minecraft.world.level.storage.PlayerDataStorage",
                "org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer",
                "org.bukkit.craftbukkit.inventory.CraftInventory",
                "org.bukkit.craftbukkit.inventory.CraftEntityEquipment"}) {
            Class.forName(name, false, craftLoader);
        }
        Class.forName("co.surumene.whatawonderfulchicken.service.WonderfulChickenStore",
                false, chicken.getClass().getClassLoader());
        Class.forName("co.surumene.www.persistence.WonderfulWolfLoadedIndividuals",
                false, wolf.getClass().getClassLoader());

        Class<?> hook = Class.forName(
                "co.surumene.simplecoins.writeragent.WriterEpochHook", true, null);
        require(hook.getClassLoader() == null, "Writer Agent not bootstrap loaded");
        @SuppressWarnings("unchecked")
        Set<String> targets = (Set<String>) hook.getMethod("instrumentedTargets").invoke(null);
        require(targets.equals(Set.of("CRAFT", "NMS", "PDC", "INVENTORY",
                        "EQUIPMENT", "WWC", "WWW")),
                "Known seven target Agent set incomplete: " + targets);
        require("PASSIVE".equals(hook.getMethod("mode").invoke(null).toString())
                && ((Number) hook.getMethod("inFlight").invoke(null)).intValue() == 0,
                "Cannot seal with an active or unknown native writer");
        Field field = simple.getClass().getDeclaredField("nativeWriterFence");
        field.setAccessible(true);
        require(field.get(simple) instanceof WriterEpochFence,
                "Product fence was not bound");
        WriterEpochFence fence = (WriterEpochFence) field.get(simple);
        require(fence.state() == WriterEpochFence.State.DISARMED,
                "Product unexpectedly armed a writer fence");
        // Test-only listener records the real item and both physical amounts
        // at the Paper event boundary, before a cancellation is applied.
        AtomicInteger moveEvents = new AtomicInteger();
        int[] firstAtEvent = {-1, -1};
        if (cancelMoveEvent) {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
                public void onMove(InventoryMoveItemEvent event) {
                    Location source = event.getSource().getLocation();
                    Location destination = event.getDestination().getLocation();
                    if (source == null || destination == null
                            || !source.getBlock().equals(chestBlock)
                            || !destination.getBlock().equals(hopperBlock))
                        return;
                    require(Bukkit.isPrimaryThread(), "Hopper move event not on main thread");
                    require(codec.identify(event.getItem()).filter(
                            d -> d == CoinDenomination.FIVE_THOUSAND).isPresent()
                            && event.getItem().getAmount() > 0,
                            "Unexpected item in actual Hopper event");
                    int sourceCoins = coinsIn(chest, codec);
                    int destinationCoins = coinsIn(hopper, codec);
                    if (moveEvents.getAndIncrement() == 0) {
                        firstAtEvent[0] = sourceCoins;
                        firstAtEvent[1] = destinationCoins;
                    }
                    event.setCancelled(true);
                }
            }, probe);
        }

        fence.arm(); // Test-only.
        hook.getMethod("beginLimitedFamilyProof").invoke(null);
        WriterEpochFence.Seal seal = fence.seal();
        require(fence.inFlight() == 0 && fence.state() == WriterEpochFence.State.SEALED,
                "Test-only Hopper writer epoch did not seal and drain");

        Bukkit.getScheduler().runTaskLater(probe, () -> {
            try {
                if (cancelMoveEvent) {
                    finishEventVeto(chest, hopper, codec, hook, fence, seal,
                            moveEvents.get(), firstAtEvent[0], firstAtEvent[1]);
                } else {
                    finish(chest, hopper, codec, hook, fence, seal);
                }
                Runtime.getRuntime().halt(0);
            } catch (Throwable failure) {
                probe.getLogger().log(Level.SEVERE,
                        cancelMoveEvent
                                ? "PAPER_A01_REAL_HOPPER_EVENT_VETO_PROBE_FAILED"
                                : "PAPER_A01_REAL_HOPPER_TRANSFER_CLOSURE_PROBE_FAILED",
                        failure);
                System.err.println(cancelMoveEvent
                        ? "PAPER_A01_REAL_HOPPER_EVENT_VETO_PROBE_FAILED"
                        : "PAPER_A01_REAL_HOPPER_TRANSFER_CLOSURE_PROBE_FAILED");
                System.err.flush();
                Runtime.getRuntime().halt(2);
            }
        }, 60L);
    }

    private static void finishEventVeto(Inventory chest, Inventory hopper,
            CoinItemCodec codec, Class<?> hook, WriterEpochFence fence,
            WriterEpochFence.Seal seal, int events, int firstSource, int firstDestination)
            throws Exception {
        require(Bukkit.isPrimaryThread() && Bukkit.getOnlinePlayers().isEmpty(),
                "Hopper move-event veto completion not isolated or on main thread");
        int left = coinsIn(chest, codec);
        int moved = coinsIn(hopper, codec);
        require(events > 0 && firstSource >= 0 && firstDestination >= 0,
                "No matching authentic Chest to Hopper InventoryMoveItemEvent observed");
        require(left >= 0 && moved >= 0 && left + moved == 2,
                "Hopper event cancellation changed total physical currency");
        String mode = hook.getMethod("mode").invoke(null).toString();
        System.out.println("PAPER_A01_REAL_HOPPER_EVENT_VETO_OBSERVED events=" + events
                + " sourceAtFirstEvent=" + firstSource
                + " destinationAtFirstEvent=" + firstDestination
                + " chestAfter=" + left + " hopperAfter=" + moved
                + " hookMode=" + mode + " productState=" + fence.state());
        require("LIMITED_TRACKING".equals(mode)
                        && fence.state() == WriterEpochFence.State.SEALED && fence.inFlight() == 0,
                "Hopper event cancellation left unexpected shared product epoch");
        fence.requireDrained(seal);
        try {
            chest.setItem(0, codec.create(CoinDenomination.FIVE_THOUSAND, 3));
            throw new IllegalStateException("Positive-control setter was not denied");
        } catch (IllegalStateException expected) {
            require("FAULTED".equals(hook.getMethod("mode").invoke(null).toString())
                            && fence.state() == WriterEpochFence.State.FAULTED,
                    "Known covered setter did not invalidate both fences");
        }
        require(coinsIn(chest, codec) == left && coinsIn(hopper, codec) == moved,
                "Positive-control setter unexpectedly changed physical money");
        if (left == 2 && moved == 0) {
            System.out.println("PAPER_A01_REAL_HOPPER_EVENT_VETO_PHYSICAL_UNCHANGED_PASS"
                    + " sourceAtFirstEvent=" + firstSource
                    + " destinationAtFirstEvent=" + firstDestination
                    + " matchingEvents=" + events
                    + " sameSealDrained=true coveredSetterDenied=true");
        } else {
            System.out.println("PAPER_A01_REAL_HOPPER_EVENT_VETO_MONETARY_CHANGE_OBSERVED"
                    + " chestAfter=" + left + " hopperAfter=" + moved
                    + " matchingEvents=" + events
                    + " sameSealDrained=true coveredSetterDenied=true");
        }
    }

    private static void finish(Inventory chest, Inventory hopper, CoinItemCodec codec,
            Class<?> hook, WriterEpochFence fence, WriterEpochFence.Seal seal) throws Exception {
        require(Bukkit.isPrimaryThread() && Bukkit.getOnlinePlayers().isEmpty(),
                "Hopper completion not on isolated Paper main thread");
        int left = coinsIn(chest, codec);
        int moved = coinsIn(hopper, codec);
        require(left >= 0 && left < 2 && moved > 0 && left + moved == 2,
                "Unproved Hopper tick transfer: chest=" + left + " hopper=" + moved);
        String mode = hook.getMethod("mode").invoke(null).toString();
        System.out.println("PAPER_A01_REAL_HOPPER_TRANSFER_OBSERVED chestBefore=2"
                + " chestAfter=" + left + " hopperBefore=0 hopperAfter=" + moved
                + " hookMode=" + mode + " productState=" + fence.state());
        if ("FAULTED".equals(mode)) {
            require(fence.state() == WriterEpochFence.State.FAULTED,
                    "Agent fault was not propagated to product fence");
            System.out.println("PAPER_A01_REAL_HOPPER_NATIVE_PARTIAL_MUTATION_BEFORE_FAULT"
                    + " chest=" + left + " hopper=" + moved);
            return;
        }
        require("LIMITED_TRACKING".equals(mode)
                && fence.state() == WriterEpochFence.State.SEALED && fence.inFlight() == 0,
                "Hopper changed money under unexpected writer epoch state");
        fence.requireDrained(seal);
        try {
            chest.setItem(0, codec.create(CoinDenomination.FIVE_THOUSAND, 3));
            throw new IllegalStateException("Positive-control setter was not rejected");
        } catch (IllegalStateException rejected) {
            require("FAULTED".equals(hook.getMethod("mode").invoke(null).toString())
                    && fence.state() == WriterEpochFence.State.FAULTED,
                    "Known setter failed to invalidate product epoch");
        }
        require(coinsIn(chest, codec) == left && coinsIn(hopper, codec) == moved,
                "Known setter positive control altered physical Hopper currency");
        System.out.println("PAPER_A01_REAL_HOPPER_NATIVE_UNCOVERED_TRANSFER"
                + " chestBefore=2 chestAfter=" + left
                + " hopperBefore=0 hopperAfter=" + moved
                + " sameSealDrained=true coveredSetterDenied=true");
    }
}
