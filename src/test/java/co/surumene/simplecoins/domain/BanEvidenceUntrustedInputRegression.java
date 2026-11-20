package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Dependency-free regression executable for untrusted BAN evidence boundaries.
 * Run with javac/java against BanEvidence.java; this does not establish CAT approval.
 */
public final class BanEvidenceUntrustedInputRegression {
    private BanEvidenceUntrustedInputRegression() {}

    public static void main(String[] args) throws Exception {
        int checks = 0;
        expectIOException(Set.of("ban"), List.of(new BanEvidence.Source("other", BanEvidence.Status.BANNED)));
        checks++;
        expectIOException(Set.of("ban"), List.of(
                new BanEvidence.Source("ban", BanEvidence.Status.BANNED),
                new BanEvidence.Source("BAN", BanEvidence.Status.CLEAR)));
        checks++;
        expectIOException(Set.of("ban"), Arrays.asList((BanEvidence.Source) null));
        checks++;
        expectIOException(Set.of("ban"), List.of(new BanEvidence.Source("ban", null)));
        checks++;
        expectIOException(Set.of("ban", "ipban"), List.of(
                new BanEvidence.Source("ban", BanEvidence.Status.BANNED),
                new BanEvidence.Source("unexpected", BanEvidence.Status.UNKNOWN)));
        checks++;
        if (BanEvidence.combine(Set.of("ban", "ipban"),
                List.of(new BanEvidence.Source("ban", BanEvidence.Status.CLEAR)))
                != BanEvidence.Status.UNKNOWN) {
            throw new AssertionError("Absent required evidence must remain UNKNOWN");
        }
        checks++;
        if (BanEvidence.combine(Set.of("ban", "ipban"),
                List.of(new BanEvidence.Source("ban", BanEvidence.Status.BANNED)))
                != BanEvidence.Status.BANNED) {
            throw new AssertionError("Known BAN must remain BANNED even if another source is absent");
        }
        checks++;
        System.out.println("PASS BanEvidenceUntrustedInputRegression checks=" + checks);
    }

    private static void expectIOException(Set<String> required, List<BanEvidence.Source> observations)
            throws Exception {
        try {
            BanEvidence.combine(required, observations);
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Untrusted or contradictory BAN evidence was accepted");
    }
}
