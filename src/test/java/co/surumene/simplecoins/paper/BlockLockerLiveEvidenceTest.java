package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerLiveEvidenceTest {
    private static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Object EVERYONE = new Object();

    @Test void everyoneOverwritesAnOtherwiseKnownOwnerWithoutGuessingUUID() throws Exception {
        var observation = BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of("Alice", EVERYONE), EVERYONE,
                value -> Optional.of(ALICE));
        assertTrue(observation.everyoneProtected());
        assertNull(observation.owner());
    }

    @Test void nonSharedProtectionRequiresExplicitUUID() throws Exception {
        var observation = BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of("Alice"), EVERYONE,
                value -> Optional.of(ALICE));
        assertFalse(observation.everyoneProtected());
        assertEquals(ALICE, observation.owner());
    }

    @Test void unknownOwnerOrAbsentOwnerIsNeverFreeByAccident() {
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of("Alice"), EVERYONE,
                value -> Optional.empty()));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.empty(), List.of(), EVERYONE,
                value -> Optional.of(ALICE)));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of(), EVERYONE,
                value -> Optional.of(ALICE)));
    }

    @Test void incompleteOrContradictoryProtectionEvidenceIsRejected() {
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of("Bob"), EVERYONE,
                value -> Optional.of(ALICE)));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), null, EVERYONE,
                value -> Optional.of(ALICE)));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.classify(
                Optional.of("Alice"), List.of("Alice"), null,
                value -> Optional.of(ALICE)));
    }
}
