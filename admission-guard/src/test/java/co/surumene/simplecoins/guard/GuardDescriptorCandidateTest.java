package co.surumene.simplecoins.guard;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** Metadata check; actual load order still requires a real Paper fixture. */
class GuardDescriptorCandidateTest {
    @Test void guardStartsBeforePostWorldAccountingAndShipsAPublicService() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream);
            String yaml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("name: SimpleCoinsGuard"));
            assertTrue(yaml.contains("load: STARTUP"));
        }
        assertNotNull(Class.forName("co.surumene.simplecoins.guard.api.AdmissionPermit"));
    }
}
