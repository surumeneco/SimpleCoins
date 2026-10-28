package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TraderDropChanceTest {
    @Test void rejectsInvalidConfigurationRatherThanCreatingFreeMoney() {
        for (double chance : new double[]{Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, -0.1, 1.1}) {
            assertFalse(TraderDropChance.shouldDrop(chance, 0));
        }
    }

    @Test void evaluatesValidChanceBoundaries() {
        assertFalse(TraderDropChance.shouldDrop(0, 0));
        assertTrue(TraderDropChance.shouldDrop(1, 0.99999));
        assertTrue(TraderDropChance.shouldDrop(0.25, 0.249));
        assertFalse(TraderDropChance.shouldDrop(0.25, 0.25));
    }

    @Test void invalidRandomSamplerValuesAreNeverAccepted() {
        assertThrows(IllegalArgumentException.class, () ->
                TraderDropChance.shouldDrop(0.5, Double.NaN));
        assertThrows(IllegalArgumentException.class, () ->
                TraderDropChance.shouldDrop(0.5, 1.0));
    }
}
