package co.surumene.simplecoins.domain;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Product-side callback for the isolated Java 25 startup Agent.
 *
 * <p>Binding only supplies method handles. It MUST NOT arm the fence, enable
 * the Agent's bounded observation window, publish G0, acknowledge G3, or
 * authorize Admission; the complete effective writer roster is unproved.</p>
 */
public final class PaperNativeSaveFenceAdapter {
    private final WriterEpochFence fence;

    public PaperNativeSaveFenceAdapter(WriterEpochFence fence) {
        this.fence = Objects.requireNonNull(fence, "writer fence");
    }

    /** Called by the bootstrap hook only while an independently authorized epoch is armed. */
    public Object enter(String target, Object parent) {
        return switch (Objects.requireNonNull(target, "writer target")) {
            case "CRAFT" -> {
                if (parent != null) throw new IllegalStateException("Nested Craft root is not proven");
                yield fence.enterRoot();
            }
            case "NMS" -> {
                if (parent == null) yield fence.enterRoot();
                if (!(parent instanceof WriterEpochFence.Entry entry))
                    throw new IllegalStateException("Unknown NMS parent token");
                yield fence.enterNested(entry);
            }
            case "PDC", "INVENTORY", "EQUIPMENT" -> {
                if (parent == null) yield fence.enterRoot();
                if (!(parent instanceof WriterEpochFence.Entry entry))
                    throw new IllegalStateException("Unrecognized nested Paper inventory/PDC/equipment writer token");
                yield fence.enterNested(entry);
            }
            case "WWC", "WWW" -> {
                if (parent != null)
                    throw new IllegalStateException("New WCL owner writer cannot inherit unrelated ancestry");
                yield fence.enterRoot();
            }
            default -> throw new IllegalArgumentException("Unknown observed writer target");
        };
    }

    public void normalReturn(Object token) {
        if (!(token instanceof WriterEpochFence.Entry entry))
            throw new IllegalArgumentException("Unrecognized writer entry token");
        entry.normalReturn();
    }

    public void exceptionalReturn(Object token) {
        if (!(token instanceof WriterEpochFence.Entry entry))
            throw new IllegalArgumentException("Unrecognized writer entry token");
        entry.exceptionalReturn();
    }

    /** Revoke every future use of a product Seal if the bootstrap Agent faults. */
    public void invalidateFromAgent() { fence.invalidate(); }

    /**
     * Bind to a bootstrap-loader class only if the actual separate Agent exists.
     * No explicit Agent means default-deny settlement remains in force.
     */
    public boolean bindIfAgentPresent() {
        final Class<?> bootstrapHook;
        try {
            bootstrapHook = Class.forName(
                    "co.surumene.simplecoins.writeragent.WriterEpochHook", false, null);
        } catch (ClassNotFoundException absent) {
            return false;
        }
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle enter = lookup.findVirtual(PaperNativeSaveFenceAdapter.class,
                    "enter", MethodType.methodType(Object.class, String.class, Object.class))
                    .bindTo(this);
            MethodHandle normal = lookup.findVirtual(PaperNativeSaveFenceAdapter.class,
                    "normalReturn", MethodType.methodType(void.class, Object.class))
                    .bindTo(this);
            MethodHandle exceptional = lookup.findVirtual(PaperNativeSaveFenceAdapter.class,
                    "exceptionalReturn", MethodType.methodType(void.class, Object.class))
                    .bindTo(this);
            MethodHandle invalidate = lookup.findVirtual(PaperNativeSaveFenceAdapter.class,
                    "invalidateFromAgent", MethodType.methodType(void.class)).bindTo(this);
            bootstrapHook.getMethod("bindProductCallbacks", MethodHandle.class,
                    MethodHandle.class, MethodHandle.class, MethodHandle.class)
                    .invoke(null, enter, normal, exceptional, invalidate);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException failure) {
            throw new IllegalStateException("Writer Agent bootstrap bridge binding failed", failure);
        }
    }
}
