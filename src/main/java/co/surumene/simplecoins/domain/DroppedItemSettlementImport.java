package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * Stages unloaded dropped-item currency as free assets. This is only one
 * entity type; the caller must reconcile loaded entities and scan all other
 * inventory-bearing Entity types before completing the authoritative source.
 */
public final class DroppedItemSettlementImport {
    private DroppedItemSettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk) throws IOException {
        Objects.requireNonNull(settlement);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Map<String, BigInteger> assets = StoredDroppedItemAssets.extract(chunk);
            for (var entry : assets.entrySet()) {
                String location = "world/" + world + "/entity/" + entry.getKey();
                settlement.accept(new SettlementAccumulator.Asset(
                        source, location, world, null, false, false, false,
                        entry.getValue(), location));
            }
            // Other chunks belong to the same source; completion is external.
        } catch (IOException failure) {
            settlement.failSource(source, "Dropped Entity chunk unreadable: " + failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            settlement.failSource(source, "Dropped Entity chunk rejected: " + failure.getMessage());
            throw failure;
        }
    }
}
