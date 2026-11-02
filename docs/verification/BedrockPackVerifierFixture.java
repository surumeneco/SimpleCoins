import co.surumene.simplecoins.domain.BedrockPack;
import co.surumene.simplecoins.domain.CoinDenomination;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/** Offline verifier fixture using the actual BedrockPack generator, not a mock ZIP. */
public final class BedrockPackVerifierFixture {
    private BedrockPackVerifierFixture() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Output directory required");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        byte[] pixel = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Yw5AHMAAAAASUVORK5CYII=");
        Map<CoinDenomination, byte[]> images = new EnumMap<>(CoinDenomination.class);
        Map<CoinDenomination, String> names = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values()) {
            images.put(denomination, pixel.clone());
            names.put(denomination, "Coin " + denomination.value());
        }
        BedrockPack.write(output.resolve("SimpleCoins.mcpack"),
                output.resolve("simplecoins.json"), images, names);
    }
}
