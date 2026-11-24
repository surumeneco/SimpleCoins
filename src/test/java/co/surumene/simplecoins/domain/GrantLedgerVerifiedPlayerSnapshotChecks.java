package co.surumene.simplecoins.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Real durable records, runnable without a Paper/JUnit classpath. */
public final class GrantLedgerVerifiedPlayerSnapshotChecks {
    private GrantLedgerVerifiedPlayerSnapshotChecks() {}
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("sc-verified-receipt-view-");
        try { coherentPlanning(temp); integrityStillRequired(temp); coherentUniverse(temp); }
        finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("GRANT_LEDGER_VERIFIED_PLAYER_SNAPSHOT_CHECKS_PASSED");
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejected(Runnable run) {
        try { run.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("Expected invalid receipt/ledger rejection");
    }
    private static void coherentPlanning(Path temp) throws Exception {
        UUID player = UUID.randomUUID(), foreign = UUID.randomUUID();
        var ledger = new GrantLedger(temp.resolve("planning"));
        var first = ledger.reserve(player, 5200, "console");
        var other = ledger.reserve(foreign, 500, "console");
        var later = ledger.reserveCoins(player, CoinDenomination.FIVE_THOUSAND, 2, "console");
        var view = ledger.verifiedPlayerSnapshot(player);
        check(view.player().equals(player));
        check(view.pending().equals(List.of(first, later)));
        try { view.pending().clear(); throw new AssertionError("mutable pending view"); }
        catch (UnsupportedOperationException expected) { }
        var empty = GrantReceiptState.empty();
        rejected(() -> empty.begin(view, other));
        rejected(() -> empty.begin(view, later));
        rejected(() -> new GrantReceiptState(other.sequence(), 0, null, 0).verify(view));
        var started = empty.begin(view, first);
        var partial = started.advance(view, first, GrantDeliveryBatchPlanner.next(first, 0, 1));
        check(partial.partialValue() == 5000);
        rejected(() -> partial.complete(view, first));
        var forged = new GrantLedger.Grant(first.sequence(), first.id(), player, 5000,
                first.issuer(), first.createdMillis(), false, CoinDenomination.FIVE_THOUSAND, 1);
        rejected(() -> started.advance(view, forged, GrantDeliveryBatchPlanner.next(forged, 0, 1)));
        var done = partial.advance(view, first, GrantDeliveryBatchPlanner.next(first, 5000, 1)).complete(view, first);
        check(done.completedSequence() == first.sequence() && !done.active());
        check(ledger.pending(player).equals(List.of(first, later))); // read-only staging
        ledger.acknowledge(first, done.completedSequence()); // existing low-level API, not a physical save proof
        var appended = ledger.reserve(player, 10, "console");
        check(view.pending().equals(List.of(first, later)));
        rejected(() -> done.begin(view, appended));
        var current = ledger.verifiedPlayerSnapshot(player);
        check(current.pending().equals(List.of(later, appended)));
        rejected(() -> empty.verify(current)); // earlier ACK prevents rollback
        var next = done.begin(current, later);
        var complete = next.advance(current, later, GrantDeliveryBatchPlanner.next(later, 0, 1)).complete(current, later);
        check(complete.completedSequence() == later.sequence());
        check(new GrantLedger(temp.resolve("planning")).pending(player).equals(List.of(later, appended)));
    }
    private static void integrityStillRequired(Path temp) throws Exception {
        UUID player = UUID.randomUUID(), foreign = UUID.randomUUID();
        Path root = temp.resolve("corrupt"); var ledger = new GrantLedger(root);
        var unrelated = ledger.reserve(foreign, 10, "console");
        var actual = ledger.reserve(player, 5200, "console");
        var captured = ledger.verifiedPlayerSnapshot(player);
        Files.delete(root.resolve(unrelated.sequence() + ".grant"));
        // Immutable earlier view is a read-only candidate, not a live integrity proof.
        check(GrantReceiptState.empty().begin(captured, actual).active());
        rejected(() -> ledger.verifiedPlayerSnapshot(player));
        rejected(() -> GrantReceiptState.empty().begin(ledger, player, actual));
        try { ledger.acknowledge(actual, actual.sequence()); throw new AssertionError("corrupt ledger ACK"); }
        catch (IllegalStateException expected) { }
        check(!Files.exists(root.resolve(unrelated.sequence() + ".grant")));
    }
    private static void coherentUniverse(Path temp) throws Exception {
        Path root = temp.resolve("universe");
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), unknown = UUID.randomUUID();
        var ledger = new GrantLedger(root);
        var first = ledger.reserve(a, 5000, "console");
        var second = ledger.reserve(b, 10, "console");
        var third = ledger.reserve(a, 100, "console");
        var universe = ledger.verifiedUniverseSnapshot();
        check(universe.history().equals(List.of(first, second, third)));
        check(universe.forPlayer(a).pending().equals(List.of(first, third)));
        check(universe.forPlayer(b).pending().equals(List.of(second)));
        check(universe.forPlayer(unknown).pending().isEmpty());
        GrantReceiptState.empty().verify(universe.forPlayer(unknown));
        rejected(() -> new GrantReceiptState(second.sequence(), 0, null, 0).verify(universe.forPlayer(a)));
        try { universe.history().clear(); throw new AssertionError("mutable history"); }
        catch (UnsupportedOperationException expected) { }
        ledger.requireUnchangedVerifiedUniverse(universe);
        var foreign = new GrantLedger(temp.resolve("foreign-universe"));
        rejected(() -> foreign.requireUnchangedVerifiedUniverse(universe));
        ledger.acknowledge(first, first.sequence());
        rejected(() -> ledger.requireUnchangedVerifiedUniverse(universe));
        var fresh = ledger.verifiedUniverseSnapshot();
        check(fresh.forPlayer(a).pending().equals(List.of(third)));
        rejected(() -> GrantReceiptState.empty().verify(fresh.forPlayer(a)));
        ledger.requireUnchangedVerifiedUniverse(fresh);
        Files.delete(root.resolve(second.sequence() + ".grant"));
        rejected(() -> ledger.requireUnchangedVerifiedUniverse(fresh));
        rejected(() -> ledger.verifiedUniverseSnapshot());
    }
}
