package co.surumene.simplecoins.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Exact value-preserving conversion contracts for the twenty specified
 * shapeless exchanges. This is the recipe definition, not a ledger mutation.
 */
public record CoinExchangeFormula(String id, CoinDenomination input,
                                  int inputCount, CoinDenomination output, int outputCount) {
    public CoinExchangeFormula {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (id.isBlank() || inputCount < 1 || inputCount > 9
                || outputCount < 1 || outputCount > 64)
            throw new IllegalArgumentException("Invalid exchange inventory counts or key");
        if ((long) input.value() * inputCount != (long) output.value() * outputCount)
            throw new IllegalArgumentException("Exchange must preserve monetary value");
    }

    public long totalValue() {
        return (long) input.value() * inputCount;
    }

    public static List<CoinExchangeFormula> all() {
        ArrayList<CoinExchangeFormula> recipes = new ArrayList<>(20);
        for (int n : new int[]{1, 10, 100, 1000}) {
            add(recipes, n, 1, n, 5, 5*n, 1);
            add(recipes, n, 2, 5*n, 1, n, 5);
            add(recipes, n, 3, 5*n, 2, 10*n, 1);
            add(recipes, n, 4, 10*n, 1, n, 10);
            add(recipes, n, 5, 10*n, 2, 5*n, 4);
        }
        Set<String> unique = new HashSet<>();
        for (CoinExchangeFormula recipe : recipes)
            if (!unique.add(recipe.id())) throw new IllegalStateException("Duplicate exchange id");
        return List.copyOf(recipes);
    }

    private static void add(List<CoinExchangeFormula> recipes, int n, int index,
                            int input, int inputCount, int output, int outputCount) {
        recipes.add(new CoinExchangeFormula("n" + n + "_f" + index,
                CoinDenomination.fromValue(input), inputCount,
                CoinDenomination.fromValue(output), outputCount));
    }
}
