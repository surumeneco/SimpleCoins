package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Code-to-spec characterization of existing Java resource-pack URL validation.
 * The linked CAT artifact is Candidate, not normative approval.
 */
class ResourcePackUrlNetworkAuthorityCharacterizationTest {
    @Test
    void ipv6AuthorityRetainsSignedQueryAndOriginalSpelling() throws Exception {
        String url = "https://[2001:db8::1]/pack.zip?sig=a%2Bb&expiry=42";
        assertEquals(Optional.of(url), ResourcePackUrl.validate(url));
    }

    @Test
    void uppercaseHttpsAndPercentEncodedQueryArePreserved() throws Exception {
        String url = "HTTPS://cdn.example.com/a%20b.zip?token=abc%2Bdef&v=1";
        assertEquals(Optional.of(url), ResourcePackUrl.validate(url));
    }

    @Test
    void explicitPortBoundsAreInclusiveButZeroAndOverflowAreRejected() throws Exception {
        assertEquals(Optional.of("https://example.com:1/p.zip"),
                ResourcePackUrl.validate("https://example.com:1/p.zip"));
        assertEquals(Optional.of("https://[2001:db8::1]:65535/p.zip"),
                ResourcePackUrl.validate("https://[2001:db8::1]:65535/p.zip"));
        for (String url : new String[]{
                "https://example.com:0/p.zip",
                "https://[2001:db8::1]:65536/p.zip"
        }) {
            assertThrows(IOException.class, () -> ResourcePackUrl.validate(url), url);
        }
    }

    @Test
    void userInfoIsRejectedForIpv6AndPercentEncodedCredentials() {
        for (String url : new String[]{
                "https://user@[2001:db8::1]/pack.zip",
                "https://u%73er:pw@example.com/pack.zip"
        }) {
            assertThrows(IOException.class, () -> ResourcePackUrl.validate(url), url);
        }
    }

    @Test
    void evenEmptyFragmentsAreRejected() {
        for (String url : new String[]{
                "https://example.com/pack.zip#",
                "https://example.com/pack.zip#fragment"
        }) {
            assertThrows(IOException.class, () -> ResourcePackUrl.validate(url), url);
        }
    }

    @Test
    void surroundingConfigurationWhitespaceIsTrimmed() throws Exception {
        assertEquals(Optional.of("https://example.com/p.zip"),
                ResourcePackUrl.validate(" \thttps://example.com/p.zip\n"));
    }
}
