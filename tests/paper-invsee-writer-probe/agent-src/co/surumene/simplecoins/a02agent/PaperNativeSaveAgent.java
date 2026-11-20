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
            ClassDesc.of("co.surumene.simplecoins.a02agent.PaperNativeSaveAgent");

    public static void premain(String args, Instrumentation instrumentation) {
        // Paper's plugin/server classloaders need not delegate to the loader
        // that resolved premain. Make the hook visible from bootstrap so that
        // instrumented CraftPlayer has a resolvable INVOKESTATIC target.
        // This is test-only; no third-party JAR or Paper class is overwritten.
        try {
            var source = PaperNativeSaveAgent.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(Path.of(source).toFile()));
        } catch (Exception invalid) {
            throw new IllegalStateException(
                    "Cannot expose native save hook to Paper classloaders", invalid);
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String className,
                    Class<?> redefining, ProtectionDomain protection, byte[] source) {
                if (!"org/bukkit/craftbukkit/entity/CraftPlayer".equals(className))
                    return null;
                var api = ClassFile.of();
                AtomicInteger modified = new AtomicInteger();
                byte[] result = api.transformClass(api.parse(source), (builder, element) -> {
                    if (element instanceof MethodModel method
                            && method.methodName().stringValue().equals("saveData")
                            && method.methodTypeSymbol().descriptorString().equals("()V")) {
                        modified.incrementAndGet();
                        builder.transformMethod(method, MethodTransform.transformingCode(
                                new CodeTransform() {
                                    @Override public void atStart(CodeBuilder code) {
                                        code.invokestatic(HOOK_CLASS, "beforeNativeSave",
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
                            "Unexpected Paper CraftPlayer.saveData method count " + modified.get());
                System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_ENTRY_INSTRUMENTED");
                return result;
            }
        });
        System.out.println("PAPER_A02_NATIVE_AGENT_TEST_ONLY_INSTALLED");
    }

    /** Refuse the selected entry before the method body reaches playerdata IO. */
    public static void beforeNativeSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only Paper native save rejected during closed proof interval");
        }
    }
}
