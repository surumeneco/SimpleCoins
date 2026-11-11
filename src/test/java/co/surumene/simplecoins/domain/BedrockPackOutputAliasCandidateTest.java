package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Candidate regression for output aliasing. It does not assert that the
 * unconfirmed CAT semantics have been approved for production.
 */
class BedrockPackOutputAliasCandidateTest {
    @TempDir Path directory;

    @Test
    void packAndGeyserMappingMustNotShareOnePublishedDestination() throws Exception {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values()) {
            images.put(denomination, new byte[]{1, 2, 3});
            names.put(denomination, denomination.value() + " Coin");
        }

        Path shared = directory.resolve("published.mcpack");
        byte[] published = "existing pack that must be preserved".getBytes(StandardCharsets.UTF_8);
        Files.write(shared, published);

        // A successful call would publish the JSON mapping over the archive.
        // This is a candidate fail-closed oracle pending normative CAT review.
        assertThrows(IOException.class,
                () -> BedrockPack.write(shared, shared, images, names));
        assertArrayEquals(published, Files.readAllBytes(shared),
                "An invalid pair of destinations must preserve published bytes");
    }
    @Test
    void hardLinkedPublishedDestinationsMustNotOverwriteEitherOutput() throws Exception {
        var images = new EnumMap<CoinDenomination, byte[]>(CoinDenomination.class);
        var names = new EnumMap<CoinDenomination, String>(CoinDenomination.class);
        for (var denomination : CoinDenomination.values()) {
            images.put(denomination, new byte[]{1, 2, 3});
            names.put(denomination, denomination.value() + " Coin");
        }
        Path pack = directory.resolve("previous.mcpack");
        Path mapping = directory.resolve("previous.json");
        byte[] published = "unchanged original output".getBytes(StandardCharsets.UTF_8);
        Files.write(pack, published);
        Files.createLink(mapping, pack);

        assertThrows(IOException.class, () -> BedrockPack.write(pack, mapping, images, names));
        assertArrayEquals(published, Files.readAllBytes(pack));
        assertArrayEquals(published, Files.readAllBytes(mapping));
        assertTrue(Files.isSameFile(pack, mapping));
    }
}
