package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Code-to-spec Candidate BE-LOC-01..04, not a two-file publication guarantee. */
class BedrockPackLocalizationIdentityTest {
    @TempDir Path directory;

    private static EnumMap<CoinDenomination, byte[]> images() {
        var map = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        for (var d : CoinDenomination.values())
            map.put(d, new byte[]{(byte) d.value(), 42, 7});
        return map;
    }

    private static EnumMap<CoinDenomination, String> names() {
        var map = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values())
            map.put(d, d.value() + " Coin");
        return map;
    }

    private static String manifest(Path pack) throws Exception {
        try (var zip = new ZipFile(pack.toFile())) {
            return new String(zip.getInputStream(zip.getEntry("manifest.json"))
                    .readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void inputMapIterationOrderDoesNotChangePackOrMapping() throws Exception {
        Path pack = directory.resolve("ordered.mcpack");
        Path mapping = directory.resolve("ordered.json");
        var imageMap = images();
        var nameMap = names();
        BedrockPack.write(pack, mapping, imageMap, nameMap);
        byte[] originalPack = Files.readAllBytes(pack);
        byte[] originalMapping = Files.readAllBytes(mapping);

        var reversedImages = new HashMap<CoinDenomination, byte[]>();
        var reversedNames = new HashMap<CoinDenomination, String>();
        var denominations = Arrays.asList(CoinDenomination.values());
        for (int i = denominations.size() - 1; i >= 0; i--) {
            var denomination = denominations.get(i);
            reversedImages.put(denomination, imageMap.get(denomination));
            reversedNames.put(denomination, nameMap.get(denomination));
        }
        BedrockPack.write(pack, mapping, reversedImages, reversedNames);
        assertArrayEquals(originalPack, Files.readAllBytes(pack));
        assertArrayEquals(originalMapping, Files.readAllBytes(mapping));
    }

    @Test void changedLocalizedNameInvalidatesPackIdentity() throws Exception {
        Path pack = directory.resolve("localized.mcpack");
        Path mapping = directory.resolve("localized.json");
        var imageMap = images();
        var nameMap = names();
        BedrockPack.write(pack, mapping, imageMap, nameMap);
        String originalManifest = manifest(pack);
        var denomination = CoinDenomination.values()[0];
        byte[] unchangedPng;
        try (var zip = new ZipFile(pack.toFile())) {
            unchangedPng = zip.getInputStream(zip.getEntry(
                    "textures/items/coin_" + denomination.value() + ".png")).readAllBytes();
        }

        nameMap.put(denomination, "Localized " + denomination.value());
        BedrockPack.write(pack, mapping, imageMap, nameMap);
        assertNotEquals(originalManifest, manifest(pack),
                "A changed display name must invalidate the Bedrock pack cache identity");
        assertTrue(Files.readString(mapping).contains("Localized " + denomination.value()));
        try (var zip = new ZipFile(pack.toFile())) {
            assertArrayEquals(unchangedPng, zip.getInputStream(zip.getEntry(
                    "textures/items/coin_" + denomination.value() + ".png")).readAllBytes());
        }
    }

    @Test void missingLocalizedNameCannotReplacePublishedPair() throws Exception {
        Path pack = directory.resolve("existing.mcpack");
        Path mapping = directory.resolve("existing.json");
        var imageMap = images();
        var nameMap = names();
        BedrockPack.write(pack, mapping, imageMap, nameMap);
        byte[] originalPack = Files.readAllBytes(pack);
        byte[] originalMapping = Files.readAllBytes(mapping);

        nameMap.remove(CoinDenomination.values()[0]);
        assertThrows(IllegalArgumentException.class,
                () -> BedrockPack.write(pack, mapping, imageMap, nameMap));
        assertArrayEquals(originalPack, Files.readAllBytes(pack));
        assertArrayEquals(originalMapping, Files.readAllBytes(mapping));
    }

    @Test void localizedDisplayNamesAreJsonEscaped() throws Exception {
        Path pack = directory.resolve("escaped.mcpack");
        Path mapping = directory.resolve("escaped.json");
        var nameMap = names();
        nameMap.put(CoinDenomination.values()[0], "quote\" slash\\ tab\t nul" + (char) 0);
        BedrockPack.write(pack, mapping, images(), nameMap);
        String json = Files.readString(mapping);
        assertFalse(json.contains(String.valueOf((char) 0)),
                "A raw NUL must never appear inside JSON");
        assertTrue(json.contains("\\u0000"), "NUL must use a JSON Unicode escape");
        assertTrue(json.contains("\\t"), "Tab must use a JSON escape");
        assertTrue(json.contains("\\\\"), "Backslash must be escaped");
        assertTrue(json.contains("\\\""), "Quote must be escaped");
    }
}
