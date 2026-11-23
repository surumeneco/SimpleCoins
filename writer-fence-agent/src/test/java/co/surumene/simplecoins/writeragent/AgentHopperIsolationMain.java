package co.surumene.simplecoins.writeragent;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Isolated Java25 transformed method: native Hopper returns false before body
 * on a refused sealed entry instead of throwing into the Paper tick loop.
 * Actual Paper/5000C physical source is independently checked in CI.
 */
public final class AgentHopperIsolationMain {
    private AgentHopperIsolationMain() {}

    private static void require(boolean actual, String why) {
        if (!actual) throw new AssertionError(why);
    }

    private static boolean pull() {
        return HopperBlockEntity.hopperPull(
                new net.minecraft.world.level.Level(),
                new net.minecraft.world.level.block.entity.Hopper(),
                new net.minecraft.world.Container(),
                new net.minecraft.world.item.ItemStack(), 0);
    }

    public static void main(String[] args) throws Exception {
        require(WriterEpochHook.class.getClassLoader() == null,
                "Bootstrap hook must not be system-loaded");
        var loader = ClassLoader.getSystemClassLoader();
        for (String name : new String[]{
                "org.bukkit.craftbukkit.entity.CraftPlayer",
                "net.minecraft.world.level.storage.PlayerDataStorage",
                "org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer",
                "org.bukkit.craftbukkit.inventory.CraftInventory",
                "org.bukkit.craftbukkit.inventory.CraftEntityEquipment",
                "co.surumene.whatawonderfulchicken.service.WonderfulChickenStore",
                "co.surumene.www.persistence.WonderfulWolfLoadedIndividuals",
                "net.minecraft.world.level.block.entity.HopperBlockEntity"}) {
            Class.forName(name, false, loader);
        }
        require(WriterEpochHook.instrumentedTargets().equals(Set.of(
                "CRAFT", "NMS", "PDC", "INVENTORY", "EQUIPMENT", "WWC", "WWW", "HOPPER")),
                "Exact native Hopper method missing from eight targets");
        int passiveBefore = HopperBlockEntity.bodyCount();
        require(pull() && HopperBlockEntity.bodyCount() == passiveBefore + 1,
                "Passive native Hopper original method and boolean return changed");
        require(WriterEpochHook.mode() == WriterEpochHook.Mode.PASSIVE
                        && WriterEpochHook.inFlight() == 0,
                "Passive Hopper left a live bridge writer");

        try (URLClassLoader child = new URLClassLoader(
                new URL[]{Path.of(args[0]).toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> fenceType = child.loadClass("co.surumene.simplecoins.domain.WriterEpochFence");
            Class<?> adapterType = child.loadClass(
                    "co.surumene.simplecoins.domain.PaperNativeSaveFenceAdapter");
            Object fence = fenceType.getConstructor().newInstance();
            Object adapter = adapterType.getConstructor(fenceType).newInstance(fence);
            require((Boolean) adapterType.getMethod("bindIfAgentPresent").invoke(adapter),
                    "Product did not bind real bootstrap hook");
            fenceType.getMethod("arm").invoke(fence);
            WriterEpochHook.beginLimitedFamilyProof(); // isolated test only
            int openBefore = HopperBlockEntity.bodyCount();
            require(pull() && HopperBlockEntity.bodyCount() == openBefore + 1,
                    "Open root Hopper call failed to enter body/return normally");
            require((int) fenceType.getMethod("inFlight").invoke(fence) == 0
                            && WriterEpochHook.inFlight() == 0,
                    "Open Hopper writer did not drain both epoch counters");
            Object seal = fenceType.getMethod("seal").invoke(fence);
            var requireDrained = fenceType.getMethod("requireDrained", seal.getClass());
            requireDrained.invoke(fence, seal);
            int bodyBeforeDenied = HopperBlockEntity.bodyCount();
            require(!pull(), "Sealed Hopper call did not return false on denied entry");
            require(HopperBlockEntity.bodyCount() == bodyBeforeDenied,
                    "Sealed native Hopper executed body despite refusal");
            require(WriterEpochHook.mode() == WriterEpochHook.Mode.FAULTED
                            && "FAULTED".equals(fenceType.getMethod("state").invoke(fence).toString()),
                    "Denied native Hopper did not revoke shared product/Agent generation");
            require(!pull() && HopperBlockEntity.bodyCount() == bodyBeforeDenied,
                    "Already faulted Hopper unexpectedly resumed physical write");
            require(WriterEpochHook.inFlight() == 0,
                    "Denied native Hopper leaked an Agent entry");
        }
        System.out.println("SIMPLECOINS_A01_NATIVE_HOPPER_PREBODY_SHARED_FENCE_GREEN");
    }
}
