package co.surumene.simplecoins.paper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only, one-source valuation of serialized items already returned by a
 * verified WWC/WWW WCL reader. It never discovers entities, resolves ownership,
 * selects LIVE over SAVED data, closes a global generation or publishes money.
 */
public final class PaperWonderfulSnapshotAssets {
    private PaperWonderfulSnapshotAssets() {}

    /**
     * One value per physical provider slot (including verified zero-valued
     * items). Nested shulker items and wallets use the existing canonical
     * saved-NBT evaluator; wallet UUIDs are unique within this selected source.
     *
     * All calls must run on Paper's primary thread, as the WCL reader does.
     * A malformed/ambiguous item is a failure, never an observed empty source.
     */
    public static Map<String, BigInteger> slotValues(
            PaperWonderfulLoadedEvidence.Snapshot source,
            int maxSerializedBytes, int maxInflatedBytes) throws IOException {
        // This convenience path is local-only: it cannot establish wallet
        // uniqueness relative to another holder or a vanilla/LIVE source.
        return slotValues(source, maxSerializedBytes, maxInflatedBytes, new HashSet<>());
    }

    /**
     * Share the EXACT mutable identity context of the enclosing candidate
     * generation with other WCL, saved NBT and LIVE source readers. A duplicate
     * UUID anywhere in that selected generation fails rather than counting
     * the same physical wallet balance twice. On any error the caller MUST
     * discard the whole generation and this mutated identity set.
     */
    public static Map<String, BigInteger> slotValues(
            PaperWonderfulLoadedEvidence.Snapshot source,
            int maxSerializedBytes, int maxInflatedBytes,
            Set<UUID> seenWalletIds) throws IOException {
        Objects.requireNonNull(source, "WCL source snapshot");
        Objects.requireNonNull(seenWalletIds, "shared physical wallet UUID generation");
        Set<String> slots = new HashSet<>();
        Map<String, BigInteger> amounts = new LinkedHashMap<>();
        for (PaperWonderfulLoadedEvidence.StoredItem item : source.items()) {
            if (item == null || item.slot() == null || item.slot().isBlank()
                    || !slots.add(item.slot()))
                throw new IOException("Invalid or duplicate WCL source slot");
            BigInteger amount = PaperLiveItemValue.serialized(
                    item.bytes(), maxSerializedBytes, maxInflatedBytes, seenWalletIds);
            amounts.put(item.slot(), amount);
        }
        return Map.copyOf(amounts);
    }
}
