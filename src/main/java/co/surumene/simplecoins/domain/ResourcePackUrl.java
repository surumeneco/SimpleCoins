package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/** Validates an operator-configured public Java pack URL without fetching it. */
public final class ResourcePackUrl {
    private ResourcePackUrl() {}

    public static Optional<String> validate(String configured) throws IOException {
        if (configured == null || configured.isBlank()) return Optional.empty();
        String value = configured.trim();
        try {
            URI uri = new URI(value).parseServerAuthority();
            String scheme = uri.getScheme();
            if (scheme == null ||
                    !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) ||
                    uri.getHost() == null || uri.getHost().isBlank() ||
                    uri.getRawUserInfo() != null || uri.getRawFragment() != null ||
                    (uri.getPort() != -1 && (uri.getPort() < 1 || uri.getPort() > 65535)))
                throw new IOException("Invalid HTTP(S) resource-pack URL");
            return Optional.of(value);
        } catch (URISyntaxException invalid) {
            throw new IOException("Malformed HTTP(S) resource-pack URL", invalid);
        }
    }
}
