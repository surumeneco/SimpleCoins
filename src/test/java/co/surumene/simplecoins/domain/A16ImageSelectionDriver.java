package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.ZipFile;

/** Fixture adapter; classifies actual generated JE/BE bytes, never predicts the CAT outcome. */
public final class A16ImageSelectionDriver {
    private Path root;
    private byte[] standard;
    private byte[] custom;
    private EnumMap<CoinDenomination, String> labels;

    public void prepare(Map<String,Object> state, Map<String,Object> input) throws IOException {
        if (!state.isEmpty()) throw new IllegalArgumentException("Unexpected state");
        root = Files.createTempDirectory("simplecoins-cat-a16-");
        labels = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination d : CoinDenomination.values()) labels.put(d, d.value() + " Coin");
        JavaCurrencyPack.build(root.resolve("baseline"), labels);
        standard = Files.readAllBytes(root.resolve("baseline/images/1 Coin.png"));
        custom = Files.readAllBytes(root.resolve("baseline/images/5 Coin.png"));
        if (Arrays.equals(standard, custom)) throw new IllegalStateException("Fixture images are indistinguishable");
        labels.put(CoinDenomination.values()[0], "First-Custom");
        Path images = root.resolve("candidate/images");
        Files.createDirectories(images);
        if (Boolean.TRUE.equals(input.get("入力.候補重複"))) {
            Files.write(images.resolve("First-Custom.png"),custom);
            Files.write(images.resolve("First_Custom.png"),custom);
        } else if (Boolean.TRUE.equals(input.get("入力.一致画像あり"))) {
            byte[] image = Boolean.TRUE.equals(input.get("入力.一致画像有効"))
                    ? custom : new byte[]{1, 2, 3, 4};
            Files.write(images.resolve("First-Custom.png"),image);
        }
    }

    public Map<String,Object> observeState() {
        return Map.of();
    }

    public Map<String,Object> trigger(String operation, Map<String,Object> input) throws IOException {
        if (!"画像選択".equals(operation)) throw new IllegalArgumentException("Unexpected trigger: " + operation);
        try {
            JavaCurrencyPack.Artifacts artifacts = JavaCurrencyPack.build(root.resolve("candidate"),labels);
            byte[] je, be;
            try (ZipFile javaArchive = new ZipFile(artifacts.javaArchive().toFile());
                 ZipFile bedrockArchive = new ZipFile(artifacts.bedrockArchive().toFile())) {
                je = javaArchive.getInputStream(javaArchive.getEntry(
                        "assets/simplecoins/textures/item/coin_1.png")).readAllBytes();
                be = bedrockArchive.getInputStream(bedrockArchive.getEntry(
                        "textures/items/coin_1.png")).readAllBytes();
            }
            if (!Arrays.equals(je,be)) return Map.of("出力.選択", "mismatched-je-be");
            if (Arrays.equals(je,standard)) return Map.of("出力.選択", "standard");
            if (Arrays.equals(je,custom)) return Map.of("出力.選択", "custom");
            return Map.of("出力.選択", "unexpected-image");
        } catch (IOException expectedRejection) {
            return Map.of("出力.選択", "reject");
        }
    }
}
