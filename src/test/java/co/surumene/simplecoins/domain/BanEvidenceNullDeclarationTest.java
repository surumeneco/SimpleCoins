package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** CAT code-to-spec Draft BAN-NULL-01..06: existing fail-closed behavior. */
class BanEvidenceNullDeclarationTest {
    private static final BanEvidence.Source BANNED =
            new BanEvidence.Source("paper", BanEvidence.Status.BANNED);

    @Test
    void nullSourceDeclarationOrObservationContainerIsRejected() {
        assertThrows(IOException.class, () -> BanEvidence.combine(null, List.of(BANNED)));
        assertThrows(IOException.class, () -> BanEvidence.combine(Set.of("paper"), null));
    }

    @Test
    void nullOrBlankMemberOfRequiredSourceDeclarationIsRejected() {
        Set<String> withNull = new HashSet<>();
        withNull.add("paper");
        withNull.add(null);
        assertThrows(IOException.class, () -> BanEvidence.combine(withNull, List.of(BANNED)));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper", " \t "), List.of(BANNED)));
    }

    @Test
    void malformedObservationAfterPositiveBanMustNotBeIgnored() throws Exception {
        assertEquals(BanEvidence.Status.BANNED,
                BanEvidence.combine(Set.of("paper"), List.of(BANNED)));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), Arrays.asList(BANNED, null)));
        assertThrows(IOException.class, () -> BanEvidence.combine(
                Set.of("paper"), List.of(BANNED,
                        new BanEvidence.Source(null, BanEvidence.Status.CLEAR))));
    }
}
