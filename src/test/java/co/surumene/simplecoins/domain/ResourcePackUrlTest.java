package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResourcePackUrlTest {
    @Test void rejectsMalformedOrMisleadingEndpoints() {
        for (String url : new String[]{
                "https://", "http:///pack.zip", "ftp://example.com/pack.zip",
                "https://user:pass@example.com/pack.zip",
                "https://example.com/pack.zip#fragment",
                "https://example.com:99999/pack.zip",
                "https://example.com/a b.zip"}) {
            assertThrows(IOException.class, () -> ResourcePackUrl.validate(url), url);
        }
    }

    @Test void validHttpAndSignedHttpsUrlsArePreserved() throws Exception {
        String signed = "https://cdn.example.com/pack.zip?token=abc&expires=123";
        assertEquals(Optional.of(signed), ResourcePackUrl.validate(signed));
        assertEquals(Optional.of("http://example.com/pack.zip"),
                ResourcePackUrl.validate("http://example.com/pack.zip"));
    }

    @Test void blankOrMissingConfigurationMeansNoDelivery() throws Exception {
        assertEquals(Optional.empty(), ResourcePackUrl.validate(null));
        assertEquals(Optional.empty(), ResourcePackUrl.validate("  "));
        assertEquals(Optional.empty(), ResourcePackUrl.validate(""));
    }
}
