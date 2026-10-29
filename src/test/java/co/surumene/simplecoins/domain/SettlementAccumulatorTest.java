package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettlementAccumulatorTest {
    private static SettlementAccumulator.Asset asset(String origin, String slot, String world,
                                                     UUID owner, boolean everyone, boolean banned,
                                                     boolean playerdata, long value) {
        return new SettlementAccumulator.Asset(origin, slot, world, owner, everyone, banned, playerdata,
                BigInteger.valueOf(value), "storage/" + slot);
    }

    @Test void onlyPublishesWhenEverySourceWasCompleted() {
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("players", "regions", "entities"));
        scan.accept(asset("players", "u:inventory:0", null, UUID.randomUUID(), false, false, true, 5));
        scan.completeSource("players");
        assertThrows(IllegalStateException.class, scan::commit);
        scan.completeSource("regions");
        scan.failSource("entities", "Unreadable NBT");
        assertThrows(IllegalStateException.class, scan::commit);
        assertThrows(IllegalStateException.class, () -> scan.completeSource("entities"));
    }

    @Test void preventsDoubleCountingAndLateWrites() {
        var owner = UUID.randomUUID();
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("memory", "disk"));
        scan.accept(asset("memory", "world/chunk/slot", "world", owner, false, false, true, 10));
        assertThrows(IllegalArgumentException.class,
            () -> scan.accept(asset("disk", "world/chunk/slot", "world", owner, false, false, true, 10)));
        scan.completeSource("memory");
        assertThrows(IllegalStateException.class,
            () -> scan.accept(asset("memory", "other", "world", owner, false, false, true, 10)));
        scan.completeSource("disk");
        assertThrows(IllegalStateException.class, scan::commit);
        assertThrows(IllegalStateException.class, scan::commit);
    }

    @Test void rejectedUnknownSourcePermanentlyPreventsPublication() {
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()), Set.of("disk"));
        assertThrows(IllegalArgumentException.class,
            () -> scan.accept(asset("unknown", "slot", "world", null, false, false, false, 10)));
        scan.completeSource("disk");
        assertThrows(IllegalStateException.class, scan::commit);
    }

    @Test void cleanTransactionStillPublishesAndRejectsLateWrites() {
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()), Set.of("disk"));
        scan.accept(asset("disk", "slot", "world", null, false, false, false, 10));
        scan.completeSource("disk");
        assertEquals(BigInteger.TEN, scan.commit().statistics().worldTotal());
        assertThrows(IllegalStateException.class, () -> scan.completeSource("disk"));
    }

    @Test void classifiesDeadAndFreeWithoutInflatingWorldTotal() {
        var p = UUID.randomUUID();
        var banned = UUID.randomUUID();
        var missing = UUID.randomUUID();
        var scan = new SettlementAccumulator(new WorldScope(false, Set.of("included"), Set.of()),
                Set.of("all"));
        scan.accept(asset("all", "a", "included", p, false, false, true, 100));
        scan.accept(asset("all", "b", "included", banned, false, true, true, 50));
        scan.accept(asset("all", "c", "included", missing, false, false, false, 25));
        scan.accept(asset("all", "d", "included", p, true, false, true, 10));
        scan.accept(asset("all", "e", "excluded", p, false, false, true, 999));
        scan.accept(asset("all", "f", null, null, false, false, false, 5));
        scan.completeSource("all");
        var r = scan.commit();
        assertEquals(BigInteger.valueOf(100), r.statistics().personal());
        assertEquals(BigInteger.valueOf(15), r.statistics().free());
        assertEquals(BigInteger.valueOf(75), r.statistics().dead());
        assertEquals(BigInteger.valueOf(115), r.statistics().worldTotal());
        assertEquals(2, r.deadLocations().size());
        assertEquals(1, r.statistics().registeredPlayers());
    }

    @Test void zeroBalanceRegistrySurvivesSuccessfulRescanOnlyForEligiblePlayers() {
        var original = UUID.randomUUID();
        var banned = UUID.randomUUID();
        var missing = UUID.randomUUID();
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("players"));
        scan.preservePreviouslyRegistered(original, false, true);
        scan.preservePreviouslyRegistered(banned, true, true);
        scan.preservePreviouslyRegistered(missing, false, false);
        scan.completeSource("players");
        var r = scan.commit();
        assertEquals(Map.of(original, BigInteger.ZERO), r.personalBalances());
        assertEquals(1, r.statistics().registeredPlayers());
    }

    @Test void invalidAndUndiscoveredInputsNeverBecomeZeroAssets() {
        var scan = new SettlementAccumulator(new WorldScope(true, Set.of(), Set.of()),
                Set.of("regions"));
        assertThrows(IllegalArgumentException.class,
            () -> scan.accept(asset("unknown", "key", "w", null, false, false, false, 0)));
        assertThrows(IllegalArgumentException.class,
            () -> scan.accept(asset("regions", "key", "w", null, false, false, false, -1)));
        assertThrows(IllegalArgumentException.class, () -> scan.completeSource("unknown"));
        assertThrows(IllegalStateException.class, scan::commit);
    }
}
