package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;

final class GrantLedgerVerifiedPlayerSnapshotTest {
    @Test void coherentReadOnlyPlanningAndDurableIntegrity() throws Exception {
        GrantLedgerVerifiedPlayerSnapshotChecks.main(new String[0]);
    }
}
