package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemDisplaySettlementImportTest {
    @Test void itemDisplayCurrencyStagesAsFreeWithoutCompletingEntitySource() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> display = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 50}, "item", coin);
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        ItemDisplaySettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", List.of(display)));
        assertThrows(IllegalStateException.class, settlement::commit);
        settlement.completeSource("entities:file");
        assertEquals(BigInteger.valueOf(100), settlement.commit().free());
    }

    @Test void itemDisplayAndDroppedItemSharingUuidInvalidateSettlement() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> display = Map.of("id", "minecraft:item_display",
                "UUID", new int[]{0, 0, 0, 50}, "item", coin);
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 50}, "Item", coin);
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        DroppedItemSettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", List.of(dropped)));
        assertThrows(IllegalArgumentException.class, () ->
                ItemDisplaySettlementImport.stageChunk(settlement, "entities:file", "world",
                        Map.of("Entities", List.of(display))));
        assertThrows(IllegalStateException.class, settlement::commit);
    }

    @Test void freeEntityAdapterStagesAllThreeSupportedEntityTypes() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        var entities = List.of(
                Map.of("id", "minecraft:item", "UUID", new int[]{0, 0, 0, 60}, "Item", coin),
                Map.of("id", "minecraft:item_frame", "UUID", new int[]{0, 0, 0, 61}, "Item", coin),
                Map.of("id", "minecraft:item_display", "UUID", new int[]{0, 0, 0, 62}, "item", coin));
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        FreeEntitySettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", entities));
        assertThrows(IllegalStateException.class, settlement::commit);
        settlement.completeSource("entities:file");
        assertEquals(BigInteger.valueOf(300), settlement.commit().free());
    }

    @Test void failedFrameAfterStagedDropCannotPublishPartialEntityTotal() throws Exception {
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> dropped = Map.of("id", "minecraft:item",
                "UUID", new int[]{0, 0, 0, 60}, "Item", coin);
        Map<String, Object> corruptFrame = Map.of("id", "minecraft:item_frame",
                "UUID", new int[]{0, 0, 0, 61}, "Item", "corrupt");
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        assertThrows(java.io.IOException.class, () ->
                FreeEntitySettlementImport.stageChunk(settlement, "entities:file", "world",
                        Map.of("Entities", List.of(dropped, corruptFrame))));
        assertThrows(IllegalStateException.class, settlement::commit);
    }
}
