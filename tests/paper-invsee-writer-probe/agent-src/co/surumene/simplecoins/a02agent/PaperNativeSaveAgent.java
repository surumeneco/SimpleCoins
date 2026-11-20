package co.surumene.simplecoins.a02agent;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disposable proof of ONE Paper native save entrypoint, not a G3 writer fence.
 * Uses the standard Java 25 Class-File API; no bundled external/ASM library.
 * The test Agent is not present in the production SimpleCoins plugin JAR.
 */
public final class PaperNativeSaveAgent {
    private PaperNativeSaveAgent() {}

    private static final ClassDesc HOOK_CLASS =
            ClassDesc.of("co.surumene.simplecoins.a02agent.NativeSaveHook");

    public static void premain(String args, Instrumentation instrumentation) {
        // Paper's child classloader cannot see the Agent's system-loader
        // classes. Add ONLY a dedicated hook JAR, not this whole Agent JAR:
        // the latter would bootstrap-load anonymous transformer classes and
        // break their package-private linkage to the app-loaded premain class.
        try {
            String hookJar = System.getProperty("a02.nativeAgent.hookJar");
            if (hookJar == null || hookJar.isBlank())
                throw new IllegalStateException("Missing explicit isolated hook JAR path");
            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(Path.of(hookJar).toFile()));
        } catch (Exception invalid) {
            throw new IllegalStateException(
                    "Cannot expose isolated save hook to Paper classloader", invalid);
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String className,
                    Class<?> redefining, ProtectionDomain protection, byte[] source) {
                boolean craft = "org/bukkit/craftbukkit/entity/CraftPlayer".equals(className);
                boolean nms = "net/minecraft/world/level/storage/PlayerDataStorage".equals(className);
                if (!craft && !nms) return null;
                boolean transformNms = nms
                        && Boolean.getBoolean("a02.nativeAgent.instrumentNmsSave");
                var api = ClassFile.of();
                AtomicInteger modified = new AtomicInteger();
                byte[] result = api.transformClass(api.parse(source), (builder, element) -> {
                    if (element instanceof MethodModel method
                            && ((craft && method.methodName().stringValue().equals("saveData")
                                        && method.methodTypeSymbol().descriptorString().equals("()V"))
                                || (nms && method.methodName().stringValue().equals("save")
                                        && method.methodTypeSymbol().descriptorString().startsWith(
                                            "(Lnet/minecraft/world/entity/player/Player;)")))) {
                        modified.incrementAndGet();
                        if (nms && !transformNms) {
                            builder.with(element);
                            return;
                        }
                        builder.transformMethod(method, MethodTransform.transformingCode(
                                new CodeTransform() {
                                    @Override public void atStart(CodeBuilder code) {
                                        code.invokestatic(HOOK_CLASS,
                                                nms ? "beforeNmsSave" : "beforeNativeSave",
                                                MethodTypeDesc.ofDescriptor("()V"));
                                    }

                                    @Override public void accept(CodeBuilder code, CodeElement element) {
                                        code.with(element);
                                    }
                                }));
                    } else {
                        builder.with(element);
                    }
                });
                if (modified.get() != 1)
                    throw new IllegalStateException(
                            "Unexpected Paper " + (nms ? "NMS PlayerDataStorage.save"
                                    : "CraftPlayer.saveData")
                                    + " method count " + modified.get());
                if (nms) {
                    System.out.println("PAPER_A02_NATIVE_AGENT_NMS_SAVE_ENTRY_FOUND");
                    if (transformNms) System.out.println(
                            "PAPER_A02_NATIVE_AGENT_NMS_SAVE_ENTRY_INSTRUMENTED");
                    return transformNms ? result : null;
                }
                System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_ENTRY_INSTRUMENTED");
                return result;
            }
        });
        System.out.println("PAPER_A02_NATIVE_AGENT_TEST_ONLY_INSTALLED");
    }

}
