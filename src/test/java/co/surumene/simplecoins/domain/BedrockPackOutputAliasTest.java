package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Draft BE-ALIAS vectors: distinct Bedrock output identities before publication. */
class BedrockPackOutputAliasTest {
    @TempDir Path directory;

    private static Map<CoinDenomination, byte[]> images() {
        var result = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values())
            result.put(denomination, new byte[] {1, 2, 3});
        return result;
    }

    private static Map<CoinDenomination, String> names() {
        var result = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values())
            result.put(denomination, denomination.value() + " Coin");
        return result;
    }

    @Test void identicalDestinationsMustNotPublishJsonOverPack() {
        Path output = directory.resolve("coins.mcpack");
        assertThrows(IOException.class, () -> BedrockPack.write(
                output, output, images(), names()));
        assertFalse(Files.exists(output), "Reject before either output is published");
    }

    @Test void dotSegmentAliasesMustNotPublishJsonOverPack() {
        Path output = directory.resolve("coins.mcpack");
        Path alias = directory.resolve("./coins.mcpack");
        assertThrows(IOException.class, () -> BedrockPack.write(
                output, alias, images(), names()));
        assertFalse(Files.exists(output), "Reject normalized aliases before publication");
    }


    @Test void existingOutputMustSurviveRejectedAlias() throws Exception {
        Path output = directory.resolve("existing.mcpack");
        byte[] original = new byte[] {0x50, 0x4b, 0x03, 0x04, 0x11};
        Files.write(output, original);
        assertThrows(IOException.class, () -> BedrockPack.write(
                output, output, images(), names()));
        assertArrayEquals(original, Files.readAllBytes(output),
                "Alias rejection must not destroy a previously published artifact");
    }

    @Test void distinctDestinationsStillPublishValidSeparateArtifacts() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(pack, mapping, images(), names());
        try (var zip = new ZipFile(pack.toFile())) {
            assertNotNull(zip.getEntry("manifest.json"));
        }
        assertTrue(Files.readString(mapping).contains("\"format_version\":2"));
    }
}
