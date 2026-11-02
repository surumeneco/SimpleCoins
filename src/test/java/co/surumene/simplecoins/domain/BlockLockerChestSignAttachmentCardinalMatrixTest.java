package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Source-level geometry characterization; does not authenticate protection,
 * loaded chunks, or a coherent Paper snapshot.
 */
class BlockLockerChestSignAttachmentCardinalMatrixTest {
    @Test void eachFacingAndChestKindAttachesWallAndStandingSignsToBothHalves() throws Exception {
        int[][] displacements = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        var facings = BlockLockerChestLinkage.Facing.values();
        for (int f = 0; f < facings.length; f++) {
            for (var kind : BlockLockerChestLinkage.Kind.values()) {
                var left = part(100, 64, 100, facings[f],
                        BlockLockerChestLinkage.Half.LEFT, kind);
                var right = part(100 + displacements[f][0], 64,
                        100 + displacements[f][1], facings[f],
                        BlockLockerChestLinkage.Half.RIGHT, kind);
                assertTrue(BlockLockerChestLinkage.linked(left, right));
                assertTrue(BlockLockerChestLinkage.linked(right, left));
                for (var half : new BlockLockerChestLinkage.Part[] {left, right}) {
                    var position = half.position();
                    var wall = new BlockLockerSignAttachment.Position(
                            position.x(), position.y(), position.z() - 1);
                    var standing = new BlockLockerSignAttachment.Position(
                            position.x(), position.y() + 1, position.z());
                    assertTrue(BlockLockerChestLinkage.signAttachedToPair(wall,
                            BlockLockerSignAttachment.Mount.WALL_NORTH, left, right));
                    assertTrue(BlockLockerChestLinkage.signAttachedToPair(standing,
                            BlockLockerSignAttachment.Mount.STANDING, right, left));
                }
                assertFalse(BlockLockerChestLinkage.signAttachedToPair(
                        new BlockLockerSignAttachment.Position(110, 64, 90),
                        BlockLockerSignAttachment.Mount.WALL_NORTH, left, right));
            }
        }
    }

    @Test void nonreciprocalHalfAndMixedKindCannotAcquireAttachedSignEvidence() throws Exception {
        for (var facing : BlockLockerChestLinkage.Facing.values()) {
            var left = part(0, 64, 0, facing,
                    BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
            var sameHalf = part(1, 64, 0, facing,
                    BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
            var mixedKind = part(1, 64, 0, facing,
                    BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.TRAPPED_CHEST);
            var sign = new BlockLockerSignAttachment.Position(0, 64, -1);
            assertFalse(BlockLockerChestLinkage.linked(left, sameHalf));
            assertFalse(BlockLockerChestLinkage.linked(left, mixedKind));
            assertThrows(java.io.IOException.class, () ->
                    BlockLockerChestLinkage.signAttachedToPair(sign,
                            BlockLockerSignAttachment.Mount.WALL_NORTH, left, sameHalf));
            assertThrows(java.io.IOException.class, () ->
                    BlockLockerChestLinkage.signAttachedToPair(sign,
                            BlockLockerSignAttachment.Mount.WALL_NORTH, left, mixedKind));
        }
    }

    private static BlockLockerChestLinkage.Part part(int x, int y, int z,
            BlockLockerChestLinkage.Facing facing,
            BlockLockerChestLinkage.Half half,
            BlockLockerChestLinkage.Kind kind) {
        return new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(x, y, z), facing, half, kind);
    }
}
