package co.surumene.simplecoins.a05probe;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.WriterEpochFence;
import co.surumene.simplecoins.paper.CoinItemCodec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Disposable real Paper, no-player negative reachability experiment.
 * Arms only in this isolated test JVM; the product itself never arms a fence.
 * Never deploy this class or use partial-family results to permit G0/G3.
 */
public final class InventoryClearClosureProbe {
    private InventoryClearClosureProbe() {}

    private static void require(boolean ok, String why) {
        if (!ok) throw new IllegalStateException(why);
    }

    private static int coinsIn(Inventory inventory, CoinItemCodec codec) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (codec.identify(item).filter(d -> d == CoinDenomination.fromValue(5000))
                    .isPresent())
                count = Math.addExact(count, item.getAmount());
            else if (item != null && !item.getType().isAir())
                throw new IllegalStateException("Unexpected non-currency inventory item");
        }
        return count;
    }

    public static String execute() throws Exception {
        require(Bukkit.isPrimaryThread() && Bukkit.getOnlinePlayers().isEmpty()
                && "127.0.0.1".equals(Bukkit.getServer().getIp()),
                "Actual Paper test not isolated on main thread");
        Plugin simple = Bukkit.getPluginManager().getPlugin("SimpleCoins");
        Plugin guard = Bukkit.getPluginManager().getPlugin("SimpleCoinsGuard");
        Plugin chicken = Bukkit.getPluginManager().getPlugin("WhatAWonderfulChicken");
        Plugin wolf = Bukkit.getPluginManager().getPlugin("WhatAWonderfulWolf");
        require(simple != null && simple.isEnabled() && guard != null && guard.isEnabled()
                && chicken != null && chicken.isEnabled() && wolf != null && wolf.isEnabled(),
                "Product, guard or distributed owner plugin absent");
        World world = Bukkit.getWorld("world");
        require(world != null, "Real vanilla overworld unavailable");
        Block block = world.getBlockAt(30, 120, 30);
        block.setType(Material.CHEST, false);
        require(block.getState() instanceof Chest, "Real Chest did not materialize");
        Inventory chest = ((Chest) block.getState()).getBlockInventory();
        Method liveClear = chest.getClass().getMethod("clear");
        System.out.println("PAPER_A01_REAL_CLEAR_METHOD"
                + " receiver=" + chest.getClass().getName()
                + " owner=" + liveClear.getDeclaringClass().getName()
                + " descriptor=" + java.lang.invoke.MethodType
                        .methodType(liveClear.getReturnType(),
                                liveClear.getParameterTypes())
                        .toMethodDescriptorString());
        CoinItemCodec codec = new CoinItemCodec(CoinItemCodec.defaultNames());
        ItemStack seed = codec.create(CoinDenomination.fromValue(5000), 2);
        ItemStack directSetterControl = codec.create(CoinDenomination.fromValue(5000), 3);
        chest.setItem(0, seed);
        require(coinsIn(chest, codec) == 2, "Initial authentic two chest coins not observed");

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
        require(hook.getClassLoader() == null, "Writer Agent not bootstrap-loaded");
        @SuppressWarnings("unchecked")
        Set<String> targets = (Set<String>) hook.getMethod("instrumentedTargets")
                .invoke(null);
        require(targets.equals(Set.of(
                "CRAFT", "NMS", "PDC", "INVENTORY", "EQUIPMENT", "WWC", "WWW")),
                "Exact installed Agent class/descriptor set incomplete: " + targets);
        require(hook.getMethod("mode").invoke(null).toString().equals("PASSIVE")
                && ((Number) hook.getMethod("inFlight").invoke(null)).intValue() == 0,
                "Test-only limited proof requested during active or faulted writer");

        Field field = simple.getClass().getDeclaredField("nativeWriterFence");
        field.setAccessible(true);
        Object raw = field.get(simple);
        require(raw instanceof WriterEpochFence, "Product writer fence not bound to plugin");
        WriterEpochFence fence = (WriterEpochFence) raw;
        require(fence.state() == WriterEpochFence.State.DISARMED,
                "Product unexpectedly armed a writer fence");

        // Test-only activation: never wired by the real SimpleCoins bootstrap.
        fence.arm();
        hook.getMethod("beginLimitedFamilyProof").invoke(null);
        WriterEpochFence.Seal seal = fence.seal();
        require(fence.inFlight() == 0 && fence.state() == WriterEpochFence.State.SEALED,
                "Test-only seven-family writer seal not established");

        boolean intercepted = false;
        try {
            chest.clear();
        } catch (IllegalStateException rejected) {
            require(hook.getMethod("mode").invoke(null).toString().equals("FAULTED")
                    && fence.state() == WriterEpochFence.State.FAULTED,
                    "clear rejected without Agent and product fail-closed");
            System.err.println("PAPER_A01_CLEAR_INTERCEPT_TRACE "
                    + java.util.Arrays.toString(rejected.getStackTrace()));
            intercepted = true;
        }
        int after = coinsIn(chest, codec);
        if (intercepted) {
            if (after == 2)
                return "PAPER_A01_REAL_CHEST_CLEAR_INTERCEPTED count=2";
            // An escaping exception does not prove that earlier inventory
            // mutation in the same clear body was prevented.
            require(after >= 0 && after < 2, "Unexpected partial clear effect on money: " + after);
            return "PAPER_A01_REAL_CHEST_CLEAR_PARTIAL_MUTATION_BEFORE_COVERED_HOOK_REFUSAL"
                    + " coinsBefore=2 coinsAfter=" + after + " hookFaulted=true productFaulted=true";
        }
        require(after == 0, "Unintercepted clear did not remove all genuine coins");
        require(fence.inFlight() == 0
                        && fence.state() == WriterEpochFence.State.SEALED
                        && hook.getMethod("mode").invoke(null).toString().equals("LIMITED_TRACKING"),
                "Unexpected Agent/Paper state after clear");
        // A drained seal here is deliberately a NEGATIVE proof: an untracked
        // LIVE monetary mutation occurred under the same still-valid seal.
        fence.requireDrained(seal);
        try {
            chest.setItem(0, directSetterControl);
            throw new IllegalStateException("Positive-control CraftInventory.setItem was not denied");
        } catch (IllegalStateException blocked) {
            require(hook.getMethod("mode").invoke(null).toString().equals("FAULTED")
                            && fence.state() == WriterEpochFence.State.FAULTED,
                    "Covered setter did not invalidate the shared product generation");
        }
        require(coinsIn(chest, codec) == 0, "Positive control altered authentic money");
        return "PAPER_A01_REAL_CHEST_CLEAR_SEALED_SEVEN_FAMILY_BYPASS_OBSERVED"
                + " coinsBefore=2 coinsAfter=0 sameSealDrained=true coveredSetterDenied=true";
    }
}
