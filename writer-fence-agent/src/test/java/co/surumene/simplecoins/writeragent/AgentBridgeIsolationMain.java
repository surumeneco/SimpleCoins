package co.surumene.simplecoins.writeragent;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Direct JVM integration: actual injected bytecode and product in a foreign loader. */
public final class AgentBridgeIsolationMain {
    static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) throws Exception {
        check(WriterEpochHook.class.getClassLoader() == null, "hook is not bootstrap loaded");
        new CraftPlayer().saveData(); // Starts PASSIVE; exercises real transformed native fixtures.
        var pdc = new org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey();
        var valueType = new org.bukkit.persistence.PersistentDataType();
        pdc.set(key, valueType, "sample"); // Invokes real transformed CraftInventory.setItem.
        pdc.remove(key);
        check(WriterEpochHook.mode() == WriterEpochHook.Mode.PASSIVE, "passive by default");
        check(WriterEpochHook.instrumentedTargets().equals(Set.of("CRAFT", "NMS", "PDC", "INVENTORY")),
                "missing native, PDC or inventory transform/descriptor");
        check(WriterEpochHook.inFlight() == 0, "passive save leaked");

        // The product JAR is NOT on the system classpath: emulate Paper's child loader.
        try (URLClassLoader isolated = new URLClassLoader(
                new URL[]{Path.of(args[0]).toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> fenceType = isolated.loadClass("co.surumene.simplecoins.domain.WriterEpochFence");
            Class<?> adapterType = isolated.loadClass(
                    "co.surumene.simplecoins.domain.PaperNativeSaveFenceAdapter");
            Object fence = fenceType.getConstructor().newInstance();
            Object adapter = adapterType.getConstructor(fenceType).newInstance(fence);
            check((Boolean) adapterType.getMethod("bindIfAgentPresent").invoke(adapter),
                    "bootstrap binding absent");
            check(WriterEpochHook.mode() == WriterEpochHook.Mode.PASSIVE, "binding armed Agent");
            check(fenceType.getMethod("state").invoke(fence).toString().equals("DISARMED"),
                    "binding armed product fence");

            fenceType.getMethod("arm").invoke(fence); // test-only, never called in product bootstrap.
            WriterEpochHook.beginLimitedFamilyProof();
            pdc.set(key, valueType, "tracked nested write");
            pdc.remove(key);
            check(((Number) fenceType.getMethod("inFlight").invoke(fence)).intValue() == 0,
                    "actual nested PDC/Inventory callbacks failed to drain");
            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            NativeSaveFixture.install(entered, release);
            AtomicReference<Throwable> writerError = new AtomicReference<>();
            Thread activeWriter = new Thread(() -> {
                try { new CraftPlayer().saveData(); }
                catch (Throwable error) { writerError.set(error); }
            });
            activeWriter.start();
            check(entered.await(5, TimeUnit.SECONDS), "actual injected nested NMS entry missing");
            check(((Number) fenceType.getMethod("inFlight").invoke(fence)).intValue() == 2,
                    "single product fence did not count Craft + nested NMS");
            Object seal = fenceType.getMethod("seal").invoke(fence);
            Method awaitDrain = fenceType.getMethod("awaitDrained", seal.getClass(), long.class);
            check(!(Boolean) awaitDrain.invoke(fence, seal, 15L), "already-entered save drained early");
            release.countDown();
            activeWriter.join(6000);
            check(!activeWriter.isAlive() && writerError.get() == null, "real nested return not completed");
            check((Boolean) awaitDrain.invoke(fence, seal, 1000L), "normal-return drain failed");
            check(WriterEpochHook.inFlight() == 0, "bridge count not drained");
            try {
                new PlayerDataStorage().save(new Player()); // Independent NMS root after seal.
                throw new AssertionError("new NMS root unexpectedly admitted");
            } catch (IllegalStateException expected) {
                check(WriterEpochHook.mode() == WriterEpochHook.Mode.FAULTED,
                        "closed writer refusal did not invalidate bridge");
                check(fenceType.getMethod("state").invoke(fence).toString().equals("FAULTED"),
                        "Agent fault left an apparently valid product Seal");
            }
        }
        System.out.println("SIMPLECOINS_A01_CROSS_LOADER_NATIVE_AGENT_RED_GREEN_PASS");
    }
}
