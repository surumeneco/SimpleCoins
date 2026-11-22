package co.surumene.simplecoins.a01agent;

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
 * Test-only interception of two loaded WCL owner writers plus one low-level Paper PDC mutation surface.
 * Neither a production agent nor proof of a complete G0 writer epoch.
 */
public final class LiveOwnerWriterAgent {
    private static final ClassDesc HOOK =
            ClassDesc.of("co.surumene.simplecoins.a01agent.LiveOwnerWriterHook");

    private LiveOwnerWriterAgent() {}

    public static void premain(String args, Instrumentation instrumentation) {
        try {
            String path = System.getProperty("a01.liveOwnerAgent.hookJar");
            if (path == null || path.isBlank())
                throw new IllegalStateException("Missing exact bootstrap hook JAR");
            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(Path.of(path).toFile()));
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot publish isolated test hook", failure);
        }
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override public byte[] transform(ClassLoader loader, String name,
                    Class<?> redefining, ProtectionDomain protection, byte[] bytes) {
                String method;
                String descriptor;
                String label;
                boolean pdc;
                if ("co/surumene/whatawonderfulchicken/service/WonderfulChickenStore"
                        .equals(name)) {
                    label = "WWC";
                    pdc = false;
                    method = "save";
                    descriptor = "(Lorg/bukkit/entity/Chicken;Lco/surumene/"
                            + "whatawonderfulchicken/data/WonderfulChickenData;)V";
                } else if ("co/surumene/www/persistence/WonderfulWolfLoadedIndividuals"
                        .equals(name)) {
                    label = "WWW";
                    pdc = false;
                    method = "saveAndRegister";
                    descriptor = "(Lorg/bukkit/entity/Wolf;Lco/surumene/www/"
                            + "individual/WonderfulWolfIndividual;)V";
                } else if ("org/bukkit/craftbukkit/persistence/CraftPersistentDataContainer"
                        .equals(name) && Boolean.getBoolean("a01.liveOwnerAgent.instrumentPdc")) {
                    label = "PDC";
                    method = null;
                    descriptor = null;
                    pdc = true;
                } else {
                    return null;
                }
                try {
                    var hierarchy = ClassHierarchyResolver.ofResourceParsing(loader)
                            .orElse(ClassHierarchyResolver.defaultResolver());
                    var api = ClassFile.of(
                            ClassFile.ClassHierarchyResolverOption.of(hierarchy));
                    AtomicInteger count = new AtomicInteger();
                    byte[] transformed = api.transformClass(api.parse(bytes), (builder, element) -> {
                        if (element instanceof MethodModel m
                                && (pdc
                                    ? (m.methodName().stringValue().equals("set")
                                        && m.methodTypeSymbol().descriptorString().equals(
                                            "(Lorg/bukkit/NamespacedKey;Lorg/bukkit/persistence/"
                                                    + "PersistentDataType;Ljava/lang/Object;)V"))
                                      || (m.methodName().stringValue().equals("remove")
                                        && m.methodTypeSymbol().descriptorString().equals(
                                            "(Lorg/bukkit/NamespacedKey;)V"))
                                    : (m.methodName().stringValue().equals(method)
                                       && m.methodTypeSymbol().descriptorString().equals(descriptor)))) {
                            count.incrementAndGet();
                            builder.transformMethod(m, MethodTransform.transformingCode(
                                    new CodeTransform() {
                                        @Override public void atStart(CodeBuilder code) {
                                            code.invokestatic(HOOK,
                                                    pdc ? "beforePdcMutation" : "before" + label,
                                                    MethodTypeDesc.ofDescriptor("()V"));
                                        }
                                        @Override public void accept(CodeBuilder code, CodeElement e) {
                                            if (e instanceof ReturnInstruction) {
                                                code.invokestatic(HOOK,
                                                        pdc ? "afterPdcMutation" : "after" + label,
                                                        MethodTypeDesc.ofDescriptor("()V"));
                                            }
                                            code.with(e);
                                        }
                                    }));
                        } else {
                            builder.with(element);
                        }
                    });
                    if (count.get() != (pdc ? 2 : 1))
                        throw new IllegalStateException("Expected " + (pdc ? 2 : 1)
                                + " exact " + label + " mutation method(s), found " + count.get());
                    System.out.println("PAPER_A01_LIVE_AGENT_" + label + "_WRITER_INSTRUMENTED");
                    return transformed;
                } catch (Throwable failure) {
                    System.err.println("PAPER_A01_LIVE_AGENT_" + label + "_TRANSFORM_FAILED");
                    failure.printStackTrace(System.err);
                    return null; // CI requires marker; silent instrumentation failure is rejected
                }
            }
        });
        System.out.println("PAPER_A01_LIVE_AGENT_TEST_ONLY_INSTALLED");
    }
}
