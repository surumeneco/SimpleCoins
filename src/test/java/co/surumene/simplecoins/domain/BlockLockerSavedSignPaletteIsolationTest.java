package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Each invalid saved palette dimension must fail independently of missing waterlogged. */
class BlockLockerSavedSignPaletteIsolationTest {
    private static Map<String, Object> state(String block, Map<String, ?> properties) {
        return Map.of("Name", block, "Properties", properties);
    }

    private static void failsWith(String expected, Map<String, Object> palette) {
        IOException error = assertThrows(IOException.class,
                () -> BlockLockerSavedSignMount.fromPaletteState(palette));
        assertEquals(expected, error.getMessage());
    }

    @Test void invalidWallFacingIsRejectedWithOtherwiseCompleteState() {
        failsWith("Invalid saved wall sign facing", state("minecraft:oak_wall_sign",
                Map.of("facing", "up", "waterlogged", "false")));
    }

    @Test void invalidStandingRotationsAreRejectedWithWaterloggedPresent() {
        for (String rotation : new String[]{"-1", "16", "01", "north"})
            failsWith("Invalid saved standing sign rotation", state("minecraft:oak_sign",
                    Map.of("rotation", rotation, "waterlogged", "true")));
    }

    @Test void unexpectedOrientationPropertiesFailWithCompleteStates() {
        failsWith("Unexpected saved sign palette property", state("minecraft:oak_wall_sign",
                Map.of("facing", "north", "rotation", "0", "waterlogged", "false")));
        failsWith("Unexpected saved sign palette property", state("minecraft:oak_sign",
                Map.of("rotation", "0", "facing", "north", "waterlogged", "false")));
    }

    @Test void unsupportedBlockTypesAndNamespacesFailIndependently() {
        failsWith("Unknown vanilla saved sign wood type", state("minecraft:oak_hanging_sign",
                Map.of("rotation", "0", "waterlogged", "false")));
        failsWith("Unknown vanilla saved sign wood type", state("minecraft:unknown_wall_sign",
                Map.of("facing", "north", "waterlogged", "false")));
        failsWith("Unsupported saved sign block type", state("minecraft:chest",
                Map.of("facing", "north", "waterlogged", "false")));
        failsWith("Unsupported saved sign block namespace", state("other:oak_wall_sign",
                Map.of("facing", "north", "waterlogged", "false")));
    }

    @Test void missingOrMalformedPropertiesFailAtTheirOwnBoundary() {
        failsWith("Missing saved sign waterlogged state", state("minecraft:oak_sign",
                Map.of("rotation", "0")));
        failsWith("Invalid saved sign waterlogged state", state("minecraft:oak_sign",
                Map.of("rotation", "0", "waterlogged", "unknown")));
        failsWith("Missing saved sign orientation", state("minecraft:oak_sign",
                Map.of("waterlogged", "false")));
        Map<String, Object> malformed = new HashMap<>();
        malformed.put("rotation", 0);
        malformed.put("waterlogged", "false");
        failsWith("Malformed saved sign palette property", state("minecraft:oak_sign", malformed));
    }
}
