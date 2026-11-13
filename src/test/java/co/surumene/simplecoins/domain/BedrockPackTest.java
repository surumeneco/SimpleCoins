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

    private static Map<CoinDenomination, byte[]> sampleImages() {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) images.put(d, new byte[]{1, 2, 3});
        return images;
    }

    private static Map<CoinDenomination, String> sampleNames() {
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) names.put(d, d.value() + " Coin");
        return names;
    }

    @Test void linkedPackParentCannotRedirectGeneratedArchive() throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path link = Files.createSymbolicLink(directory.resolve("linked"), outside);
        assertThrows(java.io.IOException.class, () -> BedrockPack.write(
                link.resolve("coins.mcpack"), directory.resolve("safe/coins.json"),
                sampleImages(), sampleNames()));
        assertFalse(Files.exists(outside.resolve("coins.mcpack")));
    }

    @Test void linkedMappingParentCannotPublishPartialPack() throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path link = Files.createSymbolicLink(directory.resolve("linked"), outside);
        Path pack = directory.resolve("safe/coins.mcpack");
        assertThrows(java.io.IOException.class, () -> BedrockPack.write(
                pack, link.resolve("coins.json"), sampleImages(), sampleNames()));
        assertFalse(Files.exists(pack));
        assertFalse(Files.exists(outside.resolve("coins.json")));
    }

    @Test void linkedDestinationFileCannotBeReplaced() throws Exception {
        Path outside = Files.writeString(directory.resolve("outside.txt"), "original");
        Path pack = Files.createSymbolicLink(directory.resolve("coins.mcpack"), outside);
        assertThrows(java.io.IOException.class, () -> BedrockPack.write(
                pack, directory.resolve("coins.json"), sampleImages(), sampleNames()));
        assertTrue(Files.isSymbolicLink(pack));
        assertEquals("original", Files.readString(outside));
    }

    @Test void refusesIncompletePackBeforeReplacingPublishedFiles() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        Files.writeString(pack, "previous");
        assertThrows(IllegalArgumentException.class, () -> BedrockPack.write(pack, mapping, Map.of(), Map.of()));
        assertEquals("previous", Files.readString(pack));
    }
    @Test void unchangedContentKeepsIdentityButChangedTextureGetsNewClientCacheKey() throws Exception {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var d : CoinDenomination.values()) { images.put(d, new byte[]{1}); names.put(d, d.value() + " Coin"); }
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(pack, mapping, images, names);
        byte[] original = Files.readAllBytes(pack);
        String manifest;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            manifest = new String(zip.getInputStream(zip.getEntry("manifest.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        BedrockPack.write(pack, mapping, images, names);
        assertArrayEquals(original, Files.readAllBytes(pack));
        images.put(CoinDenomination.values()[0], new byte[]{2});
        BedrockPack.write(pack, mapping, images, names);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            String changed = new String(zip.getInputStream(zip.getEntry("manifest.json")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertNotEquals(manifest, changed);
        }
    }

    @Test void nameOnlyChangeInvalidatesBedrockManifestIdentity() throws Exception {
        var images = sampleImages();
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        names.putAll(sampleNames());
        Path pack = directory.resolve("identity/coins.mcpack");
        Path mapping = directory.resolve("identity/coins.json");
        BedrockPack.write(pack, mapping, images, names);
        String beforeManifest;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            beforeManifest = new String(zip.getInputStream(zip.getEntry("manifest.json"))
                    .readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        String beforeMapping = Files.readString(mapping);
        CoinDenomination changed = CoinDenomination.values()[0];
        names.put(changed, "Renamed display coin");
        BedrockPack.write(pack, mapping, images, names);
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            String afterManifest = new String(zip.getInputStream(zip.getEntry("manifest.json"))
                    .readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertNotEquals(beforeManifest, afterManifest,
                    "Bedrock cache UUID must change when display name changes");
            for (var denomination : CoinDenomination.values()) {
                var entry = zip.getEntry("textures/items/coin_" + denomination.value() + ".png");
                assertNotNull(entry);
                assertArrayEquals(images.get(denomination),
                        zip.getInputStream(entry).readAllBytes());
            }
        }
        String afterMapping = Files.readString(mapping);
        assertNotEquals(beforeMapping, afterMapping);
        assertTrue(afterMapping.contains("Renamed display coin"));
    }
}
