package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/** Direct Java test of immutable, reservation-bound durable prior-state custody. */
public final class GrantFirstBatchIntentChecks {
    @FunctionalInterface interface Operation { void run() throws Exception; }
    private static void reject(Operation op) throws Exception {
        try { op.run(); } catch (IOException | IllegalStateException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected fail-closed intent rejection");
    }
    public static void run(Path root) throws Exception {
        UUID player = UUID.randomUUID(), other = UUID.randomUUID();
        Path dir = root.resolve("grants"), source = root.resolve(player + ".dat");
        GrantLedger ledger = new GrantLedger(dir);
        var grant = ledger.reserve(player, 5200, "console");
        var view = ledger.verifiedPlayerSnapshot(player);
        byte[] before = {1, 2, 3, 4};
        if (ledger.readFirstBatchIntent(view, grant, source, 64).isPresent())
            throw new AssertionError("Unexpected prior intent");
        ledger.writeFirstBatchIntent(view, grant, source, before, 64);
        reject(() -> ledger.writeFirstBatchIntent(view, grant, source, new byte[]{9}, 64));
        before[0] = 99;
        GrantLedger reopened = new GrantLedger(dir);
        var fresh = reopened.verifiedPlayerSnapshot(player);
        byte[] observed = reopened.readFirstBatchIntent(fresh, grant, source, 64).orElseThrow();
        if (!Arrays.equals(observed, new byte[]{1,2,3,4})) throw new AssertionError("Intent changed");
        reject(() -> reopened.readFirstBatchIntent(view, grant, source, 64));
        reject(() -> reopened.readFirstBatchIntent(fresh, grant, root.resolve("different").resolve(player + ".dat"), 64));
        reject(() -> reopened.readFirstBatchIntent(fresh, grant, source, 3));
        var second = reopened.reserve(other, 1, "console");
        reject(() -> reopened.readFirstBatchIntent(fresh, grant, source, 64));
        var current = reopened.verifiedPlayerSnapshot(player);
        reject(() -> reopened.readFirstBatchIntent(current, second, root.resolve(other+".dat"), 64));
        Path intent = dir.resolve(grant.sequence()+".intent");
        byte[] intact = Files.readAllBytes(intent);
        byte[] corrupt = intact.clone(); corrupt[corrupt.length-1] ^= 1;
        Files.write(intent, corrupt);
        reject(() -> reopened.readFirstBatchIntent(current, grant, source, 64));
        Files.write(intent, intact);
        reopened.acknowledge(grant, grant.sequence());
        reopened.removeAcknowledgedFirstBatchIntent(grant);
        if (Files.exists(intent)) throw new AssertionError("Acknowledged intent not removed");
        reject(() -> reopened.readFirstBatchIntent(reopened.verifiedPlayerSnapshot(player), grant, source, 64));
        reject(() -> reopened.removeAcknowledgedFirstBatchIntent(second));
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("simplecoins-grant-intent-");
        try { run(root); System.out.println("GRANT_FIRST_BATCH_INTENT_CHECKS_PASSED"); }
        finally { try (var walk = Files.walk(root)) { for (Path file : walk.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file); } }
    }
}
