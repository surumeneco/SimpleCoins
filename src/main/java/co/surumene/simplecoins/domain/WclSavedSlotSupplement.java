package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Attach an independently verified SAVED WCL slot to the *existing* saved
 * ENTITY Anvil source while it is still open. Does not introduce any new
 * settlement source or independently validate the WCL provider observation.
 *
 * This is candidate-only. It does not freeze writers, resolve an atomic
 * LIVE/SAVED generation, authorize publication, or grant admission.
 */
public final class WclSavedSlotSupplement {
    private WclSavedSlotSupplement() {}

    public static void stage(
            SettlementAccumulator pending, String source, String world,
            UUID entityId, String providerId, Map<String, BigInteger> slots,
            WclOwnerEvidence.Decision ownerEvidence,
            Set<UUID> currentlyBanned, Set<UUID> existingPlayerdata) throws IOException {
        Objects.requireNonNull(pending, "existing saved Entity transaction");
        Objects.requireNonNull(entityId, "verified saved Entity UUID");
        Objects.requireNonNull(slots, "readStored Success slot values");
        Objects.requireNonNull(ownerEvidence, "independent saved owner evidence");
        Objects.requireNonNull(currentlyBanned, "verified current BAN source");
        Objects.requireNonNull(existingPlayerdata, "same Manifest playerdata");
        if (source == null || source.isBlank() || world == null || world.isBlank())
            throw new IOException("Missing saved Entity world/source identity");
        if (ownerEvidence.kind() == WclOwnerEvidence.Kind.HOLD)
            throw new IOException("Unresolved SAVED WCL owner for " + entityId);
        var aliases = WclPhysicalSlotIdentity.keys(entityId, providerId, slots.keySet());
        // Validate every slot before accepting any. Even if zero currency is
        // observed the provider's slot remains a distinct positive observation,
        // not a claim that the whole WCL inventory is empty.
        for (var slot : slots.entrySet()) {
            if (slot.getValue() == null || slot.getValue().signum() < 0)
                throw new IOException("Invalid SAVED WCL currency value");
            String key = aliases.get(slot.getKey());
            if (slot.getValue().signum() > 0 &&
                    (key.endsWith("/equipment/mainhand") || key.endsWith("/equipment/head")))
                throw new IOException("SAVED WCL equipment mirror lacks item equivalence evidence: " + key);
        }
        UUID owner = ownerEvidence.owner();
        for (var slot : slots.entrySet()) {
            if (slot.getValue().signum() == 0) continue;
            String physical = aliases.get(slot.getKey());
            var asset = new SettlementAccumulator.Asset(
                    source, physical, world, owner, false,
                    owner != null && currentlyBanned.contains(owner),
                    owner != null && existingPlayerdata.contains(owner),
                    slot.getValue(), "world/" + world + "/" + physical);
            try {
                pending.accept(asset); // canonical dedup against native Entity slots
            } catch (RuntimeException invalid) {
                // The caller discards the entire pending generation on error.
                throw new IOException("SAVED WCL asset alias/source rejected: " + physical, invalid);
            }
        }
    }
}
