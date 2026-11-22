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
 * Startup-only Java 25 bytecode bridge for two known Paper native save methods.
 * Never represents all plugin/Paper/file/async writer coverage, and never
 * unlocks G0, G3 or Admission. Use with its separately packaged bootstrap JAR.
 */
public final class PaperNativeWriterAgent {
    private static final ClassDesc BOOTSTRAP =
            ClassDesc.of("co.surumene.simplecoins.writeragent.WriterEpochHook");
    private static final String CRAFT = "org/bukkit/craftbukkit/entity/CraftPlayer";
    private static final String NMS = "net/minecraft/world/level/storage/PlayerDataStorage";
    private static final String NMS_METHOD = "(Lnet/minecraft/world/entity/player/Player;)V";

    private PaperNativeWriterAgent() {}

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
                throw new IllegalStateException("Writer hook must be loaded by bootstrap classloader");
        } catch (Exception badBootstrap) {
            throw new IllegalStateException("Cannot initialize isolated bootstrap writer hook", badBootstrap);
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name,
                    Class<?> redefine, ProtectionDomain domain, byte[] source) {
                if (!CRAFT.equals(name) && !NMS.equals(name)) return null;
                String family = CRAFT.equals(name) ? "CRAFT" : "NMS";
                try {
                    ClassHierarchyResolver hierarchy = ClassHierarchyResolver.ofResourceParsing(loader)
                            .orElse(ClassHierarchyResolver.defaultResolver());
                    ClassFile api = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(hierarchy));
                    AtomicInteger count = new AtomicInteger();
                    byte[] changed = api.transformClass(api.parse(source), (builder, element) -> {
                        if (element instanceof MethodModel method
                                && method.methodName().stringValue().equals(
                                        CRAFT.equals(name) ? "saveData" : "save")
                                && method.methodTypeSymbol().descriptorString().equals(
                                        CRAFT.equals(name) ? "()V" : NMS_METHOD)) {
                            count.incrementAndGet();
                            builder.transformMethod(method, MethodTransform.transformingCode(
                                    new CodeTransform() {
                                        @Override public void atStart(CodeBuilder code) {
                                            code.invokestatic(BOOTSTRAP,
                                                    CRAFT.equals(name) ? "beforeCraft" : "beforeNms",
                                                    MethodTypeDesc.ofDescriptor("()V"));
                                        }
                                        @Override public void accept(CodeBuilder code, CodeElement element) {
                                            if (element instanceof ReturnInstruction) {
                                                code.invokestatic(BOOTSTRAP,
                                                        CRAFT.equals(name) ? "afterCraft" : "afterNms",
                                                        MethodTypeDesc.ofDescriptor("()V"));
                                            }
                                            code.with(element);
                                        }
                                    }));
                        } else {
                            builder.with(element);
                        }
                    });
                    if (count.get() != 1)
                        throw new IllegalStateException("Expected exactly one " + family
                                + " method descriptor; found " + count.get());
                    WriterEpochHook.recordInstrumentedTarget(family);
                    System.out.println("SIMPLECOINS_WRITER_AGENT_" + family
                            + "_EXACT_ENTRY_INSTRUMENTED");
                    return changed;
                } catch (Throwable failure) {
                    // The JVM can otherwise quietly ignore a transformer failure.
                    // Poison the callback bridge and make this visible to preflight.
                    WriterEpochHook.fault();
                    System.err.println("SIMPLECOINS_WRITER_AGENT_" + family + "_TRANSFORM_FAILED");
                    failure.printStackTrace(System.err);
                    return null;
                }
            }
        });
        System.out.println("SIMPLECOINS_WRITER_AGENT_BOOTSTRAP_INSTALLED_PASSIVE");
    }
}
