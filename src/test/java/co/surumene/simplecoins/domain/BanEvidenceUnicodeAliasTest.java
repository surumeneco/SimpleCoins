package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BanEvidenceUnicodeAliasTest {
    @Test void emSpaceSourceAliasMustNotCountTwice() {
        String alias = "paper" + Character.toString(0x2003);
        assertThrows(IOException.class, () -> BanEvidence.combine(
            Set.of("paper", alias),
            List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                    new BanEvidence.Source(alias, BanEvidence.Status.CLEAR))));
    }
    @Test void noBreakSpaceSourceAliasMustNotCountTwice() {
        String alias = Character.toString(0x00A0) + "paper";
        assertThrows(IOException.class, () -> BanEvidence.combine(
            Set.of("paper", alias),
            List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                    new BanEvidence.Source(alias, BanEvidence.Status.CLEAR))));
    }
    @Test void undeclaredEmSpaceObservationCannotCountAsRequiredEvidence() {
        String alias = "paper" + Character.toString(0x2003);
        assertThrows(IOException.class, () -> BanEvidence.combine(
            Set.of("paper"),
            List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR),
                    new BanEvidence.Source(alias, BanEvidence.Status.CLEAR))));
    }

    @Test void asciiWhitespaceDuplicateRequiredSourceMustBeRejected() {
        assertThrows(IOException.class, () -> BanEvidence.combine(
            Set.of("paper", " paper "),
            List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR))));
    }

    @Test void ordinarySingleSourceClearRemainsValid() throws IOException {
        org.junit.jupiter.api.Assertions.assertEquals(BanEvidence.Status.CLEAR,
            BanEvidence.combine(Set.of("paper"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR))));
    }
}
