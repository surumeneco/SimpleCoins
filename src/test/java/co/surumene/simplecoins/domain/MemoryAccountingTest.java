package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;

final class MemoryAccountingTest {
    @Test void confirmedMovementsUpdateOnlyTheAffectedAccounts() {
        MemoryAccountingChecks.main(new String[0]);
    }
}
