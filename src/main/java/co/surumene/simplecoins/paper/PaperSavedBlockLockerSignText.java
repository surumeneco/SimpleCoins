package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.BlockLockerSavedProfileResolution;
import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Read-only decoding of 26.2 saved SignText messages[] JSON to the visible
 * legacy text used by BlockLocker v1.15 SignParserImpl.
 * This does not observe attachment, owner, generation or a complete source.
 */
public final class PaperSavedBlockLockerSignText {
    private PaperSavedBlockLockerSignText() {}

    public static BlockLockerSavedProfileResolution.DisplayDecoder jsonComponent() {
        return PaperSavedBlockLockerSignText::read;
    }

    public static String read(String savedJson) throws IOException {
        if (savedJson == null)
            throw new IOException("Missing saved sign component");
        try {
            Component component = GsonComponentSerializer.gson().deserialize(savedJson);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (RuntimeException invalid) {
            throw new IOException("Malformed saved sign JSON component", invalid);
        }
    }
}
