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

    @Test void defaultResourceIncludesAllNineDenominationsAndHalvingWeights() throws Exception {
        YamlConfiguration defaults;
        try (var stream = WanderingTraderDropConfigReaderTest.class
                .getResourceAsStream("/config.yml")) {
            assertNotNull(stream, "Missing plugin default config.yml");
            try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                defaults = YamlConfiguration.loadConfiguration(reader);
            }
        }
        assertTrue(defaults.getBoolean("trader-drops.enabled"));
        assertEquals(0.5, defaults.getDouble("trader-drops.chance"));
        assertEquals(1, defaults.getInt("trader-drops.minimum-count"));
        assertEquals(1, defaults.getInt("trader-drops.maximum-count"));
        var weights = defaults.getConfigurationSection("trader-drops.weights");
        assertNotNull(weights);
        var denominations = CoinDenomination.values();
        int[] expected = {256, 128, 64, 32, 16, 8, 4, 2, 1};
        assertEquals(9, weights.getKeys(false).size());
        int sum = 0;
        for (int i = 0; i < denominations.length; i++) {
            int faceValue = denominations[i].value();
            int weight = weights.getInt(String.valueOf(faceValue), -1);
            assertEquals(expected[i], weight, "Wrong weight for coin " + faceValue);
            sum += weight;
        }
        assertEquals(511, sum);
        assertEquals("C", defaults.getString("currency.unit"));
        // Verify the shipped YAML loads through the production adapter, and
        // that every denomination can actually be selected after a drop.
        var loaded = WanderingTraderDropConfigReader.read(defaults);
        assertEquals(1, loaded.maxStacksPerEvent());
        var deathRng = new Random(20261111L);
        int drops = 0;
        for (int i = 0; i < 10_000; i++) {
            if (loaded.policy().draw(deathRng).isPresent()) drops++;
        }
        assertTrue(drops > 4_500 && drops < 5_500,
                "The default chance must be 50%, observed " + drops + " / 10000");
        defaults.set("trader-drops.chance", 1.0);
        var forced = WanderingTraderDropConfigReader.read(defaults);
        var observed = java.util.EnumSet.noneOf(CoinDenomination.class);
        var rng = new Random(20261110L);
        for (int i = 0; i < 50_000; i++) {
            var result = forced.policy().draw(rng).orElseThrow();
            assertEquals(1, result.count());
            observed.add(result.denomination());
        }
        assertEquals(java.util.EnumSet.allOf(CoinDenomination.class), observed);
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
