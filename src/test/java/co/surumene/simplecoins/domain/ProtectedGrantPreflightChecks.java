package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.FutureTask;

/** Executable with JDK only and also from the product JUnit suite. */
public final class ProtectedGrantPreflightChecks {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void denied(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("Expected exclusive protection rejection");
    }
    public static void main(String[] args) throws Exception {
        WriterEpochFence fence = new WriterEpochFence(); fence.arm();
        var seal = fence.seal(); denied(fence::enterRoot);
        fence.withDrainedLease(seal, () -> {
            denied(() -> fence.resume(seal));
            try { fence.withDrainedWriter(seal, () -> null); }
            catch (IllegalStateException expected) { return null; }
            throw new AssertionError("Write entered a read lease");
        });
        fence.withDrainedWriter(seal, () -> {
            check(fence.state() == WriterEpochFence.State.SEALED);
            var root = fence.enterRoot(); var child = fence.enterNested(root);
            FutureTask<Void> other = new FutureTask<>(() -> { denied(fence::enterRoot); return null; });
            new Thread(other).start();
            try { other.get(2, java.util.concurrent.TimeUnit.SECONDS); }
            catch (Exception e) { throw new AssertionError("External root blocked on monitor or admitted", e); }
            denied(() -> fence.resume(seal));
            denied(() -> fence.withDrainedSeal(seal, () -> null));
            child.normalReturn(); root.normalReturn(); return null;
        });
        check(fence.inFlight() == 0); denied(fence::enterRoot);
        fence.requireDrained(seal); fence.resume(seal);
        denied(() -> fence.requireDrained(seal));
        var later = fence.seal();
        try { fence.withDrainedWriter(later, () -> { throw new IOException("save failed"); }); }
        catch (IOException expected) { check(fence.state() == WriterEpochFence.State.FAULTED); }
        denied(() -> fence.resume(later));
        WriterEpochFence escaped = new WriterEpochFence(); escaped.arm();
        var escapedSeal = escaped.seal();
        try { escaped.withDrainedWriter(escapedSeal, () -> { escaped.enterRoot(); return null; }); }
        catch (IllegalStateException expected) { check(escaped.state() == WriterEpochFence.State.FAULTED); }
        denied(() -> escaped.resume(escapedSeal));

        var dir = Files.createTempDirectory("protected-grant-preflight-");
        try {
            GrantLedger ledger = new GrantLedger(dir.resolve("ledger"));
            UUID player = UUID.randomUUID(); var grant = ledger.reserve(player, 5000, "operator");
            var view = ledger.verifiedPlayerSnapshot(player);
            ledger.requireCurrentPlanningView(view);
            var different = new GrantLedger(dir.resolve("different"));
            denied(() -> different.requireCurrentPlanningView(view));
            ledger.reserve(UUID.randomUUID(), 1, "operator");
            denied(() -> ledger.requireCurrentPlanningView(view));
            var beforeAck = ledger.verifiedPlayerSnapshot(player);
            ledger.acknowledge(grant, grant.sequence());
            denied(() -> ledger.requireCurrentPlanningView(beforeAck));
            ledger.requireCurrentPlanningView(ledger.verifiedPlayerSnapshot(player));
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PROTECTED_GRANT_PREFLIGHT_CHECKS_PASSED");
    }
}
