package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.AdmissionBarrier;
import co.surumene.simplecoins.domain.SettlementSnapshotPublisher;
import co.surumene.simplecoins.domain.SettlementAccumulator;
import co.surumene.simplecoins.domain.WorldScope;
import java.util.Set;
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

    private static SettlementSnapshotPublisher publishEmptyVerifiedSnapshot(AdmissionBarrier barrier) {
        SettlementSnapshotPublisher published = new SettlementSnapshotPublisher(barrier);
        var pending = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("verified-file"));
        pending.completeSource("verified-file");
        published.publish(pending);
        return published;
    }

    @Test void verifiedPublicationAllowsConnectionWithoutOverridingPriorBan() {
        AdmissionBarrier barrier = new AdmissionBarrier();
        long generation = barrier.begin();
        SettlementSnapshotPublisher published = publishEmptyVerifiedSnapshot(barrier);
        SettlementAdmissionListener listener = new SettlementAdmissionListener(barrier, published);
        barrier.complete(generation, true, true);

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
        long generation = barrier.begin();
        SettlementSnapshotPublisher published = publishEmptyVerifiedSnapshot(barrier);
        SettlementAdmissionListener listener = new SettlementAdmissionListener(barrier, published);
        barrier.complete(generation, true, true);
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
        assertThrows(IllegalArgumentException.class, () -> new SettlementAdmissionListener(
                new AdmissionBarrier(), new SettlementSnapshotPublisher(new AdmissionBarrier())));

    }
}
