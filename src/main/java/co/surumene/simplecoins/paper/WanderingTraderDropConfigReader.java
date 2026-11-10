package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.CoinDenomination;
import co.surumene.simplecoins.domain.TraderDropStackPlan;
import co.surumene.simplecoins.domain.WanderingTraderDropPolicy;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Strict, fail-closed Paper config adapter for trader currency-drop candidates.
 *
 * Keys/defaults are technical candidates until approved in SimpleCoins config
 * design. Nothing in this class registers a listener or issues currency.
 */
public final class WanderingTraderDropConfigReader {
    private static final String ROOT = "trader-drops";
    private static final Set<String> FIELDS = Set.of(
            "enabled", "probability", "min-count", "max-count",
            "chance", "minimum-count", "maximum-count",
            "max-stacks-per-event", "weights");

    private WanderingTraderDropConfigReader() {}

    public record Loaded(WanderingTraderDropPolicy policy, int maxStacksPerEvent) {
        public Loaded {
            Objects.requireNonNull(policy, "policy");
            if (maxStacksPerEvent < 1
                    || maxStacksPerEvent > TraderDropStackPlan.MAX_STACKS_PER_PREPARATION)
                throw new IllegalArgumentException("Invalid stack preparation budget");
        }
    }

    public static Loaded read(ConfigurationSection root) {
        Objects.requireNonNull(root, "root");
        if (!root.contains(ROOT))
            return new Loaded(new WanderingTraderDropPolicy(false, 0, 1, 1, Map.of()), 1);
        ConfigurationSection section = root.getConfigurationSection(ROOT);
        if (section == null)
            throw new IllegalArgumentException(ROOT + " must be a configuration section");
        for (String key : section.getKeys(false))
            if (!FIELDS.contains(key))
                throw new IllegalArgumentException("Unknown trader-drop setting: " + key);

        boolean enabled = booleanField(section.get("enabled"), "enabled");
        // Disabling drops does not require specifying an unused probability,
        // but any present field must still be valid so re-enabling fails early.
        // Deployed candidate configs used chance/minimum-count/maximum-count.
        // New aliases may coexist across fields, never for the same field.
        boolean legacy = section.contains("chance") || section.contains("minimum-count")
                || section.contains("maximum-count");
        double probability = decimalField(section,
                selectedKey(section, "probability", "chance"), enabled, 0);
        int min = integerField(section,
                selectedKey(section, "min-count", "minimum-count"), enabled, 1);
        int max = integerField(section,
                selectedKey(section, "max-count", "maximum-count"), enabled, 1);
        // Legacy configs predate the stack budget; default to one 64-item stack.
        int stackBudget = integerField(section, "max-stacks-per-event", enabled && !legacy, 1);
        Map<CoinDenomination, Integer> weights = parseWeights(section, enabled);

        WanderingTraderDropPolicy policy =
                new WanderingTraderDropPolicy(enabled, probability, min, max, weights);
        if (stackBudget < 1 || stackBudget > TraderDropStackPlan.MAX_STACKS_PER_PREPARATION)
            throw new IllegalArgumentException("Invalid trader-drop stack budget");
        if (enabled && 1L + (max - 1L) / 64L > stackBudget)
            throw new IllegalArgumentException("Maximum trader-drop count exceeds stack budget");
        return new Loaded(policy, stackBudget);
    }

    private static String selectedKey(ConfigurationSection section, String current, String legacy) {
        if (section.contains(current) && section.contains(legacy))
            throw new IllegalArgumentException("Conflicting trader-drop aliases: "
                    + current + " and " + legacy);
        return section.contains(legacy) ? legacy : current;
    }

    private static boolean booleanField(Object value, String key) {
        if (value instanceof Boolean bool) return bool;
        throw new IllegalArgumentException("Expected boolean trader-drops." + key);
    }

    private static double decimalField(ConfigurationSection section, String key,
                                       boolean required, double fallback) {
        Object value = section.get(key);
        if (value == null) {
            if (required) throw new IllegalArgumentException("Missing trader-drops." + key);
            return fallback;
        }
        if (!(value instanceof Number number))
            throw new IllegalArgumentException("Expected numeric trader-drops." + key);
        return number.doubleValue();
    }

    private static int integerField(ConfigurationSection section, String key,
                                    boolean required, int fallback) {
        Object value = section.get(key);
        if (value == null) {
            if (required) throw new IllegalArgumentException("Missing trader-drops." + key);
            return fallback;
        }
        if (!(value instanceof Number number)
                || number.doubleValue() != Math.rint(number.doubleValue())
                || number.longValue() < Integer.MIN_VALUE
                || number.longValue() > Integer.MAX_VALUE
                || !Double.isFinite(number.doubleValue()))
            throw new IllegalArgumentException("Expected integer trader-drops." + key);
        return number.intValue();
    }

    private static Map<CoinDenomination, Integer> parseWeights(
            ConfigurationSection section, boolean required) {
        if (!section.contains("weights")) {
            if (required) throw new IllegalArgumentException("Missing trader-drops.weights");
            return Map.of();
        }
        ConfigurationSection raw = section.getConfigurationSection("weights");
        if (raw == null) throw new IllegalArgumentException("Weights must be a section");
        EnumMap<CoinDenomination, Integer> selected = new EnumMap<>(CoinDenomination.class);
        for (String key : raw.getKeys(false)) {
            CoinDenomination denomination;
            try {
                denomination = CoinDenomination.fromValue(Integer.parseInt(key));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Unknown trader-drop denomination: " + key, invalid);
            }
            if (selected.containsKey(denomination))
                throw new IllegalArgumentException("Duplicate trader-drop denomination: " + key);
            int weight = integerField(raw, key, true, 0);
            if (weight < 0) throw new IllegalArgumentException("Negative trader-drop weight: " + key);
            selected.put(denomination, weight);
        }
        return Map.copyOf(selected);
    }
}
