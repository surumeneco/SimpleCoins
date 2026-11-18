package co.surumene.simplecoins.paper;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PaperWonderfulReaderRegistryTest {
    private static final Map<String, Boolean> BOTH_ENABLED = Map.of(
            "WhatAWonderfulChicken", true, "WhatAWonderfulWolf", true);
    private static final PaperWonderfulReaderRegistry.Entry CHICKEN =
            new PaperWonderfulReaderRegistry.Entry(
                    "wwc", "WhatAWonderfulChicken", true, true, true);
    private static final PaperWonderfulReaderRegistry.Entry WOLF =
            new PaperWonderfulReaderRegistry.Entry(
                    "www", "WhatAWonderfulWolf", true, true, true);

    private static PaperWonderfulReaderRegistry.Assessment evaluate(
            boolean wcl, Map<String, Boolean> enabled,
            PaperWonderfulReaderRegistry.Entry... entries) {
        return PaperWonderfulReaderRegistry.classify(wcl, enabled, List.of(entries));
    }

    @Test void exactTwoSourcesArePresentButNotProvenWorldComplete() {
        var result = evaluate(true, BOTH_ENABLED, CHICKEN, WOLF);
        assertEquals(PaperWonderfulReaderRegistry.Status.PRESENT, result.status());
        assertEquals(Map.of("wwc", "WhatAWonderfulChicken",
                "www", "WhatAWonderfulWolf"), result.owners());
        assertTrue(result.registrationsPresent());
    }

    @Test void orderDoesNotSubstituteSingleServicesManagerWinner() {
        assertEquals(PaperWonderfulReaderRegistry.Status.PRESENT,
                evaluate(true, BOTH_ENABLED, WOLF, CHICKEN).status());
    }

    @Test void missingWclIsNeverEmptyProof() {
        var result = evaluate(false, BOTH_ENABLED, CHICKEN, WOLF);
        assertEquals(PaperWonderfulReaderRegistry.Status.INCOMPLETE, result.status());
        assertFalse(result.registrationsPresent());
        assertTrue(result.owners().isEmpty());
    }

    @Test void oneProviderCannotStandInForTwo() {
        assertEquals(PaperWonderfulReaderRegistry.Status.INCOMPLETE,
                evaluate(true, BOTH_ENABLED, CHICKEN).status());
    }

    @Test void missingOrDisabledExpectedPluginCannotBeAssumedEmpty() {
        assertEquals(PaperWonderfulReaderRegistry.Status.INCOMPLETE,
                evaluate(true, Map.of("WhatAWonderfulChicken", true,
                        "WhatAWonderfulWolf", false), CHICKEN, WOLF).status());
        assertEquals(PaperWonderfulReaderRegistry.Status.INCOMPLETE,
                evaluate(true, Map.of("WhatAWonderfulChicken", true),
                        CHICKEN, WOLF).status());
    }

    @Test void duplicateProviderIdFailsClosed() {
        var result = evaluate(true, BOTH_ENABLED, CHICKEN, CHICKEN, WOLF);
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
        assertTrue(result.owners().isEmpty());
    }

    @Test void unknownProviderFailsClosedEvenWithBothExpected() {
        var result = evaluate(true, BOTH_ENABLED, CHICKEN, WOLF,
                new PaperWonderfulReaderRegistry.Entry(
                        "external", "UnknownPlugin", true, true, true));
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
    }

    @Test void registrationPluginOwnershipMustBeExact() {
        var result = evaluate(true, BOTH_ENABLED, WOLF,
                new PaperWonderfulReaderRegistry.Entry(
                        "wwc", "WhatAWonderfulWolf", true, true, true));
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
    }

    @Test void sameNameButWrongPluginObjectIsRejected() {
        var result = evaluate(true, BOTH_ENABLED, WOLF,
                new PaperWonderfulReaderRegistry.Entry(
                        "wwc", "WhatAWonderfulChicken", true, false, true));
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
    }

    @Test void disabledRegisteredOwnerIsRejected() {
        var result = evaluate(true, BOTH_ENABLED, WOLF,
                new PaperWonderfulReaderRegistry.Entry(
                        "wwc", "WhatAWonderfulChicken", false, true, true));
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
    }

    @Test void foreignClassloaderServiceContractIsRejected() {
        var result = evaluate(true, BOTH_ENABLED, WOLF,
                new PaperWonderfulReaderRegistry.Entry(
                        "wwc", "WhatAWonderfulChicken", true, true, false));
        assertEquals(PaperWonderfulReaderRegistry.Status.CONFLICT, result.status());
    }

    @Test void noRegistrationsNeverMeansZeroStoredCurrency() {
        var result = evaluate(true, BOTH_ENABLED);
        assertEquals(PaperWonderfulReaderRegistry.Status.INCOMPLETE, result.status());
        assertTrue(result.owners().isEmpty());
    }
}
