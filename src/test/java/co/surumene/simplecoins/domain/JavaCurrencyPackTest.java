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

    @Test void missingRenamedImageIsRejectedWithoutPublishingOrMinting() throws Exception {
        var renamed = new EnumMap<>(names());
        renamed.put(CoinDenomination.values()[0], "First-Custom");
        assertThrows(java.io.IOException.class,
                () -> JavaCurrencyPack.build(temp.resolve("missing-custom"), renamed));
        assertFalse(Files.exists(temp.resolve("missing-custom/SimpleCoins-java.zip")));
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
