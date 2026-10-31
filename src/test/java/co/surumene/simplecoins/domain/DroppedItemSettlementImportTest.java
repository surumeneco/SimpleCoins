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

    @Test void duplicatedDroppedUuidAcrossWorldsCannotBePublishedTwice() throws Exception {
        Map<String,Object> coin = Map.of("id", "minecraft:paper", "count", 1,
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String,Object> entity = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 7}, "Item", coin);
        Map<String,Object> chunk = Map.of("Entities", List.of(entity));
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        DroppedItemSettlementImport.stageChunk(settlement, "entities:file", "world-a", chunk);
        assertThrows(IllegalArgumentException.class, () ->
                DroppedItemSettlementImport.stageChunk(settlement, "entities:file", "world-b", chunk),
                "Entity UUID is a physical identity, even when its storage world differs");
        assertThrows(IllegalStateException.class,
                () -> settlement.completeSource("entities:file"),
                "A failed entity source must never transition to complete");
        assertThrows(IllegalStateException.class, settlement::commit,
                "A duplicate Entity UUID must invalidate the entire staged settlement");
    }
}
