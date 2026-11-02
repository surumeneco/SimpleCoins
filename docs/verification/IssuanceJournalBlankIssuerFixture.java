import co.surumene.simplecoins.domain.GrantLedger;
import java.nio.file.Path;
import java.util.UUID;

/** Actual Java journal writer for the issuer blankness compatibility regression. */
public final class IssuanceJournalBlankIssuerFixture {
    private IssuanceJournalBlankIssuerFixture() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected journal output directory");
        String issuer = "\u00a0";
        if (issuer.isBlank()) throw new AssertionError("Java unexpectedly rejects nonbreaking space");
        new GrantLedger(Path.of(args[0])).reserve(UUID.randomUUID(), 100, issuer);
    }
}
