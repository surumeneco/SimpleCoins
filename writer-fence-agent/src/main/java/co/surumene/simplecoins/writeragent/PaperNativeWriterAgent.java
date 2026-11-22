package co.surumene.simplecoins.writeragent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

/**
 * Java 25 startup agent for six bounded native/Paper/WCL writer families.
 * Native PlayerDataStorage and CraftPlayer plus PDC set/remove and inventory
 * setItem are not a complete plugin, NMS, asynchronous or direct-file writer
 * universe. No observed hook count authorizes G0/G3 publication or Admission.
 */
public final class PaperNativeWriterAgent {
    private static final ClassDesc BOOTSTRAP =
            ClassDesc.of("co.surumene.simplecoins.writeragent.WriterEpochHook");
    private static final String PLAYER = "(Lnet/minecraft/world/entity/player/Player;)V";
    private static final String PDC_SET = "(Lorg/bukkit/NamespacedKey;"
            + "Lorg/bukkit/persistence/PersistentDataType;Ljava/lang/Object;)V";
    private static final String PDC_REMOVE = "(Lorg/bukkit/NamespacedKey;)V";
    private static final String INVENTORY_SET = "(ILorg/bukkit/inventory/ItemStack;)V";
    private static final String WWC_SAVE = "(Lorg/bukkit/entity/Chicken;Lco/surumene/"
            + "whatawonderfulchicken/data/WonderfulChickenData;)V";
    private static final String WWW_SAVE = "(Lorg/bukkit/entity/Wolf;Lco/surumene/www/"
            + "individual/WonderfulWolfIndividual;)V";

    private enum Target {
        CRAFT("beforeCraft", "afterCraft", 1),
        NMS("beforeNms", "afterNms", 1),
        PDC("beforePdc", "afterPdc", 2),
        INVENTORY("beforeInventory", "afterInventory", 1),
        WWC("beforeWwc", "afterWwc", 1),
        WWW("beforeWww", "afterWww", 1);

        final String enterHook, returnHook;
        final int expectedMethods;
        Target(String enterHook, String returnHook, int expectedMethods) {
            this.enterHook = enterHook;
            this.returnHook = returnHook;
            this.expectedMethods = expectedMethods;
        }

        boolean isWriter(MethodModel m) {
            String name = m.methodName().stringValue();
            String desc = m.methodTypeSymbol().descriptorString();
            return switch (this) {
                case CRAFT -> name.equals("saveData") && desc.equals("()V");
                case NMS -> name.equals("save") && desc.equals(PLAYER);
                case PDC -> name.equals("set") && desc.equals(PDC_SET)
                        || name.equals("remove") && desc.equals(PDC_REMOVE);
                case INVENTORY -> name.equals("setItem") && desc.equals(INVENTORY_SET);
                case WWC -> name.equals("save") && desc.equals(WWC_SAVE);
                case WWW -> name.equals("saveAndRegister") && desc.equals(WWW_SAVE);
            };
        }
    }

    private PaperNativeWriterAgent() {}

    private static Target targetFor(String name) {
        return switch (name) {
            case "org/bukkit/craftbukkit/entity/CraftPlayer" -> Target.CRAFT;
            case "net/minecraft/world/level/storage/PlayerDataStorage" -> Target.NMS;
            case "org/bukkit/craftbukkit/persistence/CraftPersistentDataContainer" -> Target.PDC;
            case "org/bukkit/craftbukkit/inventory/CraftInventory" -> Target.INVENTORY;
            case "co/surumene/whatawonderfulchicken/service/WonderfulChickenStore" -> Target.WWC;
            case "co/surumene/www/persistence/WonderfulWolfLoadedIndividuals" -> Target.WWW;
            default -> null;
        };
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        if (Runtime.version().feature() != 25)
            throw new IllegalStateException("SimpleCoins Agent requires Java 25");
        try {
            String hookJar = System.getProperty("simplecoins.writerAgent.bootstrapJar");
            if (hookJar == null || hookJar.isBlank())
                throw new IllegalStateException("Missing separate bootstrap hook JAR");
            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(Path.of(hookJar).toFile()));
            if (WriterEpochHook.class.getClassLoader() != null)
                throw new IllegalStateException("Writer hook must be bootstrap-loaded");
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot initialize isolated bootstrap writer hook", failure);
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name,
                    Class<?> redefining, ProtectionDomain domain, byte[] source) {
                Target target = targetFor(name);
                if (target == null) return null;
                try {
                    ClassHierarchyResolver hierarchy = ClassHierarchyResolver.ofResourceParsing(loader)
                            .orElse(ClassHierarchyResolver.defaultResolver());
                    ClassFile api = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy));
                    AtomicInteger matched = new AtomicInteger();
                    byte[] changed = api.transformClass(api.parse(source), (builder, element) -> {
                        if (element instanceof MethodModel method && target.isWriter(method)) {
                            matched.incrementAndGet();
                            builder.transformMethod(method, MethodTransform.transformingCode(
                                    new CodeTransform() {
                                        @Override public void atStart(CodeBuilder code) {
                                            code.invokestatic(BOOTSTRAP, target.enterHook,
                                                    MethodTypeDesc.ofDescriptor("()V"));
                                        }

                                        @Override public void accept(CodeBuilder code, CodeElement element) {
                                            // All selected methods return void. Exception paths remain
                                            // uncompleted and block a later protected-epoch drain.
                                            if (element instanceof ReturnInstruction)
                                                code.invokestatic(BOOTSTRAP, target.returnHook,
                                                        MethodTypeDesc.ofDescriptor("()V"));
                                            code.with(element);
                                        }
                                    }));
                        } else {
                            builder.with(element);
                        }
                    });
                    if (matched.get() != target.expectedMethods)
                        throw new IllegalStateException("Expected " + target.expectedMethods
                                + " exact " + target + " writer method(s), found " + matched.get());
                    WriterEpochHook.recordInstrumentedTarget(target.name());
                    System.out.println("SIMPLECOINS_WRITER_AGENT_" + target
                            + "_EXACT_ENTRY_INSTRUMENTED");
                    return changed;
                } catch (Throwable failure) {
                    System.err.println("SIMPLECOINS_WRITER_AGENT_" + target + "_TRANSFORM_FAILED");
                    failure.printStackTrace(System.err);
                    try { WriterEpochHook.fault(); }
                    catch (Throwable revokeFailed) { revokeFailed.printStackTrace(System.err); }
                    return null;
                }
            }
        });
        System.out.println("SIMPLECOINS_WRITER_AGENT_BOOTSTRAP_INSTALLED_PASSIVE");
    }
}
