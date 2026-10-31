package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

/**
 * Stages the displayed currency of saved Java item frames as free assets.
 * The Entity source must remain incomplete until all other Entity types,
 * loaded-memory reconciliation and ownership adapters have been checked.
 */
public final class ItemFrameSettlementImport {
    private ItemFrameSettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk) throws IOException {
        Objects.requireNonNull(settlement);
        try {
            if (world == null || world.isBlank())
                throw new IOException("A physical world identity is required");
            Map<String, BigInteger> assets = StoredItemFrameAssets.extract(chunk);
            for (var entry : assets.entrySet()) {
                String location = "world/" + world + "/item_frame/" + entry.getKey() + "/slot/0";
                // An Entity UUID is global across worlds and entity types.
                // The dropped-item importer uses the same physical key namespace.
                String physicalKey = "entity/" + entry.getKey();
                settlement.accept(new SettlementAccumulator.Asset(
                        source, physicalKey, world, null, false, false, false,
                        entry.getValue(), location));
            }
        } catch (IOException failure) {
            settlement.failSource(source, "Item frame chunk unreadable: " + failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            settlement.failSource(source, "Item frame chunk rejected: " + failure.getMessage());
            throw failure;
        }
    }
}
