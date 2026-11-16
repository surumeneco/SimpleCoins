import co.surumene.simplecoins.domain.GrantLedger;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Disposable CI fixture only: produce a valid ACKed journal record but no
 * saved playerdata, to prove guarded Paper startup detects that divergence.
 * Not packaged into the product or run in an actual user world.
 */
public final class SeedAcknowledgedWithoutPlayerdata {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected isolated journal path");
        var ledger = new GrantLedger(Path.of(args[0]));
        var player = UUID.fromString("00000000-0000-0000-0000-00000000a002");
        var grant = ledger.reserve(player, 5000, "test-fixture");
        ledger.acknowledge(grant, grant.sequence());
        if (ledger.historySnapshot().size() != 1
                || !ledger.pending(player).isEmpty()
                || !ledger.historySnapshot().getFirst().delivered())
            throw new IllegalStateException("Failed to create a valid persisted ACK");
        System.out.println("A02_VALID_ACK_WITHOUT_PLAYERDATA_SEEDED");
    }
}
