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

/** SC-BPAIR-01..03: proposed Bedrock pack/mapping pair-publication contract. */
class BedrockPackPairPublicationTest {
    @TempDir Path directory;

    private static EnumMap<CoinDenomination, byte[]> images(byte value) {
        var result = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values())
            result.put(denomination, new byte[]{value});
        return result;
    }

    private static EnumMap<CoinDenomination, String> names() {
        var result = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values())
            result.put(denomination, denomination.value() + " Coin");
        return result;
    }

    @Test void mappingPublicationFailureMustNotReplacePreviousPack() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path initialMapping = directory.resolve("initial.json");
        BedrockPack.write(pack, initialMapping, images((byte) 1), names());
        byte[] previousPack = Files.readAllBytes(pack);

        // Nonempty directory makes the second atomic move fail after the ZIP
        // move. This is a deterministic, nonprivileged filesystem fault.
        Path blockedMapping = directory.resolve("mapping.json");
        Files.createDirectories(blockedMapping);
        Files.writeString(blockedMapping.resolve("sentinel"), "do not remove");

        assertThrows(IOException.class,
                () -> BedrockPack.write(pack, blockedMapping, images((byte) 2), names()));
        assertArrayEquals(previousPack, Files.readAllBytes(pack),
                "A failed pair publication must not expose a new ZIP with old/missing mapping");
        assertTrue(Files.isRegularFile(blockedMapping.resolve("sentinel")));
    }

    @Test void packPublicationFailureMustNotReplacePreviousMapping() throws Exception {
        Path oldPack = directory.resolve("initial.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(oldPack, mapping, images((byte) 1), names());
        byte[] previousMapping = Files.readAllBytes(mapping);

        Path blockedPack = directory.resolve("coins.mcpack");
        Files.createDirectories(blockedPack);
        Files.writeString(blockedPack.resolve("sentinel"), "do not remove");

        assertThrows(IOException.class,
                () -> BedrockPack.write(blockedPack, mapping, images((byte) 2), names()));
        assertArrayEquals(previousMapping, Files.readAllBytes(mapping));
        assertTrue(Files.isRegularFile(blockedPack.resolve("sentinel")));
    }

    @Test void successfulPairPublicationContainsAllDenominations() throws Exception {
        Path pack = directory.resolve("coins.mcpack");
        Path mapping = directory.resolve("coins.json");
        BedrockPack.write(pack, mapping, images((byte) 3), names());

        assertTrue(Files.isRegularFile(pack));
        String definitions = Files.readString(mapping);
        try (var zip = new ZipFile(pack.toFile())) {
            assertNotNull(zip.getEntry("manifest.json"));
            for (CoinDenomination denomination : CoinDenomination.values()) {
                assertNotNull(zip.getEntry("textures/items/coin_" + denomination.value() + ".png"));
                assertTrue(definitions.contains("simplecoins:coin_" + denomination.value()));
            }
        }
    }
}
