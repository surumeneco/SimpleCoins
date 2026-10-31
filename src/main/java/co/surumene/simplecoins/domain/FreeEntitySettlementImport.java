package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;

/**
 * Stages the currently supported free-ownership saved Entity holders in one
 * operation. Does not complete the source: equipment, inventories, owner
 * adapters, loaded entities and snapshot consistency remain unresolved.
 */
public final class FreeEntitySettlementImport {
    private FreeEntitySettlementImport() {}

    public static void stageChunk(SettlementAccumulator settlement, String source,
                                  String world, Map<String, Object> chunk) throws IOException {
        DroppedItemSettlementImport.stageChunk(settlement, source, world, chunk);
        ItemFrameSettlementImport.stageChunk(settlement, source, world, chunk);
        ItemDisplaySettlementImport.stageChunk(settlement, source, world, chunk);
    }
}
