package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AssetDomainTest {
    @Test void noParticipantsKeepsConcentrationNotApplicable() {
        var s = AssetStatistics.calculate(Map.of(), BigInteger.valueOf(100), BigInteger.valueOf(50));
        assertEquals(BigInteger.valueOf(100), s.worldTotal());
        assertEquals(BigInteger.valueOf(50), s.dead());
        assertNull(s.halfShareSubjects());
        assertNull(s.halfSharePercent());
    }

    @Test void signedDeltasAreRelativeToBothAverages() {
        var u = UUID.randomUUID();
        var v = UUID.randomUUID();
        var s = AssetStatistics.calculate(Map.of(u, BigInteger.valueOf(40), v, BigInteger.ZERO),
                BigInteger.valueOf(60), BigInteger.ZERO);
        assertEquals(0, new BigDecimal("-50").compareTo(s.minMinusShared()));
        assertEquals(0, new BigDecimal("20").compareTo(s.maxMinusPersonal()));
        assertEquals(1, s.halfShareSubjects());
    }

    @Test void everyoneOverridesOwnerAndBanNeverBecomesFree() {
        UUID u = UUID.randomUUID();
        assertEquals(AssetOwner.Kind.FREE, AssetOwner.resolve(u, true, true, false).kind());
        assertEquals(AssetOwner.Kind.DEAD, AssetOwner.resolve(u, false, true, true).kind());
        assertEquals(AssetOwner.DeadReason.MISSING_PLAYERDATA, AssetOwner.resolve(u, false, false, false).reason());
        assertEquals(AssetOwner.Kind.PERSONAL, AssetOwner.resolve(u, false, false, true).kind());
        assertEquals(u, AssetOwner.mobOwner(u, UUID.randomUUID()));
    }
}
