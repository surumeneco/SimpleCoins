package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class BlockLockerChestLinkageTest {
    private static BlockLockerChestLinkage.Part chest(
            int x, int y, int z, BlockLockerChestLinkage.Facing facing,
            BlockLockerChestLinkage.Half half, BlockLockerChestLinkage.Kind kind) {
        return new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(x, y, z), facing, half, kind);
    }

    @Test void northFacingPairLinksAcrossChunkBoundary() throws Exception {
        var left = chest(15, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var right = chest(16, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        assertTrue(BlockLockerChestLinkage.linked(left, right));
        assertTrue(BlockLockerChestLinkage.linked(right, left));
    }

    @Test void allCardinalDirectionsFollowBlockFinderRotation() throws Exception {
        for (var pair : new Object[][] {
                {BlockLockerChestLinkage.Facing.EAST, 0, 1},
                {BlockLockerChestLinkage.Facing.SOUTH, -1, 0},
                {BlockLockerChestLinkage.Facing.WEST, 0, -1}
        }) {
            var facing = (BlockLockerChestLinkage.Facing) pair[0];
            var left = chest(0, 64, 0, facing,
                    BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
            var right = chest((int) pair[1], 64, (int) pair[2], facing,
                    BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
            assertTrue(BlockLockerChestLinkage.linked(left, right));
        }
    }

    @Test void incompatibleChestStatesCannotShareOneInventory() throws Exception {
        var left = chest(0, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        assertFalse(BlockLockerChestLinkage.linked(left, chest(1, 64, 0,
                BlockLockerChestLinkage.Facing.SOUTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST)));
        assertFalse(BlockLockerChestLinkage.linked(left, chest(1, 64, 0,
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST)));
        assertFalse(BlockLockerChestLinkage.linked(left, chest(1, 64, 0,
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.TRAPPED_CHEST)));
        assertFalse(BlockLockerChestLinkage.linked(left, chest(2, 64, 0,
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST)));
        assertFalse(BlockLockerChestLinkage.linked(chest(0, 64, 0,
                BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.SINGLE, BlockLockerChestLinkage.Kind.CHEST),
                chest(1, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST)));
    }

    @Test void chestNeighborCoordinateOverflowFailsClosed() {
        var left = chest(Integer.MAX_VALUE, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var right = chest(Integer.MIN_VALUE, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        assertThrows(IOException.class, () -> BlockLockerChestLinkage.linked(left, right));
    }
    @Test void wallAndStandingSignsCanAttachToEitherVerifiedChestHalf() throws Exception {
        var left = chest(15, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var right = chest(16, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        assertTrue(BlockLockerChestLinkage.signAttachedToPair(
                new BlockLockerSignAttachment.Position(15, 64, -1),
                BlockLockerSignAttachment.Mount.WALL_NORTH, left, right));
        assertTrue(BlockLockerChestLinkage.signAttachedToPair(
                new BlockLockerSignAttachment.Position(16, 64, -1),
                BlockLockerSignAttachment.Mount.WALL_NORTH, left, right));
        assertTrue(BlockLockerChestLinkage.signAttachedToPair(
                new BlockLockerSignAttachment.Position(16, 65, 0),
                BlockLockerSignAttachment.Mount.STANDING, left, right));
        assertFalse(BlockLockerChestLinkage.signAttachedToPair(
                new BlockLockerSignAttachment.Position(17, 64, -1),
                BlockLockerSignAttachment.Mount.WALL_NORTH, left, right));
    }

    @Test void invalidChestPairCannotBeUsedAsSignOwnershipEvidence() {
        var left = chest(15, 64, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var invalid = chest(16, 64, 0, BlockLockerChestLinkage.Facing.SOUTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        assertThrows(IOException.class, () -> BlockLockerChestLinkage.signAttachedToPair(
                new BlockLockerSignAttachment.Position(16, 64, -1),
                BlockLockerSignAttachment.Mount.WALL_NORTH, left, invalid));
    }

}
