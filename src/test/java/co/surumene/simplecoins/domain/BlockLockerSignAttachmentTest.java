package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class BlockLockerSignAttachmentTest {
    private static BlockLockerSignAttachment.Position pos(int x, int y, int z) {
        return new BlockLockerSignAttachment.Position(x, y, z);
    }

    @Test void northFacingWallSignAttachesBehindNotInFront() throws Exception {
        var sign = pos(10, 64, 10);
        assertTrue(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_NORTH, pos(10, 64, 11)));
        assertFalse(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_NORTH, pos(10, 64, 9)));
    }

    @Test void otherWallDirectionsUseOppositeSupportFace() throws Exception {
        var sign = pos(10, 64, 10);
        assertTrue(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_EAST, pos(9, 64, 10)));
        assertTrue(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_SOUTH, pos(10, 64, 9)));
        assertTrue(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_WEST, pos(11, 64, 10)));
        assertFalse(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.WALL_EAST, pos(11, 64, 10)));
    }

    @Test void standingSignRequiresSupportDirectlyBelow() throws Exception {
        var sign = pos(10, 64, 10);
        assertTrue(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.STANDING, pos(10, 63, 10)));
        assertFalse(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.STANDING, pos(10, 65, 10)));
        assertFalse(BlockLockerSignAttachment.attaches(sign,
                BlockLockerSignAttachment.Mount.STANDING, pos(11, 63, 10)));
    }

    @Test void distantSupportAndCoordinateOverflowFailClosed() throws Exception {
        assertFalse(BlockLockerSignAttachment.attaches(pos(-5, 1, 3),
                BlockLockerSignAttachment.Mount.WALL_SOUTH, pos(-5, 1, 1)));
        assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                pos(Integer.MAX_VALUE, 64, 10),
                BlockLockerSignAttachment.Mount.WALL_WEST, pos(0, 64, 10)));
        assertThrows(IOException.class, () -> BlockLockerSignAttachment.attaches(
                pos(10, Integer.MIN_VALUE, 10),
                BlockLockerSignAttachment.Mount.STANDING, pos(10, 0, 10)));
    }
}
