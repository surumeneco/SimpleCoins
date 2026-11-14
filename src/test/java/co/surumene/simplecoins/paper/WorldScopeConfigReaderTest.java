package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class WorldScopeConfigReaderTest {
    @Test void defaultsIncludeAllWorldsAndIndependentAssets() {
        var loaded = WorldScopeConfigReader.read(new YamlConfiguration());
        assertTrue(loaded.scope().includes("world"));
        assertTrue(loaded.scope().includes("world_nether"));
        assertTrue(loaded.scope().includes(null));
        assertEquals(1_000_000, loaded.maxSourceEntries());
    }

    @Test void explicitOnOffOverlapRevertsToDefaultAndIndependentAlwaysIncluded() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("statistics.default-enabled", false);
        settings.set("statistics.enabled-worlds", List.of("on", "both"));
        settings.set("statistics.disabled-worlds", List.of("off", "both"));
        settings.set("settlement.max-source-entries", 20);
        var result = WorldScopeConfigReader.read(settings);
        assertFalse(result.scope().includes("other"));
        assertTrue(result.scope().includes("on"));
        assertFalse(result.scope().includes("off"));
        assertFalse(result.scope().includes("both"));
        assertTrue(result.scope().includes(null));
        assertEquals(20, result.maxSourceEntries());
        settings.set("statistics.default-enabled", true);
        assertFalse(result.scope().includes("other"),
                "Changing config after loading cannot mutate the selected generation");
    }

    @Test void malformedDefaultAndWorldListsAreNeverCoerced() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("statistics.default-enabled", "false");
        assertThrows(IllegalArgumentException.class,
                () -> WorldScopeConfigReader.read(settings));
        settings.set("statistics.default-enabled", false);
        settings.set("statistics.enabled-worlds", "world");
        assertThrows(IllegalArgumentException.class,
                () -> WorldScopeConfigReader.read(settings));
        settings.set("statistics.enabled-worlds", List.of("world", 42));
        assertThrows(IllegalArgumentException.class,
                () -> WorldScopeConfigReader.read(settings));
        settings.set("statistics.enabled-worlds", List.of(""));
        assertThrows(IllegalArgumentException.class,
                () -> WorldScopeConfigReader.read(settings));
        settings.set("statistics.enabled-worlds", List.of("valid"));
        settings.set("statistics.disabled-worlds", List.of(" worldly"));
        assertThrows(IllegalArgumentException.class,
                () -> WorldScopeConfigReader.read(settings));
    }

    @Test void discoveryCountBudgetMustBeExactPositiveBoundedInt() {
        YamlConfiguration settings = new YamlConfiguration();
        for (Object value : List.of("100", 0, -1, 2_000_001,
                1.5, new BigInteger("18446744073709551615"), new BigDecimal("3.01"))) {
            settings.set("settlement.max-source-entries", value);
            assertThrows(IllegalArgumentException.class,
                    () -> WorldScopeConfigReader.read(settings),
                    "Expected rejection of "+value);
        }
        settings.set("settlement.max-source-entries", 2_000_000);
        assertEquals(2_000_000, WorldScopeConfigReader.read(settings).maxSourceEntries());
    }
}
