package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Candidate regression for saved sign chunk-coordinate validation.
 * This is a source-observation test, not a CAT approval or owner decision.
 */
class BlockLockerNegativeChunkBoundaryTest {
    private static Map<String, Object> legacySign(int x, int z) {
        return Map.of(
                "id", "minecraft:sign", "x", x, "y", 64, "z", z,
                "Text1", "[Private]", "Text2", "Alice",
                "Text3", "", "Text4", "");
    }

    @Test void negativeChunkAcceptsMinusOneAndMinusSixteenButRejectsZero() throws Exception {
        var inside = Map.of("xPos", -1, "zPos", -1,
                "block_entities", List.of(legacySign(-1, -16), legacySign(-16, -1)));
        var evidence = BlockLockerSavedSignCandidates.extractTextCandidates(inside);
        assertEquals(2, evidence.size());
        assertTrue(evidence.containsKey("-1,64,-16"));
        assertTrue(evidence.containsKey("-16,64,-1"));

        var outside = Map.of("xPos", -1, "zPos", -1,
                "block_entities", List.of(legacySign(0, -1)));
        assertThrows(IOException.class,
                () -> BlockLockerSavedSignCandidates.extractTextCandidates(outside));
    }

    @Test void negativeChunkRejectsMinusSeventeenAndWrongZChunk() {
        var wrongX = Map.of("xPos", -1, "zPos", -1,
                "block_entities", List.of(legacySign(-17, -1)));
        var wrongZ = Map.of("xPos", -1, "zPos", -1,
                "block_entities", List.of(legacySign(-1, -17)));
        assertAll(
                () -> assertThrows(IOException.class,
                        () -> BlockLockerSavedSignCandidates.extractTextCandidates(wrongX)),
                () -> assertThrows(IOException.class,
                        () -> BlockLockerSavedSignCandidates.extractTextCandidates(wrongZ)));
    }
}
