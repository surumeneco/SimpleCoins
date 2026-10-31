package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolvedEntitySettlementImportTest {
    @Test void stagesEquipmentAndInventoryOnlyWithExplicitOwnerResolution() throws Exception {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-00000000002a");
        Map<String, Object> coin = Map.of("id", "minecraft:paper",
                "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        Map<String, Object> wolf = Map.of("id", "minecraft:wolf",
                "UUID", new int[]{0, 0, 0, 70},
                "equipment", Map.of("mainhand", coin));
        Map<String, Object> boat = Map.of("id", "minecraft:oak_chest_boat",
                "UUID", new int[]{0, 0, 0, 71},
                "Items", List.of(Map.of("Slot", (byte) 0, "id", "minecraft:paper",
                        "components", coin.get("components"))));
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("entities:file"));
        var ownership = Map.of(
                "00000000-0000-0000-0000-000000000046",
                new ResolvedEntitySettlementImport.Ownership(owner, false, true),
                "00000000-0000-0000-0000-000000000047",
                new ResolvedEntitySettlementImport.Ownership(null, false, false));
        ResolvedEntitySettlementImport.stageChunk(settlement, "entities:file", "world",
                Map.of("Entities", List.of(wolf, boat)), ownership);
        settlement.completeSource("entities:file");
        var result = settlement.commit();
        assertEquals(BigInteger.valueOf(100), result.personalBalances().get(owner));
        assertEquals(BigInteger.valueOf(100), result.free());
    }
}
