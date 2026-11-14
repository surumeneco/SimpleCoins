package co.surumene.simplecoins.paper;

import co.surumene.simplecoins.domain.WorldScope;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Strict world-scope source selection for an observed saved-source generation.
 * Reading the config does not establish monetary settlement or authorize login.
 */
public final class WorldScopeConfigReader {
    private static final int DEFAULT_MAX_SOURCE_ENTRIES = 1_000_000;
    private static final int MAX_SOURCE_ENTRIES_LIMIT = 2_000_000;

    private WorldScopeConfigReader() {}

    public record Loaded(WorldScope scope, int maxSourceEntries) {
        public Loaded {
            Objects.requireNonNull(scope, "scope");
            if (maxSourceEntries < 1 || maxSourceEntries > MAX_SOURCE_ENTRIES_LIMIT)
                throw new IllegalArgumentException("Invalid bounded source visit budget");
        }
    }

    public static Loaded read(ConfigurationSection config) {
        Objects.requireNonNull(config, "config");
        Object rawDefault = config.get("statistics.default-enabled", Boolean.TRUE);
        if (!(rawDefault instanceof Boolean defaultIncluded))
            throw new IllegalArgumentException("Expected boolean statistics.default-enabled");
        Set<String> included = names(config.get("statistics.enabled-worlds", List.of()),
                "statistics.enabled-worlds");
        Set<String> excluded = names(config.get("statistics.disabled-worlds", List.of()),
                "statistics.disabled-worlds");
        Object rawBudget = config.get("settlement.max-source-entries", DEFAULT_MAX_SOURCE_ENTRIES);
        if (!(rawBudget instanceof Number numeric))
            throw new IllegalArgumentException("Expected integer settlement.max-source-entries");
        int budget;
        try {
            budget = new BigDecimal(numeric.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("Nonexact settlement.max-source-entries", invalid);
        }
        return new Loaded(new WorldScope(defaultIncluded, included, excluded), budget);
    }

    private static Set<String> names(Object raw, String field) {
        if (!(raw instanceof List<?> list))
            throw new IllegalArgumentException("Expected string array " + field);
        Set<String> result = new HashSet<>();
        for (Object value : list) {
            if (!(value instanceof String name) || name.isBlank() || !name.equals(name.trim()))
                throw new IllegalArgumentException("Invalid world name in " + field + ": " + value);
            result.add(name);
        }
        return Set.copyOf(result);
    }
}
