package co.surumene.simplecoins.domain;
import org.junit.jupiter.api.Test;
final class ProtectedGrantPreflightTest {
    @Test void protectsPreflightThroughSaveAndRejectsStaleLedgerView() throws Exception {
        ProtectedGrantPreflightChecks.main(new String[0]);
    }
}
