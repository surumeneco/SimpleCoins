package co.surumene.simplecoins.writeragent;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;

/**
 * Separate JVM from the normal-return proof: an escaping real transformed
 * writer Throwable must fault the bootstrap and the isolated product fence.
 */
public final class AgentExceptionalExitMain {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        require(WriterEpochHook.class.getClassLoader() == null, "not bootstrap loaded");
        new org.bukkit.craftbukkit.entity.CraftPlayer().saveData();
        var pdc = new org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey();
        var type = new org.bukkit.persistence.PersistentDataType();
        pdc.set(key, type, "normal");
        pdc.remove(key);
        var equip = new org.bukkit.craftbukkit.inventory.CraftEntityEquipment();
        equip.setItemInMainHand(new org.bukkit.inventory.ItemStack());
        equip.setItemInMainHand(new org.bukkit.inventory.ItemStack(), true);
        new co.surumene.whatawonderfulchicken.service.WonderfulChickenStore().save(
                new org.bukkit.entity.Chicken(),
                new co.surumene.whatawonderfulchicken.data.WonderfulChickenData());
        new co.surumene.www.persistence.WonderfulWolfLoadedIndividuals().saveAndRegister(
                new org.bukkit.entity.Wolf(),
                new co.surumene.www.individual.WonderfulWolfIndividual());
        require(WriterEpochHook.instrumentedTargets().equals(
                Set.of("CRAFT", "NMS", "PDC", "INVENTORY", "EQUIPMENT", "WWC", "WWW")),
                "incomplete exact target registration");
        require(WriterEpochHook.inFlight() == 0, "prior normal writer leaked");

        try (URLClassLoader child = new URLClassLoader(
                new URL[]{Path.of(args[0]).toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> fenceType = child.loadClass("co.surumene.simplecoins.domain.WriterEpochFence");
            Class<?> adapterType = child.loadClass(
                    "co.surumene.simplecoins.domain.PaperNativeSaveFenceAdapter");
            Object fence = fenceType.getConstructor().newInstance();
            Object adapter = adapterType.getConstructor(fenceType).newInstance(fence);
            require(Boolean.TRUE.equals(adapterType.getMethod("bindIfAgentPresent").invoke(adapter)),
                    "product bridge did not bind");
            fenceType.getMethod("arm").invoke(fence);
            WriterEpochHook.beginLimitedFamilyProof(); // Test only; production never arms.
            var original = new IllegalArgumentException("A01_NMS_ESCAPED");
            NativeSaveFixture.failNext(original);
            try {
                new org.bukkit.craftbukkit.entity.CraftPlayer().saveData();
                throw new AssertionError("writer body failure was suppressed");
            } catch (IllegalArgumentException actual) {
                require(actual == original, "original throwable identity was replaced");
            }
            require(WriterEpochHook.mode() == WriterEpochHook.Mode.FAULTED,
                    "escaping writer did not invalidate bootstrap generation");
            require(fenceType.getMethod("state").invoke(fence).toString().equals("FAULTED"),
                    "escaping writer left product generation apparently healthy");
            try {
                fenceType.getMethod("seal").invoke(fence);
                throw new AssertionError("faulted product generation minted a Seal");
            } catch (java.lang.reflect.InvocationTargetException expected) {
                require(expected.getCause() instanceof IllegalStateException,
                        "unexpected sealed-generation failure");
            }
        }
        System.out.println("SIMPLECOINS_A01_ESCAPING_NATIVE_WRITER_INVALIDATES_PRODUCT_PASS");
    }
}
