package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** TASK-20261101-006: Draft SC-EXCHANGE-01..04. */
class ExchangeRecipeTest {
    @Test void standardSetContainsExactlyTwentyUniqueImmutableRecipes() {
        List<ExchangeRecipe> recipes = ExchangeRecipe.standard();
        assertEquals(20, recipes.size());
        assertEquals(20, Set.copyOf(recipes).size(), "No duplicate recipe tuple");
        assertThrows(UnsupportedOperationException.class, () -> recipes.clear());
    }

    @Test void allStandardRecipesPreserveValueAndPhysicalStackBounds() {
        for (ExchangeRecipe recipe : ExchangeRecipe.standard()) {
            assertEquals((long) recipe.input().value() * recipe.inputCount(),
                    (long) recipe.output().value() * recipe.outputCount());
            assertTrue(recipe.inputCount() >= 1 && recipe.inputCount() <= 9);
            assertTrue(recipe.outputCount() >= 1 && recipe.outputCount() <= 64);
        }
    }

    @Test void eachDecimalScaleHasFiveDistinctExpectedExchanges() {
        for (int n : new int[]{1, 10, 100, 1000}) {
            CoinDenomination one = CoinDenomination.fromValue(n);
            CoinDenomination five = CoinDenomination.fromValue(5 * n);
            CoinDenomination ten = CoinDenomination.fromValue(10 * n);
            Set<ExchangeRecipe> expected = Set.of(
                    new ExchangeRecipe(one, 5, five, 1),
                    new ExchangeRecipe(five, 1, one, 5),
                    new ExchangeRecipe(five, 2, ten, 1),
                    new ExchangeRecipe(ten, 1, one, 10),
                    new ExchangeRecipe(ten, 2, five, 4));
            assertEquals(5, expected.size());
            assertTrue(ExchangeRecipe.standard().containsAll(expected));
        }
    }

    @Test void invalidRecipeCannotMintOrDiscardValue() {
        CoinDenomination one = CoinDenomination.ONE;
        CoinDenomination five = CoinDenomination.FIVE;
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(null, 1, one, 1));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(one, 1, null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(one, 0, one, 1));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(one, 10, one, 10));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(one, 1, one, 65));
        assertThrows(IllegalArgumentException.class, () -> new ExchangeRecipe(one, 1, five, 1));
    }
}
