package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Owns only two source files for Geyser Custom Items v2. Geyser must RESTART
 * after a changed staging result; this does not reload its registry or prove
 * Bedrock clients received the pack.
 */
public final class GeyserResourceInstaller {
    private static final String PACK = "packs/simplecoins-generated.mcpack";
    private static final String MAP = "custom_mappings/simplecoins-generated.json";
    private static final int MAX_PACK_BYTES = 32 * 1024 * 1024;
    private static final int MAX_MAPPING_BYTES = 1024 * 1024;

    public record Stage(boolean changed) {}
    private GeyserResourceInstaller() {}

    public static Stage stage(Path geyserData, Path bedrockPack, Path mapping,
                              boolean customContentEnabled) throws IOException {
        if (!customContentEnabled)
            throw new IOException("Geyser custom item content is not enabled");
        Path root = requireRealPath(geyserData);
        byte[] pack = readBounded(bedrockPack, MAX_PACK_BYTES);
        byte[] definitions = readBounded(mapping, MAX_MAPPING_BYTES);
        validatePack(pack);
        validateMapping(definitions);
        Map<String, byte[]> desired = Map.of(PACK, pack, MAP, definitions);
        Path packDestination = root.resolve(PACK);
        Path mappingDestination = root.resolve(MAP);
        boolean changed = !matchesExisting(packDestination, pack)
                || !matchesExisting(mappingDestination, definitions);
        // This authoritative hash-manifest check must run EVEN if the target
        // bytes already match: matching content does not establish ownership.
        new ManagedResources(root).replace(desired);
        return new Stage(changed);
    }

    /** Nexo owns presentation: delete only the two previously owned files. */
    public static boolean removeOwned(Path geyserData) throws IOException {
        Path root = requireRealPath(geyserData);
        Path manifest = root.resolve("simplecoins-managed-resources");
        if (!Files.exists(manifest, LinkOption.NOFOLLOW_LINKS)) return false;
        boolean existed = Files.exists(root.resolve(PACK), LinkOption.NOFOLLOW_LINKS)
                || Files.exists(root.resolve(MAP), LinkOption.NOFOLLOW_LINKS);
        new ManagedResources(root).replace(Map.of());
        return existed;
    }

    private static byte[] readBounded(Path source, int maxBytes) throws IOException {
        Path path = requireRealPath(source);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.size(path) < 1 || Files.size(path) > maxBytes)
            throw new IOException("Missing or excessive Geyser source artifact: " + path);
        try (InputStream stream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] contents = stream.readNBytes(maxBytes + 1);
            if (contents.length < 1 || contents.length > maxBytes)
                throw new IOException("Over-budget Geyser content: " + path);
            return contents;
        }
    }

    private static Path requireRealPath(Path path) throws IOException {
        if (path == null) throw new IOException("Missing Geyser data/artifact path");
        Path absolute = path.toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent())
            if (Files.isSymbolicLink(part))
                throw new IOException("Geyser path contains symbolic link: " + part);
        return absolute;
    }

    private static boolean matchesExisting(Path file, byte[] desired) throws IOException {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return false;
        requireRealPath(file);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.size(file) != desired.length) return false;
        return Arrays.equals(Files.readAllBytes(file), desired);
    }

    private static void validateMapping(byte[] raw) throws IOException {
        String text = new String(raw, StandardCharsets.UTF_8);
        if (!text.contains("\"format_version\":2") || !text.contains("\"minecraft:paper\""))
            throw new IOException("Not a Geyser v2 SimpleCoins PAPER mapping");
        for (CoinDenomination value : CoinDenomination.values()) {
            if (!text.contains("\"bedrock_identifier\":\"simplecoins:coin_"
                    + value.value() + "\""))
                throw new IOException("Missing Geyser denomination: " + value.value());
        }
    }

    private static void validatePack(byte[] source) throws IOException {
        Set<String> members = new HashSet<>();
        int inspectedBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(source))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
                String path = entry.getName();
                if (path.startsWith("/") || path.contains("..") || path.contains("\\")
                        || !members.add(path))
                    throw new IOException("Unsafe or duplicate Bedrock pack member: " + path);
                for (int read; (read = zip.readNBytes(4096).length) > 0;) {
                    inspectedBytes = Math.addExact(inspectedBytes, read);
                    if (inspectedBytes > MAX_PACK_BYTES)
                        throw new IOException("Expanded Bedrock pack exceeds bound");
                }
                zip.closeEntry();
            }
        } catch (ArithmeticException excessive) {
            throw new IOException("Expanded Bedrock pack size overflow", excessive);
        }
        if (!members.contains("manifest.json") || !members.contains("textures/item_texture.json"))
            throw new IOException("Incomplete Bedrock resource pack");
        for (CoinDenomination d : CoinDenomination.values())
            if (!members.contains("textures/items/coin_" + d.value() + ".png"))
                throw new IOException("Missing Bedrock coin texture: " + d.value());
    }
}
