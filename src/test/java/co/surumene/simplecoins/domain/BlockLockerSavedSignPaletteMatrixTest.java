package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of the saved BlockLocker sign palette boundary.
 * No production behavior or normative CAT status is changed by these tests.
 */
class BlockLockerSavedSignPaletteMatrixTest {
    private static final String[] WOODS = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak"
    };

    private static Map<String, Object> state(String name, Object properties) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Name", name);
        result.put("Properties", properties);
        return result;
    }

    @Test void allRecognizedVanillaWoodsAcceptWallAndStandingForms() throws Exception {
        for (String wood : WOODS) {
            for (String facing : new String[]{"north", "east", "south", "west"}) {
                assertNotNull(BlockLockerSavedSignMount.fromPaletteState(state(
                        "minecraft:" + wood + "_wall_sign",
                        Map.of("facing", facing, "waterlogged", "false"))), wood + "/" + facing);
            }
            for (int rotation = 0; rotation < 16; rotation++) {
                assertEquals(BlockLockerSignAttachment.Mount.STANDING,
                        BlockLockerSavedSignMount.fromPaletteState(state(
                                "minecraft:" + wood + "_sign",
                                Map.of("rotation", Integer.toString(rotation), "waterlogged", "true"))),
                        wood + "/" + rotation);
            }
        }
    }

    @Test void incompleteAndNonStringPropertiesNeverAuthenticateMount() {
        Object[] malformed = {
                Map.of("facing", "north"),
                Map.of("facing", "north", "waterlogged", "yes"),
                Map.of("facing", "north", "waterlogged", 1),
                Map.of("facing", 0, "waterlogged", "false"),
                Map.of("facing", "north", "waterlogged", "false", "extra", "x"),
                Map.of(1, "north", "waterlogged", "false")
        };
        for (Object properties : malformed) {
            assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                    state("minecraft:oak_wall_sign", properties)), properties.toString());
        }
    }

    @Test void noncanonicalRotationsAndNamespacesFailClosed() {
        for (String rotation : new String[]{"00", "01", "+1", " 1", "1 ", "16", "-1", "1.0"}) {
            assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                    state("minecraft:oak_sign",
                            Map.of("rotation", rotation, "waterlogged", "false"))), rotation);
        }
        assertThrows(IOException.class, () -> BlockLockerSavedSignMount.fromPaletteState(
                state("custom:oak_sign", Map.of("rotation", "1", "waterlogged", "false"))));
    }
}
