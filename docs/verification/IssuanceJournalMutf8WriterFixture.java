import co.surumene.simplecoins.domain.GrantLedger;
import java.nio.file.Path;
import java.util.UUID;

/** Writes actual GrantLedger SCG2 records at Java Modified UTF-8 issuer boundaries. */
public final class IssuanceJournalMutf8WriterFixture {
    private IssuanceJournalMutf8WriterFixture() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected journal dir and case");
        String supplementary = new String(Character.toChars(0x1F600));
        String issuer = switch (args[1]) {
            case "nul" -> String.valueOf((char) 0);
            case "supplementary" -> "x" + supplementary;
            case "max-utf16" -> "x".repeat(1022) + supplementary;
            case "too-long" -> "x".repeat(1023) + supplementary;
            default -> throw new IllegalArgumentException("Unknown case");
        };
        GrantLedger ledger = new GrantLedger(Path.of(args[0]));
        if ("too-long".equals(args[1])) {
            if (issuer.length() != 1025) throw new AssertionError("Wrong boundary");
            try {
                ledger.reserve(UUID.randomUUID(), 100, issuer);
                throw new AssertionError("Java writer accepted an overlong issuer");
            } catch (IllegalArgumentException expected) {
                // The writer must reject before persisting any grant.
            }
            return;
        }
        if ("max-utf16".equals(args[1]) && issuer.length() != 1024)
            throw new AssertionError("Wrong Java UTF-16 length");
        UUID player = UUID.randomUUID();
        GrantLedger.Grant written = ledger.reserve(player, 100, issuer);
        var reopened = new GrantLedger(Path.of(args[0]));
        var replayed = reopened.pending(player);
        if (replayed.size() != 1 || replayed.get(0).sequence() != written.sequence()
                || !issuer.equals(replayed.get(0).issuer()))
            throw new AssertionError("Java writer/replay changed the issuer");
    }
}
