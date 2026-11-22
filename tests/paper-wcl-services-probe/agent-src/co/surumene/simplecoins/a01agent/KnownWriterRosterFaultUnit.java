package co.surumene.simplecoins.a01agent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure-JVM monitor protocol check. Calls below are SYNTHETIC, not evidence of
 * actual Paper instrumented method executions. The real Paper probe remains
 * responsible for checking the five shipped entrypoints and their returns.
 */
public final class KnownWriterRosterFaultUnit {
    private KnownWriterRosterFaultUnit() {}

    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }

    private static void rejected(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Unsafe source-roster operation unexpectedly succeeded");
    }

    public static void main(String[] args) {
        if (args.length != 1 || (!"open".equals(args[0]) && !"closed".equals(args[0])))
            throw new IllegalArgumentException("Expected open or closed fixture");
        for (String writer : new String[]{"WWC", "WWW", "PDC", "EQUIPMENT", "INVENTORY"})
            LiveOwnerWriterHook.registerObservedTransformation(writer);
        // Unit-only scope accounting. This MUST NOT be interpreted as real
        // transformed WWC/WWW/Paper class return evidence.
        LiveOwnerWriterHook.beforeWWC();
        LiveOwnerWriterHook.afterWWC();
        LiveOwnerWriterHook.beforeWWW();
        LiveOwnerWriterHook.afterWWW();
        LiveOwnerWriterHook.beforePdcMutation();
        LiveOwnerWriterHook.afterPdcMutation();
        LiveOwnerWriterHook.beforeEquipmentMutation();
        LiveOwnerWriterHook.afterEquipmentMutation();
        LiveOwnerWriterHook.beforeInventoryMutation();
        LiveOwnerWriterHook.afterInventoryMutation();
        require(LiveOwnerWriterHook.knownCoverageComplete(), "Synthetic monitor setup failed");

        LiveOwnerWriterHook.KnownSourcesClosedEpoch oldProof = null;
        if ("closed".equals(args[0])) {
            LiveOwnerWriterHook.closeForProbe();
            oldProof = LiveOwnerWriterHook.mintKnownSourcesClosedEpoch();
            require(LiveOwnerWriterHook.knownEpochStillCurrent(oldProof),
                    "Synthetic closed generation was not established");
        }
        final var prior = oldProof;

        // A late/duplicate transformer is a loss-of-coverage event, not only
        // an exception to be logged while retaining the earlier true state.
        rejected(() -> LiveOwnerWriterHook.registerObservedTransformation("WWC"));
        require(LiveOwnerWriterHook.isClosed(), "Rejected registration left gate open");
        require(!LiveOwnerWriterHook.knownCoverageComplete(),
                "Rejected registration retained previously complete writer roster");
        if (prior != null) {
            require(!LiveOwnerWriterHook.knownEpochStillCurrent(prior),
                    "Already minted closed epoch survived registration fault");
        }
        rejected(LiveOwnerWriterHook::mintKnownSourcesClosedEpoch);
        AtomicInteger executed = new AtomicInteger();
        rejected(() -> LiveOwnerWriterHook.runUnderKnownSourcesClosedEpoch(
                prior, executed::incrementAndGet));
        require(executed.get() == 0, "Faulted source roster executed a guarded action");
        rejected(LiveOwnerWriterHook::reopenForProbe);
        require(LiveOwnerWriterHook.isClosed(), "Faulted source roster was reopened");
        System.out.println("PAPER_A01_SOURCE_ROSTER_FAULT_UNIT_" + args[0].toUpperCase()
                + "_CLOSED_REVOKED_PASSED");
    }
}
