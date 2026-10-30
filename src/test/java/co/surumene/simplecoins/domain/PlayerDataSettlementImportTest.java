package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerDataSettlementImportTest {
    @Test void playerdataAssetsCountEvenWhenEveryWorldIsExcluded() throws Exception {
        UUID owner = UUID.randomUUID();
        String source = "playerdata:" + owner;
        var settlement = new SettlementAccumulator(new WorldScope(false, Set.of(), Set.of()),
                Set.of(source));
        Map<String,Object> coin = Map.of("Slot", (byte) 0, "id", "minecraft:paper",
                "count", 2, "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 10))));
        Map<String,Object> player = Map.of("Inventory", List.of(coin), "EnderItems", List.of());
        PlayerDataSettlementImport.stage(settlement, source, owner, false, player);
        var result = settlement.commit();
        assertEquals(BigInteger.valueOf(20), result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(20), result.personalBalances().get(owner));
        assertEquals(BigInteger.ZERO, result.dead());
    }
}
