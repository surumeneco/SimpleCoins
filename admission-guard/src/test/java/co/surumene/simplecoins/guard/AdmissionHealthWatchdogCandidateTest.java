package co.surumene.simplecoins.guard;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Source-derived fail-closed regression candidate; not a CAT-generated test. */
class AdmissionHealthWatchdogCandidateTest {
    @Test void unverifiedBootstrapMustNeverSelfShutdownBeforeAnyAdmission() {
        var watchdog = new AdmissionHealthWatchdog();
        for (int i = 0; i < 100; i++) assertFalse(watchdog.mustStop(false));
        assertFalse(watchdog.hasObservedHealthy());
    }

    @Test void lossAfterOneVerifiedAdmittableStateBecomesTerminal() {
        var watchdog = new AdmissionHealthWatchdog();
        assertFalse(watchdog.mustStop(true));
        assertTrue(watchdog.hasObservedHealthy());
        assertTrue(watchdog.mustStop(false), "Journal or plugin health loss after readiness must trigger shutdown");
        assertTrue(watchdog.mustStop(true), "Later apparent recovery must not reopen old accounting generation");
        assertTrue(watchdog.mustStop(false));
    }

    @Test void fullyHealthyHeartbeatNeverTrips() {
        var watchdog = new AdmissionHealthWatchdog();
        for (int i = 0; i < 100; i++) assertFalse(watchdog.mustStop(true));
        assertTrue(watchdog.hasObservedHealthy());
    }

    @Test void successfulLoginFollowedByFailureMustTripWithoutAnotherLogin() {
        var watchdog = new AdmissionHealthWatchdog();
        // The connection listener records one real positive admission check.
        assertFalse(watchdog.mustStop(true));
        // A later main-thread heartbeat sees service failure, without a new login event.
        assertTrue(watchdog.mustStop(false));
    }
}
