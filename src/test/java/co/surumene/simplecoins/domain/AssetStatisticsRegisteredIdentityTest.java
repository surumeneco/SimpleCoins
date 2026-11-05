package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** STATS-IDENTITY-01 is intentionally Red until CAT-gated production repair. */
class AssetStatisticsRegisteredIdentityTest {
    @Test void nullRegisteredUuidCannotBecomeAPersonalAssetOwner() {
        Map<UUID, BigInteger> malformed = new HashMap<>();
        malformed.put(null, BigInteger.valueOf(100));
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                malformed, BigInteger.ZERO, BigInteger.ZERO));
    }

    @Test void validRegisteredUuidStillProducesPersonalAssetStatistics() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        var result = AssetStatistics.calculate(Map.of(owner, BigInteger.valueOf(100)),
                BigInteger.ZERO, BigInteger.ZERO);
        assertEquals(1, result.registeredPlayers());
        assertEquals(BigInteger.valueOf(100), result.personal());
        assertEquals(BigInteger.valueOf(100), result.worldTotal());
    }
}
