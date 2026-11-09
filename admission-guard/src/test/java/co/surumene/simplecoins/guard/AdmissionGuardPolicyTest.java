package co.surumene.simplecoins.guard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** All truth-table rows of the derived four-input guard Draft. */
class AdmissionGuardPolicyTest {
    @Test
    void denyUnlessMainRegisteredPublishedAndNotStopping() {
        for (int bits = 0; bits < 16; bits++) {
            boolean mainEnabled = (bits & 8) != 0;
            boolean providerRegistered = (bits & 4) != 0;
            boolean verifiedAdmission = (bits & 2) != 0;
            boolean shuttingDown = (bits & 1) != 0;
            boolean expected = bits == 14;
            assertEquals(expected,
                    AdmissionGuardPolicy.allowed(mainEnabled, providerRegistered,
                            verifiedAdmission, shuttingDown), "truth table row " + bits);
        }
    }
}
