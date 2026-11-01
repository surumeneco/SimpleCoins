package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only saved BlockLocker sign evidence. Neither saved PDC hints nor raw
 * sign text prove attachment, protection or economic ownership. A future
 * authoritative resolver must reconcile them with the actual block topology.
 */
public final class BlockLockerSavedSignCandidates {
    private BlockLockerSavedSignCandidates() {}

    /** Saved PDC hints only; text-only legacy signs do not create owner hints. */
    public static Map<String, BlockLockerSavedHints.Hints> extract(
            Map<String, Object> chunk) throws IOException {
        Map<String, BlockLockerSavedHints.Hints> found = new LinkedHashMap<>();
        for (var entry : savedSigns(chunk).entrySet()) {
            String position = entry.getKey();
            Map<String, Object> block = entry.getValue();
            if (!block.containsKey("PublicBukkitValues")) continue;
            Object rawPdc = block.get("PublicBukkitValues");
            if (!(rawPdc instanceof Map<?, ?> pdc) ||
                    pdc.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed saved sign PDC at " + position);
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) pdc;
            Optional<BlockLockerSavedHints.Hints> hints = BlockLockerSavedHints.read(values);
            hints.ifPresent(value -> found.put(position, value));
        }
        return Map.copyOf(found);
    }

    /** Raw modern/legacy text evidence, including signs with no saved PDC. */
    public static Map<String, BlockLockerSavedSignText.Evidence> extractTextCandidates(
            Map<String, Object> chunk) throws IOException {
        Map<String, BlockLockerSavedSignText.Evidence> found = new LinkedHashMap<>();
        for (var entry : savedSigns(chunk).entrySet()) {
            Optional<BlockLockerSavedSignText.Evidence> text =
                    BlockLockerSavedSignText.read(entry.getValue());
            text.ifPresent(value -> found.put(entry.getKey(), value));
        }
        return Map.copyOf(found);
    }

    /**
     * Validate physical sign positions before either evidence stream filters
     * them. A legacy text-only sign is a real block, not an ignorable duplicate.
     */
    private static Map<String, Map<String, Object>> savedSigns(
            Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("block_entities") instanceof List<?> entities))
            throw new IOException("Missing saved terrain block_entities list");
        Map<String, Map<String, Object>> signs = new LinkedHashMap<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> block) ||
                    !(block.get("id") instanceof String id) || id.isBlank() ||
                    block.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed saved block entity");
            if (!id.equals("minecraft:sign") && !id.equals("minecraft:hanging_sign"))
                continue;
            int x = coordinate(block.get("x"));
            int y = coordinate(block.get("y"));
            int z = coordinate(block.get("z"));
            String position = x + "," + y + "," + z;
            if (chunk.containsKey("xPos") || chunk.containsKey("zPos")) {
                if (!(chunk.get("xPos") instanceof Integer chunkX) ||
                        !(chunk.get("zPos") instanceof Integer chunkZ) ||
                        Math.floorDiv(x, 16) != chunkX || Math.floorDiv(z, 16) != chunkZ)
                    throw new IOException("Saved sign outside declaring terrain chunk: " + position);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) block;
            if (signs.putIfAbsent(position, values) != null)
                throw new IOException("Duplicate saved sign position: " + position);
        }
        return signs;
    }

    private static int coordinate(Object raw) throws IOException {
        if (!(raw instanceof Integer value))
            throw new IOException("Missing or invalid saved sign coordinates");
        return value;
    }
}
