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
        stage(pending, source, world, entityId, providerId, slots,
                ownerEvidence, currentlyBanned, existingPlayerdata, Set.of());
    }

    /**
     * Additional item-level equivalences supplied only after comparing the
     * actual selected SAVED Entity equipment NBT to WCL StoredItem bytes.
     * A proven mirror REUSES its existing native accounting Asset; it is not
     * staged a second time. Every other positive equipment mirror still HOLDS.
     */
    public static void stage(
            SettlementAccumulator pending, String source, String world,
            UUID entityId, String providerId, Map<String, BigInteger> slots,
            WclOwnerEvidence.Decision ownerEvidence,
            Set<UUID> currentlyBanned, Set<UUID> existingPlayerdata,
            Set<String> verifiedMirrorSlots) throws IOException {
        Objects.requireNonNull(verifiedMirrorSlots, "exact saved equipment mirror evidence");
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
        // A mirror proof can only name a positive equipment item in this
        // very source. It cannot suppress new independent slots or 0C values.
        for (String proved : verifiedMirrorSlots) {
            String alias = aliases.get(proved);
            BigInteger amount = slots.get(proved);
            if (alias == null || amount == null || amount.signum() <= 0 ||
                    !(alias.endsWith("/equipment/mainhand") || alias.endsWith("/equipment/head")))
                throw new IOException("Unbound or non-equipment SAVED WCL mirror proof: " + proved);
        }
        for (var slot : slots.entrySet()) {
            if (slot.getValue() == null || slot.getValue().signum() < 0)
                throw new IOException("Invalid SAVED WCL currency value");
            String key = aliases.get(slot.getKey());
            if (slot.getValue().signum() > 0 &&
                    (key.endsWith("/equipment/mainhand") || key.endsWith("/equipment/head")) &&
                    !verifiedMirrorSlots.contains(slot.getKey()))
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
                if (verifiedMirrorSlots.contains(slot.getKey())) {
                    // ItemStack structural identity was already proven by
                    // the selected Entity NBT + WCL snapshot adapter. Require
                    // exact existing source/owner/classification/value too.
                    pending.requireExistingEquivalentAsset(asset);
                } else {
                    pending.accept(asset);
                }
            } catch (RuntimeException invalid) {
                throw new IOException("SAVED WCL asset alias/source rejected: " + physical, invalid);
            }
        }
    }
}
