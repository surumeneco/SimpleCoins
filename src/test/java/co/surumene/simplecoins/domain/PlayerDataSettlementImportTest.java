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
    @Test void currentlyBannedPlayerIsExcludedFromWorldTotalAndIndexedAsDead() throws Exception {
        UUID owner = UUID.randomUUID();
        String source = "playerdata:" + owner;
        var settlement = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of(source));
        Map<String,Object> coin = Map.of("Slot", (byte) 0, "id", "minecraft:paper",
                "count", 3, "components", Map.of("minecraft:custom_data",
                        Map.of("PublicBukkitValues", Map.of("simplecoins:denomination", 100))));
        PlayerDataSettlementImport.stage(settlement, source, owner, true,
                Map.of("Inventory", List.of(coin), "EnderItems", List.of()));
        var result = settlement.commit();
        assertEquals(BigInteger.ZERO, result.statistics().worldTotal());
        assertEquals(BigInteger.valueOf(300), result.dead());
        assertEquals(1, result.deadLocations().size());
        assertTrue(result.deadLocations().getFirst().location().contains("inventory:0"));
    }

    @Test void unreadableSavedInventoryPreventsSettlementCommit() {
        UUID owner = UUID.randomUUID();
        String source = "playerdata:" + owner;
        var settlement = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of(source));
        assertThrows(java.io.IOException.class, () -> PlayerDataSettlementImport.stage(
                settlement, source, owner, false, Map.of("Inventory", List.of())));
        assertThrows(IllegalStateException.class, settlement::commit);
    }


    @Test void previouslyRegisteredPlayerWithNoCurrencyRemainsRegistered() throws Exception {
        UUID player = UUID.randomUUID();
        String source = "playerdata:" + player;
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of(source));
        // 02A: a zero-balance player is retained only after an earlier asset
        // transaction registered that UUID. Playerdata alone is not registration.
        settlement.preservePreviouslyRegistered(player, false, true);
        PlayerDataSettlementImport.stage(settlement, source, player, false,
                Map.of("Inventory", List.of(), "EnderItems", List.of()));
        var snapshot = settlement.commit();
        assertEquals(Map.of(player, BigInteger.ZERO), snapshot.personalBalances());
        assertEquals(1, snapshot.statistics().registeredPlayers());
    }

    @Test void playerdataWithoutPriorAssetTransactionDoesNotRegisterPlayer() throws Exception {
        UUID player = UUID.randomUUID();
        String source = "playerdata:" + player;
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of(source));
        PlayerDataSettlementImport.stage(settlement, source, player, false,
                Map.of("Inventory", List.of(), "EnderItems", List.of()));
        var snapshot = settlement.commit();
        assertEquals(Map.of(), snapshot.personalBalances());
        assertEquals(0, snapshot.statistics().registeredPlayers());
    }

    @Test void bannedPlayerWithNoCurrencyIsNotRegistered() throws Exception {
        UUID player = UUID.randomUUID();
        String source = "playerdata:" + player;
        var settlement = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of(source));
        PlayerDataSettlementImport.stage(settlement, source, player, true,
                Map.of("Inventory", List.of(), "EnderItems", List.of()));
        var snapshot = settlement.commit();
        assertEquals(Map.of(), snapshot.personalBalances());
        assertEquals(0, snapshot.statistics().registeredPlayers());
    }
}
