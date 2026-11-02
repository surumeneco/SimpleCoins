package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CompanionOwnerResolution;
import co.surumene.simplecoins.domain.ExternalOwnerDecision;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaperCompanionOwnerAdapterTest {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Test void verifiedTameableOwnerAndUntamedAbsenceAreDistinct() {
        var known = PaperCompanionOwnerAdapter.read(true, () -> OWNER);
        assertEquals(CompanionOwnerResolution.State.KNOWN, known.state());
        assertEquals(OWNER, known.uuid());
        var absent = PaperCompanionOwnerAdapter.read(false,
                () -> { throw new AssertionError("Untamed must not query owner"); });
        assertEquals(CompanionOwnerResolution.State.ABSENT, absent.state());
    }

    @Test void missingOrUnavailablePaperOwnerIsUnknown() {
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperCompanionOwnerAdapter.read(null, () -> OWNER).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperCompanionOwnerAdapter.read(true, null).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperCompanionOwnerAdapter.read(true, () -> null).state());
        assertEquals(CompanionOwnerResolution.State.UNKNOWN,
                PaperCompanionOwnerAdapter.read(true, () -> {
                    throw new IllegalStateException("Entity no longer loaded");
                }).state());
    }

    @Test void knownPaperOwnerDoesNotProveMissingCommanderIsAbsent() throws Exception {
        var paperOwner = PaperCompanionOwnerAdapter.read(true, () -> OWNER);
        assertEquals(ExternalOwnerDecision.Kind.UNKNOWN,
                CompanionOwnerResolution.resolve(
                        CompanionOwnerResolution.unknown(), paperOwner).kind());
        assertEquals(OWNER, CompanionOwnerResolution.resolve(
                CompanionOwnerResolution.absent(), paperOwner).owner());
    }
}
