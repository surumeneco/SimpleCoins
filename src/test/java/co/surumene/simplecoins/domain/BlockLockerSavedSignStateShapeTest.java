package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of malformed saved sign state envelopes.
 * Complements the palette-value matrix without changing production behavior.
 */
class BlockLockerSavedSignStateShapeTest {
    private static Map<String, Object> state(Object name, Object properties) {
        Map<String, Object> result = new HashMap<>();
        result.put("Name", name);
        result.put("Properties", properties);
        return result;
    }

    @Test void absentOrInvalidEnvelopeFailsClosed() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(null));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(Map.of()));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(Map.of("Name", "minecraft:oak_sign")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(Map.of("Properties", Map.of())));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(state(null, Map.of())));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(state(123, Map.of())));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(state("minecraft:oak_sign", null)));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(state("minecraft:oak_sign", "rotation=0")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(state("minecraft:oak_sign", Map.of())));
    }

    @Test void unsupportedBlockIdentityCannotAuthenticateAttachment() {
        Map<String, Object> wall = Map.of("facing", "north", "waterlogged", "false");
        Map<String, Object> standing = Map.of("rotation", "0", "waterlogged", "false");
        for (String block : new String[]{
                "", "minecraft:", "minecraft:oak", "minecraft:oak_wall_hanging_sign",
                "minecraft:oak_hanging_sign", "minecraft:stone_wall_sign",
                "minecraft:oak_wall_sign_extra", "other:oak_wall_sign"}) {
            assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                    state(block, wall)), block);
        }
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:stone_sign", standing)));
    }
}
