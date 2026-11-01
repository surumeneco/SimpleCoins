package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only saved BlockLocker sign candidates. A candidate is not proof of
 * attachment, valid display text, or economic ownership; the authoritative
 * resolver must reconcile those before staging any assets.
 */
public final class BlockLockerSavedSignCandidates {
    private BlockLockerSavedSignCandidates() {}

    public static Map<String, BlockLockerSavedHints.Hints> extract(
            Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("block_entities") instanceof List<?> entities))
            throw new IOException("Missing saved terrain block_entities list");
        Map<String, BlockLockerSavedHints.Hints> found = new LinkedHashMap<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> block) ||
                    !(block.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved block entity");
            if (!id.equals("minecraft:sign") && !id.equals("minecraft:hanging_sign"))
                continue;
            String position = coordinate(block.get("x")) + "," +
                    coordinate(block.get("y")) + "," + coordinate(block.get("z"));
            if (!block.containsKey("PublicBukkitValues")) continue;
            Object rawPdc = block.get("PublicBukkitValues");
            if (!(rawPdc instanceof Map<?, ?> pdc) ||
                    pdc.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed saved sign PDC at " + position);
            @SuppressWarnings("unchecked")
            Map<String, Object> values = (Map<String, Object>) pdc;
            Optional<BlockLockerSavedHints.Hints> hints = BlockLockerSavedHints.read(values);
            if (hints.isPresent() && found.putIfAbsent(position, hints.get()) != null)
                throw new IOException("Duplicate saved BlockLocker sign candidate at " + position);
        }
        return Map.copyOf(found);
    }

    private static int coordinate(Object raw) throws IOException {
        if (!(raw instanceof Integer value))
            throw new IOException("Missing or invalid saved sign coordinates");
        return value;
    }
}
