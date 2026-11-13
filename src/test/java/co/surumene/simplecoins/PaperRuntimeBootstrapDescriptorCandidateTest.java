package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * TASK-20261108-001 / CAT issue-work Red candidate.
 * Protects the Paper bootstrap contract; it is not a Paper server fixture.
 */
class PaperRuntimeBootstrapDescriptorCandidateTest {
    @Test void packagedPluginDescriptorHasExactlyOneLoadableJavaPluginEntrypoint() throws Exception {
        String yaml;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input, "Paper plugin.yml must be on the runtime classpath");
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> mains = yaml.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("main:"))
                .toList();
        assertEquals(1, mains.size(), "One unambiguous Paper JavaPlugin main is required");
        String main = mains.get(0).substring("main:".length()).trim();
        assertFalse(main.isBlank(), "A blank main is not an entrypoint");
        assertFalse(main.contains("#"), "A plugin entrypoint must not contain inline comment syntax");
        Class<?> entrypoint = Class.forName(main, false, getClass().getClassLoader());
        assertTrue(JavaPlugin.class.isAssignableFrom(entrypoint),
                () -> main + " must extend JavaPlugin");
    }
}
