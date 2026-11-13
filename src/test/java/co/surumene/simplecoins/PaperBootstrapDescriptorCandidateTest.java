package co.surumene.simplecoins;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Descriptor/bootstrap characterization; not a Paper runtime acceptance test. */
class PaperBootstrapDescriptorCandidateTest {
    @Test
    void descriptorNamesOneLoadableJavaPluginEntrypoint() throws Exception {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(resource, "SimpleCoins cannot load without plugin.yml");
            String yaml = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(1, yaml.lines().filter(line -> line.startsWith("main:")).count());
            assertTrue(yaml.contains("main: co.surumene.simplecoins.SimpleCoinsPlugin"));
            assertTrue(yaml.contains("api-version: '26.2'"));
        }
        assertTrue(JavaPlugin.class.isAssignableFrom(
                Class.forName("co.surumene.simplecoins.SimpleCoinsPlugin")));
    }
}
