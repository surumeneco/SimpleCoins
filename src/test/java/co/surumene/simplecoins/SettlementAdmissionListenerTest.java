package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

/** Real Paper 26.2 API event contract, without a running server or plugin registration. */
class SettlementAdmissionListenerTest {
    private static PlayerConnectionValidateLoginEvent connection(Component preexistingKick) {
        return new PlayerConnectionValidateLoginEvent(null, preexistingKick);
    }

    @Test void admissionBlockedBeforeAndDuringSettlement() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementAdmissionListener listener = new SettlementAdmissionListener(barrier);
        var before = connection(null);
        assertTrue(before.isAllowed());
        listener.validate(before);
        assertFalse(before.isAllowed());
        assertNotNull(before.getKickMessage());

        barrier.begin();
        var during = connection(null);
        listener.validate(during);
        assertFalse(during.isAllowed());
    }

    @Test void verifiedPublicationAllowsConnectionWithoutOverridingPriorBan() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementAdmissionListener listener = new SettlementAdmissionListener(barrier);
        barrier.complete(barrier.begin(), true, true);

        var permitted = connection(null);
        listener.validate(permitted);
        assertTrue(permitted.isAllowed());

        Component earlierBan = Component.text("Other plugin denied login");
        var rejected = connection(earlierBan);
        listener.validate(rejected);
        assertFalse(rejected.isAllowed());
        assertEquals(earlierBan, rejected.getKickMessage());
    }

    @Test void fatalFailureRevokesPublicationAndDeniesFurtherConnections() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        SettlementAdmissionListener listener = new SettlementAdmissionListener(barrier);
        barrier.complete(barrier.begin(), true, true);
        barrier.fatalFailure();
        var denied = connection(null);
        listener.validate(denied);
        assertFalse(denied.isAllowed());
        barrier.stop();
        var stopped = connection(null);
        listener.validate(stopped);
        assertFalse(stopped.isAllowed());
    }

    @Test void eventHandlerUsesHighPriorityAndDoesNotSkipExistingDenial() throws Exception {
        var handler = SettlementAdmissionListener.class.getMethod(
                "validate", PlayerConnectionValidateLoginEvent.class).getAnnotation(EventHandler.class);
        assertNotNull(handler);
        assertEquals(EventPriority.HIGHEST, handler.priority());
        assertThrows(NullPointerException.class, () -> new SettlementAdmissionListener(null));
    }
}
