package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemFrameSettlementImportTest {
    @Test void itemFrameCurrencyStagesAsFreeWithoutCompletingEntitySource() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 50))));
        Map<String, Object> frame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 10}, "Item", coin);
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        ItemFrameSettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", List.of(frame)));
        assertThrows(IllegalStateException.class, settlement::commit);
        settlement.completeSource("entities:file");
        assertEquals(BigInteger.valueOf(100), settlement.commit().free());
    }
}
