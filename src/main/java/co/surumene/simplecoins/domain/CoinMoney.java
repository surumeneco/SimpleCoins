package co.surumene.simplecoins.domain;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class CoinMoney {
    private CoinMoney() {}

    /** Greedy decomposition is minimum-coin for the agreed 1/5/10 decimal series. */
    public static Map<CoinDenomination, Long> minimumCoins(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        EnumMap<CoinDenomination, Long> result = new EnumMap<>(CoinDenomination.class);
        long remaining = amount;
        CoinDenomination[] denominations = CoinDenomination.values();
        for (int i = denominations.length - 1; i >= 0; i--) {
            CoinDenomination d = denominations[i];
            long count = remaining / d.value();
            if (count != 0) result.put(d, count);
            remaining %= d.value();
        }
        return Collections.unmodifiableMap(result);
    }

    /** Uses unbounded arithmetic; callers must explicitly handle inventory limits. */
    public static BigInteger totalValue(Map<CoinDenomination, ? extends Number> coins) {
        BigInteger total = BigInteger.ZERO;
        for (var e : coins.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) throw new IllegalArgumentException("Null coin");
            BigInteger count;
            try { count = new BigInteger(e.getValue().toString()); }
            catch (NumberFormatException invalid) { throw new IllegalArgumentException("Coin count must be an integer", invalid); }
            if (count.signum() < 0) throw new IllegalArgumentException("Negative coin count");
            total = total.add(BigInteger.valueOf(e.getKey().value()).multiply(count));
        }
        return total;
    }
}
