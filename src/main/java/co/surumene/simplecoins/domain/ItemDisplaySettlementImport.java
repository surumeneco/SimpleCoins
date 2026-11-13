package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

/**
 * Stages saved item_display currency as free assets. This is only one Entity
 * storage type; all other holders and live/disk reconciliation must succeed
 * before the caller may complete the Entity settlement source.
 */
public final class ItemDisplaySettlementImport {
    private ItemDisplaySettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk) throws IOException {
        stageChunk(settlement, source, world, chunk, new java.util.HashSet<>());
    }

    /** For one whole saved-data generation, share wallet UUIDs across sources. */
    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk,
                                  Set<UUID> seenWallets) throws IOException {
        Objects.requireNonNull(seenWallets, "saved wallet identities");
        Objects.requireNonNull(settlement);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Map<String, BigInteger> assets = StoredItemDisplayAssets.extract(chunk, seenWallets);
            for (var entry : assets.entrySet()) {
                String location = "world/" + world + "/item_display/" + entry.getKey() + "/slot/0";
                // UUIDs are global physical identities across Entity types/worlds.
                String physicalKey = "entity/" + entry.getKey();
                settlement.accept(new SettlementAccumulator.Asset(
                        source, physicalKey, world, null, false, false, false,
                        entry.getValue(), location));
            }
        } catch (IOException failure) {
            settlement.failSource(source, "Item display chunk unreadable: " + failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            settlement.failSource(source, "Item display chunk rejected: " + failure.getMessage());
            throw failure;
        }
    }
}
