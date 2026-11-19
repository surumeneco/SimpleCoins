package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saved vanilla tame-owner hints only. An Entity Owner tag is not generally
 * economic ownership: dropped items use it for delivery targets, projectiles
 * for their shooter. Caller must resolve registration, bans, WWC/WWW commander
 * precedence and loaded-Entity state before any asset classification.
 */
public final class SavedEntityOwner {
    private static final Set<String> TAMEABLES = Set.of(
            "minecraft:wolf", "minecraft:cat", "minecraft:parrot",
            "minecraft:horse", "minecraft:donkey", "minecraft:mule",
            "minecraft:llama", "minecraft:trader_llama",
            "minecraft:skeleton_horse", "minecraft:zombie_horse");

    private SavedEntityOwner() {}

    /**
     * Read the independently saved vanilla tame-owner observation for exactly
     * one WWC/WWW WCL candidate. The caller must prove this chunk is from the
     * same selected, immutable source generation; parsing cannot prove that.
     * A verified normal Chicken is not a tameable, unlike a saved Wolf.
     */
    public static WclOwnerEvidence.TameOwner wclTameObservation(
            Map<String, Object> chunk, UUID target, String providerId) throws IOException {
        if (target == null) throw new IOException("Missing selected WCL Entity UUID");
        String species;
        if ("wwc".equals(providerId)) species = "minecraft:chicken";
        else if ("www".equals(providerId)) species = "minecraft:wolf";
        else throw new IOException("Unknown provider for saved tame observation: " + providerId);

        // Validate ALL saved Entity UUIDs before inferring positive absence.
        if (!SavedEntityUuidIntegrity.identities(chunk).contains(target))
            throw new IOException("Selected WCL Entity absent from saved chunk: " + target);
        List<?> entities = (List<?>) chunk.get("Entities");
        boolean found = false;
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity))
                throw new IOException("Malformed saved Entity in WCL tame source");
            UUID id = uuid(entity.get("UUID"), "selected WCL Entity UUID");
            if (!target.equals(id)) continue;
            if (!species.equals(entity.get("id")))
                throw new IOException("Selected WCL Entity has the wrong saved species");
            found = true;
            break;
        }
        if (!found) throw new IOException("Selected WCL Entity disappeared during read");
        if ("wwc".equals(providerId)) return WclOwnerEvidence.TameOwner.verifiedAbsent();
        UUID owner = extract(chunk).get(target.toString());
        return owner == null ? WclOwnerEvidence.TameOwner.verifiedAbsent()
                : WclOwnerEvidence.TameOwner.verified(owner);
    }

    public static Map<String, UUID> extract(Map<String, Object> chunk) throws IOException {
        if (chunk == null || !(chunk.get("Entities") instanceof List<?> entities))
            throw new IOException("Missing saved entity chunk Entities list");
        Map<String, UUID> owners = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (Object raw : entities) {
            if (!(raw instanceof Map<?, ?> entity) ||
                    !(entity.get("id") instanceof String id) || id.isBlank())
                throw new IOException("Malformed saved entity");
            if (!TAMEABLES.contains(id)) continue;
            UUID uuid = uuid(entity.get("UUID"), "tameable Entity UUID");
            String key = uuid.toString();
            if (!seen.add(key))
                throw new IOException("Duplicate tameable Entity UUID: " + key);
            if (!entity.containsKey("Owner")) continue;
            owners.put(key, uuid(entity.get("Owner"), "tameable Entity Owner"));
        }
        return Map.copyOf(owners);
    }

    private static UUID uuid(Object raw, String label) throws IOException {
        if (!(raw instanceof int[] parts) || parts.length != 4)
            throw new IOException("Invalid " + label);
        return new UUID(((long) parts[0] << 32) | Integer.toUnsignedLong(parts[1]),
                ((long) parts[2] << 32) | Integer.toUnsignedLong(parts[3]));
    }
}
