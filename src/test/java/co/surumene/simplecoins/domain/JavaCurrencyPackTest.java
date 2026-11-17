package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JavaCurrencyPackTest {
    @TempDir Path temp;

    private static Map<CoinDenomination, String> names() {
        EnumMap<CoinDenomination, String> values = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination d : CoinDenomination.values()) values.put(d, d.value() + " Coin");
        return values;
    }

    @Test void generatesNineStableJeItemDefinitionsAndBedrockMappingWithPngs() throws Exception {
        var files = JavaCurrencyPack.build(temp.resolve("simplecoins"), names());
        assertTrue(Files.isRegularFile(files.javaArchive()));
        assertTrue(Files.isRegularFile(files.bedrockArchive()));
        assertTrue(Files.isRegularFile(files.bedrockMapping()));
        assertEquals(9, Files.list(temp.resolve("simplecoins/images")).filter(p -> p.toString().endsWith(".png")).count());
        try (ZipFile javaZip = new ZipFile(files.javaArchive().toFile());
             ZipFile bedrockZip = new ZipFile(files.bedrockArchive().toFile())) {
            assertEquals(1 + 9 * 3, javaZip.size());
            var meta = new String(javaZip.getInputStream(javaZip.getEntry("pack.mcmeta")).readAllBytes());
            assertTrue(meta.contains("\"min_format\":[88,0]"));
            assertTrue(meta.contains("\"max_format\":[88,0]"));
            for (CoinDenomination d : CoinDenomination.values()) {
                String id = "coin_" + d.value();
                String item = new String(javaZip.getInputStream(
                        javaZip.getEntry("assets/simplecoins/items/" + id + ".json")).readAllBytes());
                assertTrue(item.contains("simplecoins:item/" + id));
                assertNotNull(javaZip.getEntry("assets/simplecoins/models/item/" + id + ".json"));
                var image = javaZip.getInputStream(
                        javaZip.getEntry("assets/simplecoins/textures/item/" + id + ".png")).readAllBytes();
                assertNotNull(ImageIO.read(new ByteArrayInputStream(image)));
                assertNotNull(bedrockZip.getEntry("textures/items/" + id + ".png"));
                assertTrue(Files.size(temp.resolve("simplecoins/images/" + d.value() + " Coin.png")) > 32);
            }
        }
        String mapping = Files.readString(files.bedrockMapping());
        assertEquals(9, mapping.split("\"bedrock_identifier\"").length - 1);
    }

    @Test void missingRenamedImageUsesBuiltInDenominationPngForJeAndBedrock() throws Exception {
        Path baseline = temp.resolve("baseline");
        JavaCurrencyPack.build(baseline, names());
        byte[] standard = Files.readAllBytes(baseline.resolve("images/1 Coin.png"));

        var renamed = new EnumMap<>(names());
        renamed.put(CoinDenomination.values()[0], "First-Custom");
        Path root = temp.resolve("renamed");
        var result = JavaCurrencyPack.build(root, renamed);
        assertFalse(Files.exists(root.resolve("images/First-Custom.png")));
        assertFalse(Files.exists(root.resolve("images/1 Coin.png")));
        try (ZipFile javaZip = new ZipFile(result.javaArchive().toFile());
             ZipFile bedrockZip = new ZipFile(result.bedrockArchive().toFile())) {
            assertArrayEquals(standard, javaZip.getInputStream(javaZip.getEntry(
                    "assets/simplecoins/textures/item/coin_1.png")).readAllBytes());
            assertArrayEquals(standard, bedrockZip.getInputStream(bedrockZip.getEntry(
                    "textures/items/coin_1.png")).readAllBytes());
            assertNotNull(javaZip.getEntry("assets/simplecoins/items/coin_1.json"));
        }
        assertTrue(Files.readString(result.bedrockMapping()).contains("First-Custom"));
    }

    @Test void malformedMatchingRenamedImageDoesNotSilentlyFallBack() throws Exception {
        var renamed = new EnumMap<>(names());
        renamed.put(CoinDenomination.values()[0], "First-Custom");
        Path root = temp.resolve("malformed-custom");
        Files.createDirectories(root.resolve("images"));
        Files.write(root.resolve("images/First-Custom.png"), new byte[]{1, 2, 3, 4});
        assertThrows(java.io.IOException.class, () -> JavaCurrencyPack.build(root, renamed));
        assertFalse(Files.exists(root.resolve("SimpleCoins-java.zip")));
    }

    @Test void validMatchingRenamedImageOverridesBuiltInDefault() throws Exception {
        var renamed = new EnumMap<>(names());
        renamed.put(CoinDenomination.values()[0], "First-Custom");
        Path root = temp.resolve("custom");
        Files.createDirectories(root.resolve("images"));
        // Use a valid but *different denomination* texture to detect precedence.
        JavaCurrencyPack.build(temp.resolve("baseline"), names());
        byte[] custom = Files.readAllBytes(temp.resolve("baseline/images/5 Coin.png"));
        Files.write(root.resolve("images/First-Custom.png"), custom);
        var result = JavaCurrencyPack.build(root, renamed);
        try (ZipFile javaZip = new ZipFile(result.javaArchive().toFile());
             ZipFile bedrockZip = new ZipFile(result.bedrockArchive().toFile())) {
            assertArrayEquals(custom, javaZip.getInputStream(javaZip.getEntry(
                    "assets/simplecoins/textures/item/coin_1.png")).readAllBytes());
            assertArrayEquals(custom, bedrockZip.getInputStream(bedrockZip.getEntry(
                    "textures/items/coin_1.png")).readAllBytes());
        }
    }

    @Test void ambiguousUnderscoreAndHyphenNamesAreRejected() throws Exception {
        var names = names();
        var changed = new EnumMap<>(names);
        CoinDenomination first = CoinDenomination.values()[0];
        changed.put(first, "First Coin");
        Path root = temp.resolve("ambiguous");
        Files.createDirectories(root.resolve("images"));
        byte[] png;
        JavaCurrencyPack.build(temp.resolve("defaults"), names);
        png = Files.readAllBytes(temp.resolve("defaults/images/1 Coin.png"));
        Files.write(root.resolve("images/First-Coin.png"), png);
        Files.write(root.resolve("images/First_Coin.png"), png);
        assertThrows(java.io.IOException.class, () -> JavaCurrencyPack.build(root, changed));
        assertFalse(Files.exists(root.resolve("SimpleCoins-java.zip")));
    }

    @Test void existingImageContentAndStableModelsSurviveRebuild() throws Exception {
        Path root = temp.resolve("rebuilt");
        var names = names();
        var first = JavaCurrencyPack.build(root, names);
        byte[] original = Files.readAllBytes(root.resolve("images/1 Coin.png"));
        names = new EnumMap<>(names);
        names.put(CoinDenomination.values()[0], "1_Coin");
        var second = JavaCurrencyPack.build(root, names);
        assertArrayEquals(original, Files.readAllBytes(root.resolve("images/1 Coin.png")));
        assertEquals(first.javaArchive(), second.javaArchive());
        try (ZipFile zip = new ZipFile(second.javaArchive().toFile())) {
            assertNotNull(zip.getEntry("assets/simplecoins/items/coin_1.json"));
            assertEquals(28, zip.size());
        }
    }
}
