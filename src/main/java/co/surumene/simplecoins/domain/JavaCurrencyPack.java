package co.surumene.simplecoins.domain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Creates private JE/BE currency pack candidates. Neither generated files
 * nor this class deliver packs to clients or authorize monetary settlement.
 */
public final class JavaCurrencyPack {
    private static final int MAX_PNG_BYTES = 1024 * 1024;

    public record Artifacts(Path javaArchive, Path bedrockArchive, Path bedrockMapping) {}
    private JavaCurrencyPack() {}

    public static Artifacts build(Path dataFolder, Map<CoinDenomination, String> displayNames)
            throws IOException {
        if (dataFolder == null || displayNames == null
                || displayNames.size() != CoinDenomination.values().length)
            throw new IOException("Missing complete denomination display names");
        Path root = dataFolder.toAbsolutePath().normalize();
        Path imagesFolder = PackImageSource.ensureDirectory(root.resolve("images"));
        List<String> candidates = new ArrayList<>();
        try (var files = Files.list(imagesFolder)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".png")) candidates.add(name);
            }
        }

        EnumMap<CoinDenomination, String> selected = new EnumMap<>(CoinDenomination.class);
        Set<String> stems = new HashSet<>();
        List<CoinDenomination> defaultsToCreate = new ArrayList<>();
        // No image writes until all configured denominations and collisions
        // have been checked. Existing images are strictly read-only.
        try {
            for (CoinDenomination coin : CoinDenomination.values()) {
                String label = displayNames.get(coin);
                String stem = CoinImageNames.normalizeStem(label);
                if (!stems.add(stem))
                    throw new IOException("Two denominations require one ambiguous texture: " + stem);
                long matches = 0;
                String resolved = null;
                for (String candidate : candidates) {
                    String candidateStem = CoinImageNames.normalizeStem(
                            candidate.substring(0, candidate.length() - ".png".length()));
                    if (stem.equals(candidateStem)) {
                        matches++;
                        resolved = candidate;
                    }
                }
                if (matches > 1) throw new IOException("Ambiguous PNG for " + label);
                if (matches == 0) {
                    if (!label.equals(coin.value() + " Coin"))
                        throw new IOException("Missing image for custom currency name: " + label);
                    defaultsToCreate.add(coin);
                    resolved = label + ".png";
                } else {
                    readAndValidatePng(PackImageSource.existingPng(imagesFolder, resolved));
                }
                selected.put(coin, resolved);
            }
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid denomination image name", invalid);
        }

        for (CoinDenomination coin : defaultsToCreate) {
            Path fresh = PackImageSource.newDefaultPng(imagesFolder, selected.get(coin));
            writeDefaultPng(fresh, coin.value());
        }

        EnumMap<CoinDenomination, byte[]> imageBytes = new EnumMap<>(CoinDenomination.class);
        EnumMap<CoinDenomination, String> names = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination coin : CoinDenomination.values()) {
            Path input = PackImageSource.existingPng(imagesFolder, selected.get(coin));
            imageBytes.put(coin, readAndValidatePng(input));
            names.put(coin, displayNames.get(coin));
        }

        Path javaArchive = root.resolve("SimpleCoins-java.zip");
        Path bedrockArchive = root.resolve("geyser/SimpleCoins.mcpack");
        Path mapping = root.resolve("geyser/simplecoins.json");
        Path temporary = Files.createTempFile(root, "simplecoins-java-", ".zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                put(zip, "pack.mcmeta", """
                        {"pack":{"description":"SimpleCoins 26.2 currency",
                        "min_format":[88,0],"max_format":[88,0]}}
                        """.getBytes(StandardCharsets.UTF_8));
                for (CoinDenomination coin : CoinDenomination.values()) {
                    String id = "coin_" + coin.value();
                    put(zip, "assets/simplecoins/textures/item/" + id + ".png", imageBytes.get(coin));
                    put(zip, "assets/simplecoins/models/item/" + id + ".json", (
                            "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":"
                            + "\"simplecoins:item/" + id + "\"}}").getBytes(StandardCharsets.UTF_8));
                    put(zip, "assets/simplecoins/items/" + id + ".json", (
                            "{\"model\":{\"type\":\"minecraft:model\",\"model\":"
                            + "\"simplecoins:item/" + id + "\"}}").getBytes(StandardCharsets.UTF_8));
                }
            }
            // Both outputs are staging artifacts under the plugin's own data
            // folder, not the Geyser live configuration or server.properties.
            BedrockPack.write(bedrockArchive, mapping, imageBytes, names);
            PackArchivePublication.publish(temporary, javaArchive);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return new Artifacts(javaArchive, bedrockArchive, mapping);
    }

    private static byte[] readAndValidatePng(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) < 32 || Files.size(path) > MAX_PNG_BYTES)
            throw new IOException("Invalid or oversized currency PNG: " + path);
        byte[] raw;
        try (InputStream input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            raw = input.readNBytes(MAX_PNG_BYTES + 1);
            if (raw.length > MAX_PNG_BYTES)
                throw new IOException("Currency PNG exceeded fixed byte limit: " + path);
        }
        byte[] header = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        for (int i = 0; i < header.length; i++)
            if (raw[i] != header[i])
                throw new IOException("Non-PNG currency texture: " + path);
        // Parse the IHDR dimensions BEFORE allocating an ImageIO pixel buffer.
        int width = java.nio.ByteBuffer.wrap(raw, 16, 4).getInt();
        int height = java.nio.ByteBuffer.wrap(raw, 20, 4).getInt();
        if (width < 1 || height < 1 || width > 512 || height > 512)
            throw new IOException("Unsupported currency texture dimensions: " + path);
        var decoded = ImageIO.read(new ByteArrayInputStream(raw));
        if (decoded == null || decoded.getWidth() != width || decoded.getHeight() != height)
            throw new IOException("Currency texture failed PNG decoding: " + path);
        return raw;
    }

    private static void writeDefaultPng(Path path, int value) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean silver = value < 10;
            boolean copper = value < 100;
            Color base = silver ? new Color(181, 194, 211)
                    : copper ? new Color(200, 123, 68) : new Color(219, 181, 74);
            Color light = silver ? new Color(239, 244, 251)
                    : copper ? new Color(246, 181, 122) : new Color(255, 232, 146);
            g.setColor(new Color(0, 0, 0, 60)); g.fillOval(6, 7, 53, 53);
            g.setColor(base); g.fillOval(4, 4, 54, 54);
            g.setStroke(new BasicStroke(3f)); g.setColor(light); g.drawOval(9, 9, 44, 44);
            String label = String.valueOf(value);
            int fontSize = label.length() >= 5 ? 13 :
                    label.length() >= 4 ? 16 : label.length() >= 3 ? 19 : 24;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            var metrics = g.getFontMetrics();
            g.setColor(new Color(56, 42, 24));
            g.drawString(label, (64 - metrics.stringWidth(label)) / 2,
                    32 + metrics.getAscent() / 3);
        } finally {
            g.dispose();
        }
        try (OutputStream output = Files.newOutputStream(path,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            if (!ImageIO.write(image, "png", output))
                throw new IOException("PNG encoder unavailable");
        }
    }

    private static void put(ZipOutputStream zip, String path, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }
}
