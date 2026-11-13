package co.surumene.simplecoins;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * Technical acceptance probe for TASK-20261108-001: verifies that the future
 * Paper plugin can actually be discovered by its classpath descriptor.
 * This is a Candidate Red check, not a passed Paper integration or CAT Gate.
 */
class PaperBootstrapDescriptorCandidateTest {
    @Test
    void packagedDescriptorResolvesToAnActualJavaPluginEntrypoint() throws Exception {
        String descriptor;
        try (InputStream stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream, "Paper plugin descriptor is missing from the runtime classpath");
            descriptor = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> declarations = Arrays.stream(descriptor.split("\\R"))
                .map(String::trim)
                .filter(line -> line.matches("main\\s*:.*"))
                .toList();
        assertEquals(1, declarations.size(), "Paper must have exactly one main class");
        String entrypoint = declarations.getFirst().replaceFirst("^main\\s*:\\s*", "").trim();
        assertFalse(entrypoint.isBlank(), "Paper main must not be empty");

        Class<?> mainType = Class.forName(entrypoint, false, getClass().getClassLoader());
        assertTrue(JavaPlugin.class.isAssignableFrom(mainType),
                "Paper descriptor main must refer to a JavaPlugin subclass");
    }
}
