package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BlockEntitySettlementImportTest {
    private static Map<String,Object> chest(int x, int amount) {
        return Map.of("id", "minecraft:chest", "x", x, "y", 64, "z", 2,
                "Items", List.of(Map.of("Slot", (byte) 0,
                        "id", "minecraft:paper", "count", amount,
                        "components", Map.of("minecraft:custom_data",
                                Map.of("PublicBukkitValues",
                                        Map.of("simplecoins:denomination", 100))))));
    }

    @Test void stagesKnownContainerOwnerWithoutTreatingTheChunkAsACompletedSource() throws Exception {
        UUID owner = UUID.randomUUID();
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("region:file"));
        BlockEntitySettlementImport.stageChunk(settlement, "region:file", "world",
                Map.of("block_entities", List.of(chest(10, 2))),
                position -> new BlockEntitySettlementImport.Ownership(owner, false, false, true));
        assertThrows(IllegalStateException.class, settlement::commit);
        settlement.completeSource("region:file");
        var result = settlement.commit();
        assertEquals(BigInteger.valueOf(200), result.personalBalances().get(owner));
        assertEquals(BigInteger.ZERO, result.free());
    }

    @Test void allAccessStorageIsAlwaysClassifiedAsFree() throws Exception {
        UUID owner = UUID.randomUUID();
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("region:file"));
        BlockEntitySettlementImport.stageChunk(settlement, "region:file", "world",
                Map.of("block_entities", List.of(chest(10, 2))),
                position -> new BlockEntitySettlementImport.Ownership(owner, true, false, true));
        settlement.completeSource("region:file");
        assertEquals(BigInteger.valueOf(200), settlement.commit().free());
    }

    @Test void unresolvedContainerOwnerAbortsTheWholeSettlement() {
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("region:file"));
        assertThrows(IOException.class, () ->
                BlockEntitySettlementImport.stageChunk(settlement, "region:file", "world",
                        Map.of("block_entities", List.of(chest(10, 1))),
                        position -> { throw new IOException("BlockLocker protection not resolved"); }));
        assertThrows(IllegalStateException.class, settlement::commit);
    }
}
