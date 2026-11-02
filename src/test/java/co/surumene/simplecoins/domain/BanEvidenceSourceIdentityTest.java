package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Draft BAN-IDENTITY vectors; no external provider discovery is assumed. */
class BanEvidenceSourceIdentityTest {
    private static BanEvidence.Source source(String id, BanEvidence.Status status) {
        return new BanEvidence.Source(id, status);
    }

    @Test void canonicalIdentityMatchesCaseAndOuterWhitespace() throws Exception {
        assertEquals(BanEvidence.Status.CLEAR, BanEvidence.combine(
                Set.of(" paper-profile ", "EXTERNAL:Plugin"),
                List.of(source("PAPER-PROFILE", BanEvidence.Status.CLEAR),
                        source(" external:plugin ", BanEvidence.Status.CLEAR))));
    }

    @Test void duplicateRequiredSourceAliasesAreRejected() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("external:Plugin", " EXTERNAL:plugin "),
                List.of(source("external:plugin", BanEvidence.Status.CLEAR))));
    }

    @Test void missingAndMalformedRequiredIdentitiesAreRejected() {
        assertThrows(IOException.class, () -> BanEvidence.combine(null, List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of(), List.of()));
        Set<String> nullId = new HashSet<>();
        nullId.add(null);
        assertThrows(IOException.class, () -> BanEvidence.combine(nullId, List.of()));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("   "), List.of()));
    }

    @Test void duplicateObservationsCannotClaimIndependentProof() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"), List.of(
                        source("paper-profile", BanEvidence.Status.BANNED),
                        source(" PAPER-PROFILE ", BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"), List.of(
                        source("paper-profile", BanEvidence.Status.CLEAR),
                        source("paper-profile", BanEvidence.Status.CLEAR))));
    }

    @Test void unexpectedAndMalformedObservationsFailClosed() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"),
                List.of(source("external:unknown", BanEvidence.Status.BANNED))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"),
                Arrays.asList((BanEvidence.Source) null)));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"),
                List.of(source("paper-profile", null))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"),
                List.of(source("  ", BanEvidence.Status.CLEAR))));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper-profile"), (List<BanEvidence.Source>) null));
    }

    @Test void missingRequiredSourceIsUnknownUnlessDefinitivelyBanned() throws Exception {
        Set<String> required = Set.of("paper-profile", "ip-ban", "external:plugin");
        assertEquals(BanEvidence.Status.UNKNOWN, BanEvidence.combine(
                required, List.of(source("paper-profile", BanEvidence.Status.CLEAR))));
        assertEquals(BanEvidence.Status.BANNED, BanEvidence.combine(
                required, List.of(source("paper-profile", BanEvidence.Status.BANNED))));
    }
}
