package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Physical-slot collision keys for the shipped WWC/WWW WCL storage views.
 *
 * A WWW weapon is also rendered into the same wolf's vanilla main hand by
 * WonderfulWolfWeaponRuntime.syncWeapon. The WCL and vanilla observations
 * must NOT be independently added to a monetary settlement. Use the matching
 * collision key to reject an unproven double view; only an independently
 * verified equivalence may coalesce those views into one asset.
 *
 * This is NOT evidence of equivalent contents, complete Entity discovery,
 * ownership, a frozen writer epoch, or permission to publish a settlement.
 */
public final class WclPhysicalSlotIdentity {
    private WclPhysicalSlotIdentity() {}

    public static Map<String, String> keys(UUID entityId, String providerId,
                                           Collection<String> slots) throws IOException {
        Objects.requireNonNull(entityId, "verified Entity UUID");
        Objects.requireNonNull(providerId, "verified provider ID");
        Objects.requireNonNull(slots, "verified WCL slots");
        Map<String, String> identities = new LinkedHashMap<>();
        String entity = "entity/" + entityId;
        for (String slot : slots) {
            if (slot == null || slot.isBlank())
                throw new IOException("Blank WCL storage slot");
            String physical;
            if ("wwc".equals(providerId) && "shulker".equals(slot)) {
                // Distinct carried item; never confuse it with chicken head.
                physical = entity + "/wcl/wwc/shulker";
            } else if ("wwc".equals(providerId) && "carpet".equals(slot)) {
                physical = entity + "/wcl/wwc/carpet";
            } else if ("wwc".equals(providerId) && "head".equals(slot)) {
                // Shipped WWC syncs headItem into this Chicken's helmet.
                // This is an alias detection key, not evidence of value equality.
                physical = entity + "/equipment/head";
            } else if ("www".equals(providerId) && "weapon".equals(slot)) {
                physical = entity + "/equipment/mainhand";
            } else if ("www".equals(providerId) && slot.startsWith("cargo:")) {
                String index = slot.substring("cargo:".length());
                if (!index.matches("0|[1-9][0-9]*"))
                    throw new IOException("Noncanonical WWW cargo slot: " + slot);
                try {
                    Integer.parseInt(index);
                } catch (NumberFormatException invalid) {
                    throw new IOException("Invalid WWW cargo slot: " + slot, invalid);
                }
                physical = entity + "/wcl/www/" + slot;
            } else {
                throw new IOException("Unknown WCL physical slot: " + providerId + "/" + slot);
            }
            if (identities.putIfAbsent(slot, physical) != null)
                throw new IOException("Repeated WCL physical slot: " + providerId + "/" + slot);
        }
        return Map.copyOf(identities);
    }
}
