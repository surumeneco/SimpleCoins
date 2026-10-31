package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Validates the identity of every saved Entity in a stable chunk snapshot.
 * Individual inventory/equipment readers cannot detect aliases across
 * different Entity types, so this check must precede value staging.
 */
public final class SavedEntityUuidIntegrity {
    private SavedEntityUuidIntegrity() {}

    public static void validate(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");

        Set<UUID> seen = new HashSet<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved Entity identity");
            Object rawUuid = entity.get("UUID");
            if (!(rawUuid instanceof int[] parts) || parts.length != 4)
                throw new IOException("Saved Entity missing valid UUID: " + id);
            UUID uuid = new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                    ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
            if (!seen.add(uuid))
                throw new IOException("Duplicate saved Entity UUID: " + uuid);
        }
    }
}
