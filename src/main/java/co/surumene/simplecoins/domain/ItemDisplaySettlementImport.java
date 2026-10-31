package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * Stages saved item_display currency as free assets. This is only one Entity
 * storage type; all other holders and live/disk reconciliation must succeed
 * before the caller may complete the Entity settlement source.
 */
public final class ItemDisplaySettlementImport {
    private ItemDisplaySettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk) throws IOException {
        Objects.requireNonNull(settlement);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Map<String, BigInteger> assets = StoredItemDisplayAssets.extract(chunk);
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
