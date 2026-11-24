package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
final class OwnerReconciliationTest {
    @Test void indexedSavedStorageRead() throws Exception { OwnerSavedStorageChecks.main(new String[0]); }
    @Test void observedOwnerReplacement() { OwnerReconciliationChecks.main(new String[0]); }
}
