package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

/**
 * Stages the currently supported free-ownership saved Entity holders in one
 * operation. Does not complete the source: equipment, inventories, owner
 * adapters, loaded entities and snapshot consistency remain unresolved.
 */
public final class FreeEntitySettlementImport {
    private FreeEntitySettlementImport() {}

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
            SavedEntityUuidIntegrity.validate(chunk);
        } catch (IOException failure) {
            settlement.failSource(source, "Saved Entity identity invalid: " + failure.getMessage());
            throw failure;
        }
        DroppedItemSettlementImport.stageChunk(settlement, source, world, chunk, seenWallets);
        ItemFrameSettlementImport.stageChunk(settlement, source, world, chunk, seenWallets);
        ItemDisplaySettlementImport.stageChunk(settlement, source, world, chunk, seenWallets);
    }
}
