package co.surumene.simplecoins.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Pure computation over COMPLETE classified asset snapshots, not a disk scanner. */
public final class AssetStatistics {
    private AssetStatistics() {}

    public record Snapshot(int registeredPlayers, BigInteger personal, BigInteger free,
                           BigInteger dead, BigInteger worldTotal, BigDecimal sharedAverage,
                           BigDecimal personalAverage, BigInteger max, BigInteger min,
                           BigDecimal maxWorldPercent, Integer halfShareSubjects,
                           BigDecimal halfSharePercent, BigDecimal maxMinusShared,
                           BigDecimal minMinusShared, BigDecimal maxMinusPersonal,
                           BigDecimal minMinusPersonal) {}

    public static Snapshot calculate(Map<UUID, BigInteger> registeredPlayers,
                                     BigInteger free, BigInteger dead) {
        Objects.requireNonNull(registeredPlayers);
        nonNegative(free);
        nonNegative(dead);
        registeredPlayers.values().forEach(AssetStatistics::nonNegative);
        BigInteger personal = registeredPlayers.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger world = personal.add(free);
        int n = registeredPlayers.size();
        BigInteger max = n == 0 ? null : registeredPlayers.values().stream().max(Comparator.naturalOrder()).orElseThrow();
        BigInteger min = n == 0 ? null : registeredPlayers.values().stream().min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal sharedAverage = n == 0 ? null : divide(world, BigInteger.valueOf(n));
        BigDecimal personalAverage = n == 0 ? null : divide(personal, BigInteger.valueOf(n));
        BigDecimal maxWorldPercent = n == 0 || world.signum() == 0 ? null :
                divide(max.multiply(BigInteger.valueOf(100)), world);
        Integer k = null;
        BigDecimal rate = null;
        // N=0 and T=0 make K and the percentage not applicable under 02A.
        if (n > 0 && world.signum() > 0) {
            List<BigInteger> subjects = new ArrayList<>(registeredPlayers.values());
            subjects.add(free);
            subjects.sort(Comparator.reverseOrder());
            BigInteger accumulated = BigInteger.ZERO;
            int count = 0;
            for (BigInteger subject : subjects) {
                accumulated = accumulated.add(subject);
                count++;
                if (accumulated.multiply(BigInteger.TWO).compareTo(world) >= 0) break;
            }
            k = count;
            rate = divide(BigInteger.valueOf(100L * k), BigInteger.valueOf(n));
        }
        return new Snapshot(n, personal, free, dead, world, sharedAverage, personalAverage,
                max, min, maxWorldPercent, k, rate,
                delta(max, sharedAverage), delta(min, sharedAverage),
                delta(max, personalAverage), delta(min, personalAverage));
    }

    private static BigDecimal delta(BigInteger value, BigDecimal average) {
        return value == null || average == null ? null : new BigDecimal(value).subtract(average);
    }
    private static void nonNegative(BigInteger v) {
        if (v == null || v.signum() < 0) throw new IllegalArgumentException("Negative or null asset");
    }
    private static BigDecimal divide(BigInteger a, BigInteger b) {
        return new BigDecimal(a).divide(new BigDecimal(b), 8, RoundingMode.HALF_UP);
    }
}
