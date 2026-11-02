package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompanionOwnerResolutionTest {
    private static final UUID COMMANDER =
            UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000022");

    @Test void verifiedCommanderTakesPrecedenceOverDifferentTamedOwner() throws Exception {
        var result = CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.known(COMMANDER),
                CompanionOwnerResolution.known(OWNER));
        assertEquals(ExternalOwnerDecision.Kind.PERSONAL, result.kind());
        assertEquals(COMMANDER, result.owner());
    }

    @Test void verifiedAbsenceAllowsOwnerThenFreeFallback() throws Exception {
        var personal = CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.absent(), CompanionOwnerResolution.known(OWNER));
        assertEquals(OWNER, personal.owner());
        var free = CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.absent(), CompanionOwnerResolution.absent());
        assertEquals(ExternalOwnerDecision.Kind.FREE, free.kind());
        assertNull(free.owner());
    }

    @Test void unknownSourceCannotBeMistakenForVerifiedAbsence() throws Exception {
        var unknownCommander = CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.unknown(), CompanionOwnerResolution.known(OWNER));
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN, unknownCommander.kind());
        var unknownOwner = CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.absent(), CompanionOwnerResolution.unknown());
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN, unknownOwner.kind());
        var clear = BanEvidence.evaluate(Set.of("paper"),
                List.of(new BanEvidence.Source("paper", BanEvidence.Status.CLEAR)));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                unknownCommander, clear, true));
        assertThrows(IOException.class, () -> ExternalOwnerDecision.toEntity(
                unknownOwner, clear, true));
    }

    @Test void missingOrContradictoryIdentityEvidenceIsInvalid() {
        assertThrows(IOException.class, () -> CompanionOwnerResolution.resolve(
                null, CompanionOwnerResolution.absent()));
        assertThrows(IOException.class, () -> CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.absent(), null));
        assertThrows(IllegalArgumentException.class, () ->
                CompanionOwnerResolution.known(null));
        assertThrows(IllegalArgumentException.class, () ->
                new CompanionOwnerResolution.Identity(
                        CompanionOwnerResolution.State.ABSENT, OWNER));
        assertThrows(IllegalArgumentException.class, () ->
                new CompanionOwnerResolution.Identity(null, null));
    }
}
