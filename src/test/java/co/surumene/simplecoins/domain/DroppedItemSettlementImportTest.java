package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DroppedItemSettlementImportTest {
    @Test void droppedCurrencyStagesAsFreeWithoutCompletingTheEntitySource() throws Exception {
        Map<String,Object> coin = Map.of("id", "minecraft:paper", "count", 2,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String,Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 7}, "Item", coin);
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        DroppedItemSettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", List.of(dropped)));
        assertThrows(IllegalStateException.class, settlement::commit);
        settlement.completeSource("entities:file");
        var result = settlement.commit();
        assertEquals(BigInteger.valueOf(200), result.free());
        assertEquals(BigInteger.valueOf(200), result.statistics().worldTotal());
    }
}
