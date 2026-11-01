package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminSettlementQueryTest {
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000027");

    private static SettlementAccumulator.Result completed() {
        var acc = new SettlementAccumulator(
                new WorldScope(true, Set.of(), Set.of()), Set.of("source"));
        acc.preservePreviouslyRegistered(PLAYER, false, true);
        acc.accept(new SettlementAccumulator.Asset(
                "source", "free-slot", "world", null, false, false, false,
                BigInteger.TEN, "world:0,64,0"));
        acc.accept(new SettlementAccumulator.Asset(
                "source", "dead-slot", "world", PLAYER, false, true, true,
                BigInteger.valueOf(20), "world:1,64,0"));
        acc.completeSource("source");
        return acc.commit();
    }

    @Test void committedTotalsAndDeadReasonAreAvailableWithoutMutation() throws Exception {
        var result = completed();
        var stats = AdminSettlementQuery.statistics(result);
        assertEquals(1, stats.registeredPlayers());
        assertEquals(BigInteger.TEN, stats.free());
        assertEquals(BigInteger.valueOf(20), stats.dead());
        assertEquals(BigInteger.ZERO,
                AdminSettlementQuery.personalBalance(result, PLAYER).orElseThrow());
        var dead = AdminSettlementQuery.deadLocationsFor(result, PLAYER);
        assertEquals(1, dead.size());
        assertEquals(AssetOwner.DeadReason.CURRENT_BAN, dead.getFirst().reason());
        assertEquals("world:1,64,0", dead.getFirst().location());
        assertThrows(UnsupportedOperationException.class, () -> dead.add(dead.getFirst()));
    }

    @Test void unknownPlayerIsNotRegisteredZero() throws Exception {
        var result = completed();
        assertTrue(AdminSettlementQuery.personalBalance(result, UUID.randomUUID()).isEmpty());
        assertTrue(AdminSettlementQuery.deadLocationsFor(result, UUID.randomUUID()).isEmpty());
    }

    @Test void nullRegisteredPlayerIdentityFailsClosedEvenWithConsistentTotals() {
        var balances = new java.util.HashMap<UUID, BigInteger>();
        balances.put(null, BigInteger.ZERO);
        var result = new SettlementAccumulator.Result(
                AssetStatistics.calculate(balances, BigInteger.ZERO, BigInteger.ZERO),
                balances, BigInteger.ZERO, BigInteger.ZERO, List.of());
        assertThrows(IOException.class, () -> AdminSettlementQuery.statistics(result));
    }

    @Test void overlongDeadLocationEvidenceFailsClosed() {
        var valid = completed();
        var location = new SettlementAccumulator.DeadLocation(
                PLAYER, AssetOwner.DeadReason.CURRENT_BAN,
                BigInteger.valueOf(20), "x".repeat(1025));
        var result = new SettlementAccumulator.Result(
                valid.statistics(), valid.personalBalances(), valid.free(),
                valid.dead(), List.of(location));
        assertThrows(IOException.class, () -> AdminSettlementQuery.deadLocations(result));
    }

    @Test void inconsistentOrMissingSnapshotEvidenceFailsClosed() throws Exception {
        var result = completed();
        var wrongFree = new SettlementAccumulator.Result(
                result.statistics(), result.personalBalances(), BigInteger.valueOf(99),
                result.dead(), result.deadLocations());
        var missingDead = new SettlementAccumulator.Result(
                result.statistics(), result.personalBalances(), result.free(),
                result.dead(), List.of());
        assertThrows(IOException.class, () -> AdminSettlementQuery.statistics(wrongFree));
        assertThrows(IOException.class, () -> AdminSettlementQuery.deadLocationsFor(missingDead, PLAYER));
        assertThrows(IOException.class, () -> AdminSettlementQuery.statistics(null));
        assertThrows(IOException.class, () -> AdminSettlementQuery.personalBalance(result, null));
    }
}
