package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Durable partial progress is a lower bound on all future receipt observations. */
public final class GrantPartialCustodyChecks {
    @FunctionalInterface interface Operation { void run() throws Exception; }
    private static void reject(Operation op) throws Exception {
        try { op.run(); } catch (IOException | IllegalStateException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected custody rejection");
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError("Custody mismatch"); }
    public static void run(Path root) throws Exception {
        UUID player = UUID.randomUUID(); Path dir = root.resolve("ledger"), file = root.resolve(player + ".dat");
        GrantLedger ledger = new GrantLedger(dir);
        var grant = ledger.reserveCoins(player, CoinDenomination.fromValue(1), 130, "console");
        var initial = ledger.verifiedPlayerSnapshot(player);
        var first = new GrantReceiptState(0, grant.sequence(), grant.id(), 64);
        var second = new GrantReceiptState(0, grant.sequence(), grant.id(), 128);
        ledger.writeBatchIntent(initial, grant, file, new byte[]{1,2}, 64);
        reject(() -> ledger.confirmPartial(grant, GrantReceiptState.empty(), new GrantReceiptState(0, grant.sequence(), grant.id(), 63)));
        ledger.confirmPartial(grant, GrantReceiptState.empty(), first);
        reject(() -> ledger.requireCurrentPlanningView(initial));
        GrantLedger reopened = new GrantLedger(dir);
        var current = reopened.pending(player).getFirst();
        check(current.confirmedPartial() == 64 && !current.delivered());
        var view = reopened.verifiedPlayerSnapshot(player);
        reject(() -> GrantReceiptState.empty().verify(view));
        reject(() -> new GrantReceiptState(0, current.sequence(), UUID.randomUUID(), 64).verify(view));
        first.verify(view); second.verify(view);
        var obsolete = reopened.readBatchIntent(view, current, file, 64).orElseThrow();
        check(obsolete.priorValue() == 0);
        reject(() -> reopened.removeConfirmedBatchIntent(current, file, 64)); // not the stored intent boundary
        reopened.removeConfirmedBatchIntent(current, file, 0);
        check(reopened.readBatchIntent(view, current, file, 64).isEmpty());
        reopened.writeBatchIntent(view, current, file, new byte[]{3,4}, 64);
        check(reopened.readBatchIntent(view, current, file, 64).orElseThrow().priorValue() == 64);
        reject(() -> reopened.confirmPartial(current, GrantReceiptState.empty(), second));
        reopened.confirmPartial(current, first, second);
        var again = new GrantLedger(dir);
        var pending = again.pending(player).getFirst();
        check(pending.confirmedPartial() == 128);
        reject(() -> first.verify(again.verifiedPlayerSnapshot(player)));
        second.verify(again.verifiedPlayerSnapshot(player));
        again.removeConfirmedBatchIntent(pending, file, 64);
        var finalView = again.verifiedPlayerSnapshot(player);
        again.writeBatchIntent(finalView, pending, file, new byte[]{5,6}, 64);
        var completed = new GrantReceiptState(pending.sequence(), 0, null, 0);
        completed.verify(finalView);
        again.acknowledge(pending, pending.sequence());
        again.removeAcknowledgedFirstBatchIntent(pending);
        var end = new GrantLedger(dir);
        check(end.pending(player).isEmpty() && end.historySnapshot().getFirst().delivered());
        completed.verify(end.verifiedPlayerSnapshot(player));
        reject(() -> second.verify(end.verifiedPlayerSnapshot(player)));
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("simplecoins-partial-custody-");
        try { run(root); System.out.println("GRANT_PARTIAL_CUSTODY_CHECKS_PASSED"); }
        finally { try (var walk = Files.walk(root)) { for (Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); } }
    }
}
