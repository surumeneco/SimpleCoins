package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261101-004: exhaustive three-state commander/owner matrix.
 * These inputs are already-verified evidence, not a WWC/WWW/WCL API test.
 * CAT Draft: docs/cat/phase5-companion-identity-matrix.md.
 */
class CompanionOwnerDecisionMatrixTest {
    private static final UUID COMMANDER =
            UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000022");

    private static List<CompanionOwnerResolution.Identity> ownerStates() {
        return List.of(
                CompanionOwnerResolution.known(OWNER),
                CompanionOwnerResolution.absent(),
                CompanionOwnerResolution.unknown());
    }

    @Test void verifiedCommanderOverridesAllThreeOwnerStates() throws Exception {
        for (var owner : ownerStates()) {
            var actual = CompanionOwnerResolution.resolve(
                    CompanionOwnerResolution.known(COMMANDER), owner);
            assertEquals(ExternalOwnerDecision.personal(COMMANDER), actual,
                    "commander KNOWN, owner " + owner.state());
        }
    }

    @Test void verifiedCommanderAbsencePermitsOnlyVerifiedOwnerFallback() throws Exception {
        var expected = List.of(
                ExternalOwnerDecision.personal(OWNER),
                ExternalOwnerDecision.free(),
                ExternalOwnerDecision.unknown());
        var states = ownerStates();
        for (int i = 0; i < states.size(); i++) {
            var actual = CompanionOwnerResolution.resolve(
                    CompanionOwnerResolution.absent(), states.get(i));
            assertEquals(expected.get(i), actual,
                    "commander ABSENT, owner " + states.get(i).state());
        }
    }

    @Test void unknownCommanderNeverFallsBackToAnyOwnerState() throws Exception {
        for (var owner : ownerStates()) {
            var actual = CompanionOwnerResolution.resolve(
                    CompanionOwnerResolution.unknown(), owner);
            assertEquals(ExternalOwnerDecision.unknown(), actual,
                    "commander UNKNOWN, owner " + owner.state());
        }
    }

    @Test void malformedIdentityCannotMasqueradeAsVerifiedAbsence() {
        assertThrows(IllegalArgumentException.class, () ->
                new CompanionOwnerResolution.Identity(
                        CompanionOwnerResolution.State.ABSENT, OWNER));
        assertThrows(IllegalArgumentException.class, () ->
                new CompanionOwnerResolution.Identity(
                        CompanionOwnerResolution.State.UNKNOWN, OWNER));
        assertThrows(IllegalArgumentException.class, () ->
                new CompanionOwnerResolution.Identity(
                        CompanionOwnerResolution.State.KNOWN, null));
    }
}
