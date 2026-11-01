package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockLockerSavedSignCandidatesTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0001-0000-000000000002");

    private static Map<String,Object> privatePdc() {
        return Map.of("blocklocker:header", "PRIVATE",
                "blocklocker:profile_1", Map.of(
                        "blocklocker:n", "Alice",
                        "blocklocker:u", new long[]{OWNER.getMostSignificantBits(),
                                                     OWNER.getLeastSignificantBits()}));
    }

    private static Map<String,Object> sign(String id, int x, Map<String,Object> pdc) {
        return Map.of("id", id, "x", x, "y", 64, "z", 2,
                "PublicBukkitValues", pdc);
    }

    @Test void enumeratesOnlySavedBlockLockerSignHintsWithoutAssigningOwnership() throws Exception {
        Map<String,Object> protectedSign = sign("minecraft:sign", 1, privatePdc());
        Map<String,Object> ordinarySign = Map.of(
                "id", "minecraft:sign", "x", 2, "y", 64, "z", 2);
        Map<String,Object> chest = sign("minecraft:chest", 3, privatePdc());
        var found = BlockLockerSavedSignCandidates.extract(Map.of(
                "block_entities", List.of(protectedSign, ordinarySign, chest)));
        assertEquals(1, found.size());
        assertEquals(OWNER, found.get("1,64,2").firstProfileUuid().orElseThrow());
    }

    @Test void malformedSavedSignAndDuplicateCandidateCoordinatesFailClosed() {
        Map<String,Object> badPdc = sign("minecraft:hanging_sign", 1,
                Map.of("blocklocker:profile_1", Map.of("blocklocker:n", "Alice")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(badPdc))));
        Map<String,Object> duplicate = sign("minecraft:sign", 1, privatePdc());
        assertThrows(IOException.class, () -> BlockLockerSavedSignCandidates.extract(
                Map.of("block_entities", List.of(duplicate, duplicate))));
    }
}
