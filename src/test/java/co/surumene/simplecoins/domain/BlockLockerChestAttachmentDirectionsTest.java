package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Characterization of saved-state double-chest attachment geometry.
 * This is not proof of BlockLocker authority or an economic owner.
 */
class BlockLockerChestAttachmentDirectionsTest {
    private static BlockLockerChestLinkage.Part part(
            int x, int z, BlockLockerChestLinkage.Facing facing,
            BlockLockerChestLinkage.Half half, BlockLockerChestLinkage.Kind kind) {
        return new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(x, 64, z), facing, half, kind);
    }

    @Test void standingSignAboveEitherHalfWorksForAllDirectionsAndChestKinds() throws Exception {
        for (var kind : BlockLockerChestLinkage.Kind.values()) {
            for (var facing : BlockLockerChestLinkage.Facing.values()) {
                int dx = switch (facing) {
                    case NORTH -> 1;
                    case SOUTH -> -1;
                    default -> 0;
                };
                int dz = switch (facing) {
                    case EAST -> 1;
                    case WEST -> -1;
                    default -> 0;
                };
                var left = part(0, 0, facing, BlockLockerChestLinkage.Half.LEFT, kind);
                var right = part(dx, dz, facing, BlockLockerChestLinkage.Half.RIGHT, kind);
                assertTrue(BlockLockerChestLinkage.linked(left, right));
                for (var sign : new BlockLockerSignAttachment.Position[] {
                        new BlockLockerSignAttachment.Position(0, 65, 0),
                        new BlockLockerSignAttachment.Position(dx, 65, dz)
                }) {
                    assertTrue(BlockLockerChestLinkage.signAttachedToPair(
                            sign, BlockLockerSignAttachment.Mount.STANDING, left, right),
                            () -> kind + " " + facing + " " + sign);
                    assertTrue(BlockLockerChestLinkage.signAttachedToPair(
                            sign, BlockLockerSignAttachment.Mount.STANDING, right, left),
                            () -> "reverse order " + kind + " " + facing + " " + sign);
                }
                assertFalse(BlockLockerChestLinkage.signAttachedToPair(
                        new BlockLockerSignAttachment.Position(0, 66, 0),
                        BlockLockerSignAttachment.Mount.STANDING, left, right));
            }
        }
    }

    @Test void missingOrNonreciprocalVerifiedStatesCannotProvideAttachmentEvidence() {
        var left = part(0, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.LEFT, BlockLockerChestLinkage.Kind.CHEST);
        var right = part(1, 0, BlockLockerChestLinkage.Facing.NORTH,
                BlockLockerChestLinkage.Half.RIGHT, BlockLockerChestLinkage.Kind.CHEST);
        var sign = new BlockLockerSignAttachment.Position(0, 65, 0);
        assertThrows(IOException.class, () -> BlockLockerChestLinkage.signAttachedToPair(
                sign, BlockLockerSignAttachment.Mount.STANDING, null, right));
        assertThrows(IOException.class, () -> BlockLockerChestLinkage.signAttachedToPair(
                sign, BlockLockerSignAttachment.Mount.STANDING, left, null));
        var wrongHeight = new BlockLockerChestLinkage.Part(
                new BlockLockerChestLinkage.Position(1, 65, 0),
                right.facing(), right.half(), right.kind());
        assertThrows(IOException.class, () -> BlockLockerChestLinkage.signAttachedToPair(
                sign, BlockLockerSignAttachment.Mount.STANDING, left, wrongHeight));
    }
}
