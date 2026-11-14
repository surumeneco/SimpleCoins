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

    @Test void refusesUnloadedDoubleChestPartnerBeforeProviderLookup() throws Exception {
        var left = org.bukkit.block.data.type.Chest.Type.LEFT;
        var right = org.bukkit.block.data.type.Chest.Type.RIGHT;
        var single = org.bukkit.block.data.type.Chest.Type.SINGLE;
        // A north-facing LEFT half at x=15 requires the adjacent x=16
        // chunk, but the RIGHT half's partner lies within x=14.
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.requireLoadedPartner(
                15, 4, org.bukkit.block.BlockFace.NORTH, left,
                (cx, cz) -> cx == 0 && cz == 0));
        assertDoesNotThrow(() -> BlockLockerLiveEvidence.requireLoadedPartner(
                15, 4, org.bukkit.block.BlockFace.NORTH, right,
                (cx, cz) -> cx == 0 && cz == 0));
        // Other orientations and negative chunk coordinates must not be
        // rounded toward zero or have LEFT/RIGHT reversed.
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.requireLoadedPartner(
                0, 4, org.bukkit.block.BlockFace.NORTH, right,
                (cx, cz) -> cx == 0 && cz == 0));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.requireLoadedPartner(
                15, 4, org.bukkit.block.BlockFace.SOUTH, right,
                (cx, cz) -> cx == 0 && cz == 0));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.requireLoadedPartner(
                4, 15, org.bukkit.block.BlockFace.EAST, left,
                (cx, cz) -> cx == 0 && cz == 0));
        assertThrows(IOException.class, () -> BlockLockerLiveEvidence.requireLoadedPartner(
                4, 0, org.bukkit.block.BlockFace.WEST, left,
                (cx, cz) -> cx == 0 && cz == 0));
        // A single chest must never access an arbitrary neighboring chunk.
        assertDoesNotThrow(() -> BlockLockerLiveEvidence.requireLoadedPartner(
                15, 15, org.bukkit.block.BlockFace.NORTH, single,
                (cx, cz) -> false));
        assertDoesNotThrow(() -> BlockLockerLiveEvidence.requireLoadedPartner(
                15, 4, org.bukkit.block.BlockFace.NORTH, left,
                (cx, cz) -> cx == 1 && cz == 0));
    }


}
