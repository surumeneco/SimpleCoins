package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Candidate characterization for BL-DOUBLE-03. This verifies only the pure
 * sign-to-support geometry; it does not establish BlockLocker ownership,
 * double-chest membership, loaded chunks or a coherent saved snapshot.
 */
class BlockLockerSignAttachmentNeighborhoodMatrixTest {
    private static final BlockLockerSignAttachment.Mount[] MOUNTS =
            BlockLockerSignAttachment.Mount.values();

    @Test
    void exactlyOneOfTheTwentySevenNeighborCellsSupportsEachMount() throws IOException {
        var sign = new BlockLockerSignAttachment.Position(23, 72, -41);
        for (var mount : MOUNTS) {
            int matches = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        var container = new BlockLockerSignAttachment.Position(
                                sign.x() + dx, sign.y() + dy, sign.z() + dz);
                        boolean expected = switch (mount) {
                            case WALL_NORTH -> dx == 0 && dy == 0 && dz == 1;
                            case WALL_EAST -> dx == -1 && dy == 0 && dz == 0;
                            case WALL_SOUTH -> dx == 0 && dy == 0 && dz == -1;
                            case WALL_WEST -> dx == 1 && dy == 0 && dz == 0;
                            case STANDING -> dx == 0 && dy == -1 && dz == 0;
                        };
                        boolean actual = BlockLockerSignAttachment.attaches(sign, mount, container);
                        assertEquals(expected, actual, () -> "mount=" + mount +
                                ", offset=" + container);
                        if (actual) matches++;
                    }
                }
            }
            assertEquals(1, matches, "Exactly one support cell per mount: " + mount);
        }
    }

    @Test
    void overflowInEachOutwardSupportDirectionFailsClosed() {
        assertAll(
                () -> assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                        new BlockLockerSignAttachment.Position(0, 64, Integer.MAX_VALUE),
                        BlockLockerSignAttachment.Mount.WALL_NORTH,
                        new BlockLockerSignAttachment.Position(0, 64, 0))),
                () -> assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                        new BlockLockerSignAttachment.Position(Integer.MIN_VALUE, 64, 0),
                        BlockLockerSignAttachment.Mount.WALL_EAST,
                        new BlockLockerSignAttachment.Position(0, 64, 0))),
                () -> assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                        new BlockLockerSignAttachment.Position(0, 64, Integer.MIN_VALUE),
                        BlockLockerSignAttachment.Mount.WALL_SOUTH,
                        new BlockLockerSignAttachment.Position(0, 64, 0))),
                () -> assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                        new BlockLockerSignAttachment.Position(Integer.MAX_VALUE, 64, 0),
                        BlockLockerSignAttachment.Mount.WALL_WEST,
                        new BlockLockerSignAttachment.Position(0, 64, 0))),
                () -> assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                        new BlockLockerSignAttachment.Position(0, Integer.MIN_VALUE, 0),
                        BlockLockerSignAttachment.Mount.STANDING,
                        new BlockLockerSignAttachment.Position(0, 0, 0))));
    }
}
