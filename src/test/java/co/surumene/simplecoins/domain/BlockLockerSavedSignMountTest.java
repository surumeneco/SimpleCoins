package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockLockerSavedSignMountTest {
    private static Map<String, Object> state(String block, Map<String, String> properties) {
        return Map.of("Name", block, "Properties", properties);
    }

    @Test void fourWallFacingsMapToVerifiedSupportGeometry() throws Exception {
        var chest = new BlockLockerSignAttachment.Position(0, 64, 0);
        String[] facing = {"north", "east", "south", "west"};
        var positions = new BlockLockerSignAttachment.Position[]{
                new BlockLockerSignAttachment.Position(0, 64, -1),
                new BlockLockerSignAttachment.Position(1, 64, 0),
                new BlockLockerSignAttachment.Position(0, 64, 1),
                new BlockLockerSignAttachment.Position(-1, 64, 0)};
        for (int i = 0; i < facing.length; i++) {
            var mount = BlockLockerSavedSignMount.fromPaletteState(state(
                    "minecraft:oak_wall_sign", Map.of("facing", facing[i], "waterlogged", "false")));
            assertTrue(BlockLockerSignAttachment.attaches(positions[i], mount, chest));
        }
    }

    @Test void standingSignRequiresValidRotationAndSupportsBelow() throws Exception {
        for (String rotation : new String[]{"0", "8", "15"}) {
            var mount = BlockLockerSavedSignMount.fromPaletteState(state(
                    "minecraft:cherry_sign", Map.of("rotation", rotation, "waterlogged", "true")));
            assertEquals(BlockLockerSignAttachment.Mount.STANDING, mount);
            assertTrue(BlockLockerSignAttachment.attaches(
                    new BlockLockerSignAttachment.Position(0, 65, 0), mount,
                    new BlockLockerSignAttachment.Position(0, 64, 0)));
        }
    }

    @Test void invalidPaletteStatesFailClosed() {
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(null));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                Map.of("Name", "minecraft:oak_wall_sign")));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_wall_sign", Map.of("facing", "up"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_sign", Map.of("rotation", "16"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_sign", Map.of("rotation", "-1"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_wall_sign", Map.of("facing", "north", "rotation", "0"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_sign", Map.of("rotation", "0", "facing", "north"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:oak_hanging_sign", Map.of("rotation", "0"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:unknown_wall_sign", Map.of("facing", "north"))));
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("minecraft:chest", Map.of("facing", "north"))));
    }
}
