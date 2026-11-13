package co.surumene.simplecoins.domain;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** Exact 20 user-specified conversion contracts; no Bukkit runtime needed. */
class CoinExchangeFormulaTest {
    @Test void all20RulesPreserveValueAndHaveUniqueNamesAndValidBounds() {
        var all = CoinExchangeFormula.all();
        assertEquals(20, all.size());
        assertEquals(20, new HashSet<>(all.stream().map(CoinExchangeFormula::id).toList()).size());
        assertEquals(Set.of(1L, 10L, 100L, 1000L),
                all.stream().map(f -> (long) f.input().value()).filter(v -> v == 1 || v == 10 || v == 100 || v == 1000).collect(Collectors.toSet()));
        for (CoinExchangeFormula f : all) {
            assertEquals((long) f.input().value() * f.inputCount(), f.totalValue());
            assertEquals((long) f.output().value() * f.outputCount(), f.totalValue());
            assertTrue(f.inputCount() >= 1 && f.inputCount() <= 9);
            assertTrue(f.outputCount() >= 1 && f.outputCount() <= 64);
            assertTrue(f.id().matches("n(1|10|100|1000)_f[1-5]"));
        }
        for (int n : new int[]{1, 10, 100, 1000}) {
            final int scale = n;
            assertEquals(5, all.stream().filter(f -> f.id().startsWith("n" + scale + "_")).count());
        }
        assertThrows(UnsupportedOperationException.class, () -> all.add(all.get(0)));
    }

    @Test void constructionRejectsValueCreationAndInvalidCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new CoinExchangeFormula("bad", CoinDenomination.ONE, 2, CoinDenomination.FIVE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new CoinExchangeFormula("bad", CoinDenomination.ONE, 0, CoinDenomination.ONE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new CoinExchangeFormula("bad", CoinDenomination.ONE, 10, CoinDenomination.ONE, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new CoinExchangeFormula("bad", CoinDenomination.ONE, 1, CoinDenomination.ONE, 65));
        assertThrows(NullPointerException.class,
                () -> new CoinExchangeFormula("bad", null, 1, CoinDenomination.ONE, 1));
    }
}
