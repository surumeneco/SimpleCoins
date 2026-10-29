package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Deterministic Geyser v2 definition mapping and its matching Bedrock texture pack. */
public final class BedrockPack {
    private BedrockPack() {}

    public static void write(Path pack, Path mapping, Map<CoinDenomination, byte[]> images,
                             Map<CoinDenomination, String> names) throws IOException {
        for (var d : CoinDenomination.values()) {
            if (images.get(d) == null || images.get(d).length == 0 || names.get(d) == null)
                throw new IllegalArgumentException("Missing denomination " + d);
        }
        Files.createDirectories(pack.toAbsolutePath().getParent());
        Files.createDirectories(mapping.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(pack.toAbsolutePath().getParent(), "bedrock-", ".zip");
        Path temporaryMapping = Files.createTempFile(mapping.toAbsolutePath().getParent(), "mapping-", ".json");
        try {
            StringBuilder textures = new StringBuilder("{\"resource_pack_name\":\"SimpleCoins\",\"texture_name\":\"atlas.items\",\"texture_data\":{");
            StringBuilder definitions = new StringBuilder("{\"format_version\":2,\"items\":{\"minecraft:paper\":[");
            try (var zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                put(zip, "manifest.json", """
                    {"format_version":2,"header":{"name":"SimpleCoins","description":"SimpleCoins currency textures",
                    "uuid":"c4207313-06d8-4aa2-8b22-2f8fc83baacd","version":[1,0,0],"min_engine_version":[1,21,0]},
                    "modules":[{"type":"resources","uuid":"b3f7d8bd-34b3-429b-8cc6-a96c1cac0277","version":[1,0,0]}]}
                    """.getBytes(StandardCharsets.UTF_8));
                boolean first = true;
                for (var d : CoinDenomination.values()) {
                    if (!first) { textures.append(','); definitions.append(','); }
                    first = false;
                    String id = "coin_" + d.value();
                    String qualified = "simplecoins:" + id;
                    textures.append(quote(qualified)).append(":{\"textures\":[")
                            .append(quote("textures/items/" + id)).append("]}");
                    definitions.append("{\"type\":\"definition\",\"model\":").append(quote(qualified))
                            .append(",\"bedrock_identifier\":").append(quote(qualified))
                            .append(",\"display_name\":").append(quote(names.get(d)))
                            .append(",\"bedrock_options\":{\"icon\":").append(quote(qualified))
                            .append("},\"components\":{\"minecraft:max_stack_size\":64}}");
                    put(zip, "textures/items/" + id + ".png", images.get(d));
                }
                put(zip, "textures/item_texture.json", textures.append("}}").toString().getBytes(StandardCharsets.UTF_8));
            }
            Files.writeString(temporaryMapping, definitions.append("]}}").toString(), StandardCharsets.UTF_8);
            Files.move(temporary, pack, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryMapping, mapping, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
            Files.deleteIfExists(temporaryMapping);
        }
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        var entry = new ZipEntry(name);
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 32 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
