package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.CoinDenomination;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Random;

class WanderingTraderDropConfigReaderTest {
    private static YamlConfiguration valid() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("trader-drops.enabled", true);
        config.set("trader-drops.probability", 1.0);
        config.set("trader-drops.min-count", 65);
        config.set("trader-drops.max-count", 65);
        config.set("trader-drops.max-stacks-per-event", 2);
        config.set("trader-drops.weights.1", 1);
        config.set("trader-drops.weights.5", 0);
        return config;
    }

    private static void assertRejected(YamlConfiguration input) {
        assertThrows(IllegalArgumentException.class,
                () -> WanderingTraderDropConfigReader.read(input));
    }

    @Test void absentSectionOrExplicitDisableNeverDraws() {
        assertTrue(WanderingTraderDropConfigReader.read(new YamlConfiguration())
                .policy().draw(new Random(1)).isEmpty());
        YamlConfiguration disabled = new YamlConfiguration();
        disabled.set("trader-drops.enabled", false);
        assertTrue(WanderingTraderDropConfigReader.read(disabled).policy()
                .draw(new Random(1)).isEmpty());
    }

    @Test void validConfigDrawsExactValueAndBindsBudget() {
        YamlConfiguration root = valid();
        var loaded = WanderingTraderDropConfigReader.read(root);
        var result = loaded.policy().draw(new Random(100)).orElseThrow();
        assertEquals(CoinDenomination.ONE, result.denomination());
        assertEquals(65, result.count());
        assertEquals(2, loaded.maxStacksPerEvent());
        root.set("trader-drops.weights.1", 0);
        assertEquals(CoinDenomination.ONE,
                loaded.policy().draw(new Random(100)).orElseThrow().denomination());
    }

    @Test void malformedConfigRejectedRatherThanCoerced() {
        YamlConfiguration root = valid();
        root.set("trader-drops.probability", "1.0");
        assertRejected(root);
        root = valid();
        root.set("trader-drops.enabled", "true");
        assertRejected(root);
        root = valid();
        root.set("trader-drops.unknown-key", 42);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.weights.123", 10);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.weights.01", 1);
        assertRejected(root);
    }

    @Test void acceptsLegacySettingsButNeverConflictingAliases() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("trader-drops.enabled", true);
        legacy.set("trader-drops.chance", 1.0);
        legacy.set("trader-drops.minimum-count", 1);
        legacy.set("trader-drops.maximum-count", 1);
        legacy.set("trader-drops.weights.100", 4);
        var loaded = WanderingTraderDropConfigReader.read(legacy);
        var decision = loaded.policy().draw(new Random(5)).orElseThrow();
        assertEquals(CoinDenomination.HUNDRED, decision.denomination());
        assertEquals(1, decision.count());
        assertEquals(1, loaded.maxStacksPerEvent());

        legacy.set("trader-drops.probability", 0.5);
        assertRejected(legacy);
        legacy.set("trader-drops.probability", null);
        legacy.set("trader-drops.min-count", 1);
        assertRejected(legacy);
        legacy.set("trader-drops.min-count", null);
        legacy.set("trader-drops.max-count", 1);
        assertRejected(legacy);
    }

    @Test void enabledConfigRequiresAllFieldsAndPreparedCapacity() {
        YamlConfiguration root = valid();
        root.set("trader-drops.max-stacks-per-event", 1);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.probability", null);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.max-count", 65537);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.weights.1", -1);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.weights", "1: 1");
        assertRejected(root);
        root = valid();
        root.set("trader-drops.max-count", 65.5);
        assertRejected(root);
        root = valid();
        root.set("trader-drops.max-count", new java.math.BigInteger("18446744073709551616"));
        assertRejected(root);
        root = valid();
        root.set("trader-drops.max-count", new java.math.BigDecimal("2147483648"));
        assertRejected(root);
        root = valid();
        root.set("trader-drops.max-stacks-per-event", new java.math.BigInteger("18446744073709551617"));
        assertRejected(root);
    }
}
