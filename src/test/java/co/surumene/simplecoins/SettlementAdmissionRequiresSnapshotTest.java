package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase6 SNAP-04: the barrier's READY flag is not evidence that an
 * authoritative complete settlement result has been published.
 */
class SettlementAdmissionRequiresSnapshotTest {
    @Test void readyFlagWithoutPublishedSnapshotMustStillDenyLogin() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        var listener = new SettlementAdmissionListener(barrier);
        long generation = barrier.begin();
        barrier.complete(generation, true, true);

        var connection = new PlayerConnectionValidateLoginEvent(null, null);
        assertTrue(connection.isAllowed());
        listener.validate(connection);
        assertFalse(connection.isAllowed(),
                "A READY boolean alone must not admit users without a published settlement snapshot");
    }
}
