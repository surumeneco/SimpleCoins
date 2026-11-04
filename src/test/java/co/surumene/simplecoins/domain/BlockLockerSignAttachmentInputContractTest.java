package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Draft characterization of fail-closed invalid inputs (BL-DOUBLE-07) and
 * translation invariance of physical support geometry. This does not assert
 * BlockLocker ownership or double-chest policy.
 */
class BlockLockerSignAttachmentInputContractTest {
    @Test
    void missingVerifiedSignMountOrContainerFailsClosed() {
        var sign = new BlockLockerSignAttachment.Position(12, 65, -8);
        var container = new BlockLockerSignAttachment.Position(12, 65, -7);
        assertAll(
                () -> assertThrows(IOException.class, () ->
                        BlockLockerSignAttachment.attaches(null,
                                BlockLockerSignAttachment.Mount.WALL_NORTH, container)),
                () -> assertThrows(IOException.class, () ->
                        BlockLockerSignAttachment.attaches(sign, null, container)),
                () -> assertThrows(IOException.class, () ->
                        BlockLockerSignAttachment.attaches(sign,
                                BlockLockerSignAttachment.Mount.WALL_NORTH, null)));
    }

    @Test
    void attachmentIsInvariantUnderSafeCoordinateTranslation() throws IOException {
        var sign = new BlockLockerSignAttachment.Position(12, 65, -8);
        var support = new BlockLockerSignAttachment.Position(12, 65, -7);
        var unrelated = new BlockLockerSignAttachment.Position(13, 65, -7);
        for (var mount : BlockLockerSignAttachment.Mount.values()) {
            boolean originalSupport = BlockLockerSignAttachment.attaches(sign, mount, support);
            boolean originalUnrelated = BlockLockerSignAttachment.attaches(sign, mount, unrelated);
            for (int delta : new int[] {-1000, -1, 0, 1, 1000}) {
                var translatedSign = translate(sign, delta);
                assertEquals(originalSupport, BlockLockerSignAttachment.attaches(
                        translatedSign, mount, translate(support, delta)));
                assertEquals(originalUnrelated, BlockLockerSignAttachment.attaches(
                        translatedSign, mount, translate(unrelated, delta)));
            }
        }
    }

    private static BlockLockerSignAttachment.Position translate(
            BlockLockerSignAttachment.Position position, int delta) {
        return new BlockLockerSignAttachment.Position(
                Math.addExact(position.x(), delta),
                Math.addExact(position.y(), delta),
                Math.addExact(position.z(), delta));
    }
}
