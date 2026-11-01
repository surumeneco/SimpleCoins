package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** TASK-20261101-005: reject ambiguous world identities before staging. */
class SettlementWorldIdentityTest {
    private static SettlementAccumulator.Asset asset(String world) {
        return new SettlementAccumulator.Asset("source", "physical-slot", world,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                false, false, true, BigInteger.TEN, "storage/physical-slot");
    }

    @Test void emptyWorldIdentityCannotBecomeASettlementAsset() {
        assertThrows(IllegalArgumentException.class, () -> asset(""));
    }

    @Test void whitespaceWorldIdentityCannotBecomeASettlementAsset() {
        assertThrows(IllegalArgumentException.class, () -> asset("   "));
    }

    @Test void nullWorldIdentityRemainsValidForWorldIndependentPlayerdata() {
        var pending = new SettlementAccumulator(
                new WorldScope(false, Set.of(), Set.of()), Set.of("source"));
        pending.accept(asset(null));
        pending.completeSource("source");
        assertEquals(BigInteger.TEN, pending.commit().statistics().worldTotal());
    }

    @Test void explicitWorldIdentityStillHonorsWorldScope() {
        var pending = new SettlementAccumulator(
                new WorldScope(false, Set.of("included"), Set.of()), Set.of("source"));
        pending.accept(asset("excluded"));
        pending.completeSource("source");
        assertEquals(BigInteger.ZERO, pending.commit().statistics().worldTotal());
    }
}
