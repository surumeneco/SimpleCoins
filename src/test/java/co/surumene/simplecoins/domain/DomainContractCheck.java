package co.surumene.simplecoins.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Executable, dependency-free domain regression suite (java -ea). */
public final class DomainContractCheck {
    private DomainContractCheck() {}

    public static void main(String[] args) {
        assert CoinDenomination.values().length == 9;
        assert CoinMoney.minimumCoins(5200).equals(Map.of(CoinDenomination.FIVE_THOUSAND, 1L, CoinDenomination.HUNDRED, 2L));
        assert CoinMoney.minimumCoins(1).equals(Map.of(CoinDenomination.ONE, 1L));
        assert CoinMoney.minimumCoins(10001).equals(Map.of(CoinDenomination.TEN_THOUSAND, 1L, CoinDenomination.ONE, 1L));
        expect(IllegalArgumentException.class, () -> CoinMoney.minimumCoins(0));

        List<ExchangeRecipe> recipes = ExchangeRecipe.standard();
        assert recipes.size() == 20;
        assert Set.copyOf(recipes).size() == 20;
        for (ExchangeRecipe r : recipes) assert (long)r.input().value()*r.inputCount() == (long)r.output().value()*r.outputCount();

        var wallet = new WalletBalance(650000);
        assert wallet.withdraw(20000).amount() == 630000;
        expect(IllegalStateException.class, () -> wallet.deposit(1, 640000));
        assert new WalletBalance(639999).deposit(1, 640000).amount() == 640000;
        expect(IllegalStateException.class, () -> new WalletBalance(1).withdraw(2));
        expect(ArithmeticException.class, () -> new WalletBalance(Long.MAX_VALUE).deposit(1, Long.MAX_VALUE));

        var scope = new WorldScope(false, Set.of("on", "both"), Set.of("off", "both"));
        assert scope.includes("on") && !scope.includes("off") && !scope.includes("both") && !scope.includes("other");
        assert scope.includes(null);
        var onScope = new WorldScope(true, Set.of("both"), Set.of("both"));
        assert onScope.includes("both");

        assert CoinImageNames.uniquePng("100 Coin", List.of("100-Coin.png")).equals("100-Coin.png");
        expect(IllegalStateException.class, () -> CoinImageNames.uniquePng("100 Coin", List.of("100_Coin.png", "100 Coin.png")));
        expect(IllegalArgumentException.class, () -> CoinImageNames.uniquePng("../escape", List.of("escape.png")));

        var a = UUID.randomUUID(); var b = UUID.randomUUID();
        var assets = AssetStatistics.calculate(Map.of(a, BigInteger.valueOf(40), b, BigInteger.ZERO), BigInteger.valueOf(60), BigInteger.valueOf(80));
        assert assets.worldTotal().equals(BigInteger.valueOf(100));
        assert assets.dead().equals(BigInteger.valueOf(80));
        assert assets.registeredPlayers() == 2;
        assert assets.min().equals(BigInteger.ZERO);
        assert assets.halfShareSubjects() == 1; // free alone has 60%
        assert assets.halfSharePercent().compareTo(new BigDecimal("50")) == 0;
        var empty = AssetStatistics.calculate(Map.of(), BigInteger.ZERO, BigInteger.valueOf(500));
        assert empty.registeredPlayers() == 0 && empty.sharedAverage() == null && empty.halfShareSubjects() == null;

        var hugeCount = BigInteger.ONE.shiftLeft(80);
        Map<CoinDenomination, BigInteger> huge = new HashMap<>();
        huge.put(CoinDenomination.TEN_THOUSAND, hugeCount);
        assert CoinMoney.totalValue(huge).equals(hugeCount.multiply(BigInteger.valueOf(10000))); // overflow regression

        System.out.println("Domain contracts passed");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception " + error, error);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
