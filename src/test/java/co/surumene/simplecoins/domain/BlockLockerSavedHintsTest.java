package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerSavedHintsTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0001-0000-000000000002");

    private static Map<String,Object> ownerProfile() {
        return Map.of("blocklocker:n", "Alice",
                "blocklocker:u", new long[]{OWNER.getMostSignificantBits(),
                                             OWNER.getLeastSignificantBits()});
    }

    @Test void privateSignExposesOnlyCandidateOwnerAndEveryoneMarker() throws Exception {
        var hints = BlockLockerSavedHints.read(Map.of(
                "blocklocker:header", "PRIVATE",
                "blocklocker:profile_1", ownerProfile(),
                "blocklocker:profile_2", Map.of("blocklocker:e", (byte) 0))).orElseThrow();
        assertEquals("PRIVATE", hints.header());
        assertEquals(OWNER, hints.firstProfileUuid().orElseThrow());
        assertTrue(hints.everyoneMarkerPresent(),
                "BlockLocker treats the presence of its saved boolean key as a marker");
    }

    @Test void moreUsersProfileCannotBeMistakenForMainOwner() throws Exception {
        var hints = BlockLockerSavedHints.read(Map.of(
                "blocklocker:header", "MORE_USERS",
                "blocklocker:profile_1", ownerProfile())).orElseThrow();
        assertTrue(hints.firstProfileUuid().isEmpty());
        assertFalse(hints.everyoneMarkerPresent());
    }

    @Test void absentPdcCannotProveContainerIsUnprotected() throws Exception {
        assertTrue(BlockLockerSavedHints.read(Map.of()).isEmpty());
    }

    @Test void malformedOrOrphanedProfileDataMustFailClosed() {
        assertThrows(IOException.class, () -> BlockLockerSavedHints.read(Map.of(
                "blocklocker:profile_1", ownerProfile())));
        assertThrows(IOException.class, () -> BlockLockerSavedHints.read(Map.of(
                "blocklocker:header", "PRIVATE",
                "blocklocker:profile_1", "not a compound")));
        assertThrows(IOException.class, () -> BlockLockerSavedHints.read(Map.of(
                "blocklocker:header", "PRIVATE",
                "blocklocker:profile_1", Map.of("blocklocker:n", "Alice",
                        "blocklocker:u", new long[]{1L}))));
    }
}
