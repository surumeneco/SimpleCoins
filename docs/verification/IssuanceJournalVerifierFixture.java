import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.GrantLedger;
import java.nio.file.Path;
import java.util.UUID;

/** Uses the actual production journal writer for cross-language verifier tests. */
public final class IssuanceJournalVerifierFixture {
    private IssuanceJournalVerifierFixture() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected journal output directory");
        var ledger = new GrantLedger(Path.of(args[0]));
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        var acknowledged = ledger.reserve(firstPlayer, 100, "管理者");
        ledger.reserveCoins(secondPlayer, CoinDenomination.FIVE, 3, "console");
        ledger.acknowledge(acknowledged, acknowledged.sequence());
    }
}
