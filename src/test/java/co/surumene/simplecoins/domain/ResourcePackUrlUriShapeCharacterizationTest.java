package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Existing-code characterization only; linked CAT artifact remains Candidate. */
class ResourcePackUrlUriShapeCharacterizationTest {
    @Test
    void percentEncodedDelimitersAndPathSegmentsRemainLiteral() throws Exception {
        for (String url : new String[]{
                "https://example.com/?sig=a%23b",
                "https://example.com/%2e%2e/pack.zip"
        }) {
            assertEquals(Optional.of(url), ResourcePackUrl.validate(url));
        }
    }

    @Test
    void queryOnlyPathAndExplicitHttpsPortRemainUnchanged() throws Exception {
        for (String url : new String[]{
                "https://example.com?x=1",
                "https://example.com:443/pack.zip"
        }) {
            assertEquals(Optional.of(url), ResourcePackUrl.validate(url));
        }
    }

    @Test
    void schemeRelativeAndOpaqueUrisAreRejected() {
        for (String url : new String[]{
                "//example.com/pack.zip",
                "https:example.com/pack.zip"
        }) {
            assertThrows(IOException.class, () -> ResourcePackUrl.validate(url), url);
        }
    }

    @Test
    void rawUnicodeHostnameIsRejectedByServerAuthorityParser() {
        assertThrows(IOException.class,
                () -> ResourcePackUrl.validate("https://例え.テスト/pack.zip"));
    }
}
