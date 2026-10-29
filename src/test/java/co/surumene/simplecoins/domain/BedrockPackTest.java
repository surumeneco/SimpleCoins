package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class BedrockPackTest {
    @TempDir Path directory;

    @Test void exportsEveryDenominationWithStableModelAndEscapedName() throws Exception {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) {
            images.put(d, new byte[]{1,2,3});
            names.put(d, d.value() + " \"Coin\"\n");
        }
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(pack, mapping, images, names);
        String json = Files.readString(mapping);
        assertTrue(json.contains("\"format_version\":2"));
        assertTrue(json.contains("1 \\\"Coin\\\"\\n"));
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            assertNotNull(zip.getEntry("manifest.json"));
            assertNotNull(zip.getEntry("textures/item_texture.json"));
            for (var d : CoinDenomination.values()) {
                assertNotNull(zip.getEntry("textures/items/coin_" + d.value() + ".png"));
                assertTrue(json.contains("\"model\":\"simplecoins:coin_" + d.value() + "\""));
            }
            assertEquals(11, zip.size());
        }
    }

    @Test void refusesIncompletePackBeforeReplacingPublishedFiles() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        Files.writeString(pack, "previous");
        assertThrows(IllegalArgumentException.class, () -> BedrockPack.write(pack, mapping, Map.of(), Map.of()));
        assertEquals("previous", Files.readString(pack));
    }
}
