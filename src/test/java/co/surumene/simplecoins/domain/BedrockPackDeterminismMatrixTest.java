package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Characterizes existing byte-stable JE/BE asset output with varied source data. */
class BedrockPackDeterminismMatrixTest {
    @TempDir Path directory;

    @Test
    void repeatPublicationKeepsZipAndMappingBytesForSixteenUtf8AndImageVariants() throws Exception {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        var random = new Random(471127);
        byte[] precedingArchive = null;
        byte[] precedingMapping = null;

        for (int variant = 0; variant < 16; variant++) {
            for (var denomination : CoinDenomination.values()) {
                byte[] png = new byte[1 + variant * 3 + denomination.value() % 7];
                random.nextBytes(png);
                images.put(denomination, png);
                names.put(denomination, denomination.value()
                        + (variant % 2 == 0 ? " \"Coin\"\\n" : " 貨幣-🎲-") + variant);
            }

            Path archive = directory.resolve("coins.mcpack");
            Path mapping = directory.resolve("coins.json");
            BedrockPack.write(archive, mapping, images, names);
            byte[] originalArchive = Files.readAllBytes(archive);
            byte[] originalMapping = Files.readAllBytes(mapping);
            if (precedingArchive != null) {
                assertFalse(Arrays.equals(precedingArchive, originalArchive),
                        "New names/PNG bytes must invalidate stale Bedrock pack " + variant);
                assertFalse(Arrays.equals(precedingMapping, originalMapping),
                        "New display names must change Geyser mapping " + variant);
            }
            try (var zip = new ZipFile(archive.toFile())) {
                assertNotNull(zip.getEntry("manifest.json"));
                for (var denomination : CoinDenomination.values())
                    assertNotNull(zip.getEntry("textures/items/coin_" + denomination.value() + ".png"));
            }

            BedrockPack.write(archive, mapping, images, names);
            assertArrayEquals(originalArchive, Files.readAllBytes(archive), "pack bytes variant=" + variant);
            assertArrayEquals(originalMapping, Files.readAllBytes(mapping), "mapping bytes variant=" + variant);
            precedingArchive = originalArchive;
            precedingMapping = originalMapping;
        }
    }
}
