package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Draft STATS-BOUNDARY vectors; source completeness is an external precondition. */
class AssetStatisticsConcentrationBoundaryTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    private static void decimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test void freeAssetsAreAConcentrationSubjectButNotARegisteredPlayer() {
        var s = AssetStatistics.calculate(Map.of(A, BigInteger.ONE, B, BigInteger.ONE),
                BigInteger.valueOf(8), BigInteger.valueOf(100));
        assertEquals(2, s.registeredPlayers());
        assertEquals(BigInteger.TEN, s.worldTotal());
        assertEquals(BigInteger.valueOf(100), s.dead());
        assertEquals(1, s.halfShareSubjects());
        decimal("50", s.halfSharePercent());
        decimal("10", s.maxWorldPercent());
    }

    @Test void exactHalfThresholdIncludesTheFirstSubject() {
        var s = AssetStatistics.calculate(Map.of(A, BigInteger.valueOf(50),
                B, BigInteger.valueOf(50)), BigInteger.ZERO, BigInteger.ZERO);
        assertEquals(1, s.halfShareSubjects());
        decimal("50", s.halfSharePercent());
        decimal("50", s.maxWorldPercent());
    }

    @Test void fractionalAveragesAndConcentrationUseEightDecimalHalfUp() {
        var s = AssetStatistics.calculate(Map.of(A, BigInteger.ZERO,
                B, BigInteger.ZERO, C, BigInteger.ZERO), BigInteger.ONE, BigInteger.ZERO);
        decimal("0.33333333", s.sharedAverage());
        decimal("0", s.personalAverage());
        decimal("33.33333333", s.halfSharePercent());
        decimal("0", s.maxWorldPercent());
        decimal("-0.33333333", s.maxMinusShared());
    }

    @Test void hugeValuesRemainExactAndDeadAssetsStaySeparate() {
        BigInteger huge = BigInteger.ONE.shiftLeft(128);
        var s = AssetStatistics.calculate(Map.of(A, huge), huge, huge.multiply(BigInteger.TEN));
        assertEquals(huge.shiftLeft(1), s.worldTotal());
        assertEquals(huge.multiply(BigInteger.TEN), s.dead());
        assertEquals(1, s.halfShareSubjects());
        decimal("50", s.maxWorldPercent());
        decimal("100", s.halfSharePercent());
    }

    @Test void absentRegisteredPopulationHasNoInventedAverages() {
        var s = AssetStatistics.calculate(Map.of(), BigInteger.valueOf(7), BigInteger.valueOf(9));
        assertEquals(BigInteger.valueOf(7), s.worldTotal());
        assertNull(s.sharedAverage());
        assertNull(s.personalAverage());
        assertNull(s.max());
        assertNull(s.min());
        assertNull(s.halfShareSubjects());
        assertNull(s.halfSharePercent());
    }

    @Test void malformedPopulationAndAmountsAreRejected() {
        assertThrows(NullPointerException.class, () -> AssetStatistics.calculate(
                null, BigInteger.ZERO, BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                Map.of(), null, BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                Map.of(), BigInteger.ZERO, null));
        Map<UUID, BigInteger> bad = new HashMap<>();
        bad.put(A, null);
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                bad, BigInteger.ZERO, BigInteger.ZERO));
    }
}
