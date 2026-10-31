package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FreeEntitySettlementImportTest {
    @Test void emptyFrameCannotAliasCurrencyBearingDroppedEntityUuid() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 80}, "Item", coin);
        Map<String, Object> emptyFrame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 80});
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));

        assertThrows(java.io.IOException.class, () ->
                FreeEntitySettlementImport.stageChunk(settlement, "entities:file", "world",
                        Map.of("Entities", List.of(dropped, emptyFrame))));
        assertThrows(IllegalStateException.class, () -> settlement.completeSource("entities:file"));
        assertThrows(IllegalStateException.class, settlement::commit);
    }
}
