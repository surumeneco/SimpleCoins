package co.surumene.simplecoins;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.CoinImageNames;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** Generates a resource-pack artifact. Hosting/JE distribution and Geyser mappings are separate adapters. */
public final class JavaPackBuilder {
    private final SimpleCoinsPlugin plugin;
    private final CoinItems items;

    public JavaPackBuilder(SimpleCoinsPlugin plugin, CoinItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    /** Safe even if the old pack is serving: write a new archive then replace atomically. */
    public Path build() throws IOException {
        Path folder = plugin.getDataFolder().toPath();
        Path imageDir = folder.resolve("images");
        Files.createDirectories(imageDir);
        List<String> names = new ArrayList<>();
        try (var listing = Files.list(imageDir)) {
            listing.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).forEach(names::add);
        }
        for (CoinDenomination d : CoinDenomination.values()) {
            String stem = CoinImageNames.normalizeStem(items.name(d));
            String defaultFile = items.name(d) + ".png";
            if (names.stream().noneMatch(file -> file.endsWith(".png") &&
                    safeNormalize(file.substring(0, file.length()-4)).equals(stem))) {
                if (!items.name(d).equals(d.value() + " Coin")) {
                    throw new IOException("Missing image for renamed denomination: " + items.name(d));
                }
                Path image = imageDir.resolve(defaultFile).normalize();
                if (!image.getParent().equals(imageDir)) throw new IOException("Invalid image path");
                makeDefaultCoin(image, d.value());
                names.add(defaultFile);
            }
        }

        Path archive = folder.resolve("SimpleCoins-java.zip");
        Path temporary = Files.createTempFile(folder, "pack-", ".zip");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                put(zip, "pack.mcmeta",
                    "{\"pack\":{\"description\":\"SimpleCoins 26.2\",\"min_format\":[88,0],\"max_format\":[88,0]}}");
                for (CoinDenomination d : CoinDenomination.values()) {
                    String name;
                    try { name = CoinImageNames.uniquePng(items.name(d), names); }
                    catch (RuntimeException ambiguous) { throw new IOException("Image lookup failed for " + d.value(), ambiguous); }
                    int value = d.value();
                    String id = "coin_" + value;
                    zip.putNextEntry(new ZipEntry("assets/simplecoins/textures/item/" + id + ".png"));
                    Files.copy(imageDir.resolve(name), zip);
                    zip.closeEntry();
                    put(zip, "assets/simplecoins/models/item/" + id + ".json",
                        "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"simplecoins:item/" + id + "\"}}");
                    put(zip, "assets/simplecoins/items/" + id + ".json",
                        "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"simplecoins:item/" + id + "\"}}");
                }
            }
            try {
                Files.move(temporary, archive, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, archive, StandardCopyOption.REPLACE_EXISTING);
            }
            return archive;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String safeNormalize(String stem) {
        try { return CoinImageNames.normalizeStem(stem); }
        catch (IllegalArgumentException ignored) { return ""; }
    }

    private static void put(ZipOutputStream zip, String name, String data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void makeDefaultCoin(Path path, int value) throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean silver = value < 10;
            boolean copper = value < 100;
            Color base = silver ? new Color(181, 194, 211) : copper ? new Color(200, 123, 68) : new Color(219, 181, 74);
            Color light = silver ? new Color(239, 244, 251) : copper ? new Color(246, 181, 122) : new Color(255, 232, 146);
            g.setColor(new Color(0, 0, 0, 60)); g.fillOval(6, 7, 53, 53);
            g.setColor(base); g.fillOval(4, 4, 54, 54);
            g.setStroke(new BasicStroke(3f)); g.setColor(light); g.drawOval(9, 9, 44, 44);
            String label = String.valueOf(value);
            int size = label.length() >= 5 ? 13 : label.length() >= 4 ? 16 : label.length() >= 3 ? 19 : 24;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
            var metrics = g.getFontMetrics();
            g.setColor(new Color(56, 42, 24));
            g.drawString(label, (64 - metrics.stringWidth(label)) / 2, 32 + metrics.getAscent()/3);
        } finally { g.dispose(); }
        if (!ImageIO.write(img, "PNG", path.toFile())) throw new IOException("PNG encoder unavailable");
    }
}
