package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssetStatisticsTest {
    @Test void deadAssetsStayOutsideOrdinaryTotal() {
        var s = AssetStatistics.calculate(Map.of(), BigInteger.valueOf(100), BigInteger.valueOf(50));
        assertEquals(BigInteger.valueOf(100), s.worldTotal());
        assertEquals(BigInteger.valueOf(50), s.dead());
        assertNull(s.halfShareSubjects());
    }

    @Test void registeredPopulationControlsConcentrationAndSignedDeltas() {
        var a = UUID.fromString("00000000-0000-0000-0000-000000000011");
        var b = UUID.fromString("00000000-0000-0000-0000-000000000022");
        var s = AssetStatistics.calculate(Map.of(a, BigInteger.valueOf(40), b, BigInteger.ZERO),
                BigInteger.valueOf(60), BigInteger.valueOf(80));
        assertEquals(BigInteger.valueOf(100), s.worldTotal());
        assertEquals(0, s.sharedAverage().compareTo(new BigDecimal("50")));
        assertEquals(1, s.halfShareSubjects());
        assertEquals(0, s.minMinusShared().compareTo(new BigDecimal("-50")));
    }

    @Test void zeroTotalHasNoConcentration() {
        var a = UUID.fromString("00000000-0000-0000-0000-000000000033");
        var s = AssetStatistics.calculate(Map.of(a, BigInteger.ZERO),
                BigInteger.ZERO, BigInteger.TEN);
        assertEquals(BigInteger.ZERO, s.worldTotal());
        assertNull(s.maxWorldPercent());
        assertNull(s.halfShareSubjects());
    }

    @Test void negativeValuesAreRejectedBeforeReportPublication() {
        var a = UUID.fromString("00000000-0000-0000-0000-000000000044");
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                Map.of(a, BigInteger.valueOf(-1)), BigInteger.ZERO, BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                Map.of(), BigInteger.valueOf(-1), BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AssetStatistics.calculate(
                Map.of(), BigInteger.ZERO, BigInteger.valueOf(-1)));
    }
}
