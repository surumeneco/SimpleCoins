package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Code-to-spec Candidate: reject ZIP/JSON output aliasing before publication. */
class BedrockPackOutputCollisionTest {
    @TempDir Path directory;

    private static EnumMap<CoinDenomination, byte[]> images() {
        var result = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values())
            result.put(denomination, new byte[]{1, 2, 3});
        return result;
    }

    private static EnumMap<CoinDenomination, String> names() {
        var result = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values())
            result.put(denomination, denomination.value() + " Coin");
        return result;
    }

    @Test void identicalPackAndMappingPathMustBeRejected() throws Exception {
        Path output = directory.resolve("coins.mcpack");
        assertThrows(IllegalArgumentException.class,
                () -> BedrockPack.write(output, output, images(), names()));
        assertFalse(Files.exists(output), "Invalid input must not publish a JSON-overwritten ZIP");
    }

    @Test void normalizedAliasOfOutputPathMustBeRejected() throws Exception {
        Files.createDirectories(directory.resolve("nested"));
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("nested/../coins.mcpack");
        assertEquals(pack.normalize(), mapping.normalize());
        assertThrows(IllegalArgumentException.class,
                () -> BedrockPack.write(pack, mapping, images(), names()));
        assertFalse(Files.exists(pack), "Normalized output aliases must fail before publication");
    }

    @Test void distinctOutputPathsProduceZipAndMapping() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(pack, mapping, images(), names());
        try (var zip = new ZipFile(pack.toFile())) {
            assertNotNull(zip.getEntry("manifest.json"));
            assertNotNull(zip.getEntry("textures/item_texture.json"));
        }
        assertTrue(Files.readString(mapping).contains("minecraft:paper"));
    }
}
