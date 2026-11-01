package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** TASK-20261101-004: reject partial modern saved sign palette states. */
class BlockLockerSavedSignCompletenessTest {
    private static Map<String, Object> state(String block, Map<String, String> properties) {
        return Map.of("Name", block, "Properties", properties);
    }

    @Test void standingSignWithoutWaterloggedIsNotVerified() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_sign", Map.of("rotation", "8"))));
    }

    @Test void wallSignWithoutWaterloggedIsNotVerified() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_wall_sign", Map.of("facing", "north"))));
    }

    @Test void completeModernPaletteStatesRemainSupported() throws Exception {
        assertEquals(BlockLockerSignAttachment.Mount.STANDING,
                BlockLockerSavedSignMount.fromPaletteState(state("minecraft:oak_sign",
                        Map.of("rotation", "15", "waterlogged", "false"))));
        assertEquals(BlockLockerSignAttachment.Mount.WALL_EAST,
                BlockLockerSavedSignMount.fromPaletteState(state("minecraft:oak_wall_sign",
                        Map.of("facing", "east", "waterlogged", "true"))));
    }

    @Test void invalidWaterloggedAndUnexpectedPropertyStillFail() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_sign",
                        Map.of("rotation", "0", "waterlogged", "unknown"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_wall_sign",
                        Map.of("facing", "north", "waterlogged", "false", "rotation", "0"))));
    }
}
