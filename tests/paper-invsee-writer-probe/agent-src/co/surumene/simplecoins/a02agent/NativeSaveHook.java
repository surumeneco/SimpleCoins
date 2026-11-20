package co.surumene.simplecoins.a02agent;

/**
 * Minimal bootstrap-visible target for the test-only Paper save gate.
 * This class has no references to plugins, agent transformers or Paper APIs.
 * It does NOT certify all native save or external writer entrypoints.
 */
public final class NativeSaveHook {
    private NativeSaveHook() {}

    public static void beforeNativeSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_CRAFTPLAYER_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only Paper native save rejected during closed proof interval");
        }
    }

    /** Separate NMS internal playerdata writer entry, not a universal IO fence. */
    public static void beforeNmsSave() {
        if (Boolean.getBoolean("a02.nativeAgent.closed")) {
            System.out.println("PAPER_A02_NATIVE_AGENT_NMS_SAVE_DENIED");
            throw new IllegalStateException(
                    "A02 test-only NMS PlayerDataStorage save rejected during closed interval");
        }
    }
}
