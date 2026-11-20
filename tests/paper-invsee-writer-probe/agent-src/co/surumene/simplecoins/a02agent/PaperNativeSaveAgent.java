package co.surumene.simplecoins.a02agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * Disposable proof of one Paper native save entrypoint, not a complete G3
 * writer fence. This Java agent is NOT packaged in the SimpleCoins plugin.
 *
 * It depends on a launch-time JVM flag and JDK-internal ASM access; real
 * deployment needs explicit architectural approval and all-writer coverage.
 */
public final class PaperNativeSaveAgent {
    private PaperNativeSaveAgent() {}

    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(
                    ClassLoader loader, String className, Class<?> redefining,
                    ProtectionDomain protection, byte[] source) {
                if (!"org/bukkit/craftbukkit/entity/CraftPlayer".equals(className))
                    return null;
                ClassReader reader = new ClassReader(source);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                AtomicInteger modified = new AtomicInteger();
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override public MethodVisitor visitMethod(
                            int access, String name, String desc,
                            String signature, String[] exceptions) {
                        MethodVisitor original = super.visitMethod(
                                access, name, desc, signature, exceptions);
                        if (!"saveData".equals(name) || !"()V".equals(desc))
                            return original;
                        modified.incrementAndGet();
                        return new MethodVisitor(Opcodes.ASM9, original) {
                            @Override public void visitCode() {
                                super.visitCode();
                                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                        "co/surumene/simplecoins/a02agent/PaperNativeSaveAgent",
                                        "beforeNativeSave", "()V", false);
                            }
                        };
                    }
                }, 0);
                if (modified.get() != 1)
                    throw new IllegalStateException(
                            "Unexpected Paper CraftPlayer.saveData method count "
                                    + modified.get());
                System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_ENTRY_INSTRUMENTED");
                return writer.toByteArray();
            }
        });
        System.out.println("PAPER_A02_NATIVE_AGENT_TEST_ONLY_INSTALLED");
    }

    /** Denies a call before CraftPlayer.saveData reaches its original body. */
    public static void beforeNativeSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only Paper native save rejected during closed proof interval");
        }
    }
}
