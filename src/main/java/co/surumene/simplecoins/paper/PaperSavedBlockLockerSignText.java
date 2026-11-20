package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import java.io.IOException;

/**
 * In Paper 26.2's stopped-world Anvil NBT, front_text/back_text.messages[]
 * already contain the visible sign strings, not JSON component documents.
 * Preserve the exact value: re-parsing a JSON-looking player string could
 * incorrectly turn literal text into a BlockLocker permission.
 * Does not attest source generation, attached geometry or the whole sign set.
 */
public final class PaperSavedBlockLockerSignText {
    private PaperSavedBlockLockerSignText() {}

    public static BlockLockerSavedProfileResolution.DisplayDecoder nativeText() {
        return PaperSavedBlockLockerSignText::read;
    }

    public static String read(String savedLine) throws IOException {
        if (savedLine == null)
            throw new IOException("Saved Paper 26.2 sign display line is unavailable");
        return savedLine;
    }
}
