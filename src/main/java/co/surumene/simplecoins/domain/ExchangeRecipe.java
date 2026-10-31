package co.surumene.simplecoins.domain;

import java.util.ArrayList;
import java.util.List;

/** Exactly the twenty agreed value-preserving recipes. */
public record ExchangeRecipe(CoinDenomination input, int inputCount,
                             CoinDenomination output, int outputCount) {
    public ExchangeRecipe {
        if (input == null || output == null || inputCount <= 0 || outputCount <= 0
                || inputCount > 9 || outputCount > 64
                || (long) input.value() * inputCount != (long) output.value() * outputCount) {
            throw new IllegalArgumentException("Invalid coin exchange");
        }
    }

    public static List<ExchangeRecipe> standard() {
        List<ExchangeRecipe> result = new ArrayList<>();
        for (int n : new int[]{1, 10, 100, 1000}) {
            result.add(new ExchangeRecipe(CoinDenomination.fromValue(n), 5, CoinDenomination.fromValue(5*n), 1));
            result.add(new ExchangeRecipe(CoinDenomination.fromValue(5*n), 1, CoinDenomination.fromValue(n), 5));
            result.add(new ExchangeRecipe(CoinDenomination.fromValue(5*n), 2, CoinDenomination.fromValue(10*n), 1));
            result.add(new ExchangeRecipe(CoinDenomination.fromValue(10*n), 1, CoinDenomination.fromValue(n), 10));
            result.add(new ExchangeRecipe(CoinDenomination.fromValue(10*n), 2, CoinDenomination.fromValue(5*n), 4));
        }
        return List.copyOf(result);
    }
}
