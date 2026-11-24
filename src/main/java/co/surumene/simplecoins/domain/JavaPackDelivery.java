package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** The Java pack request carries the exact hash of the locally generated archive. */
public final class JavaPackDelivery {
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes(
            "simplecoins:java-currency-pack".getBytes(StandardCharsets.UTF_8));
    private final String url;
    private final byte[] sha1;

    private JavaPackDelivery(String url, byte[] sha1) {
        this.url = url;
        this.sha1 = sha1.clone();
    }

    public static boolean shouldRequest(boolean nexoDelegated, boolean javaClient, boolean prepared) {
        return !nexoDelegated && javaClient && prepared;
    }

    public static Optional<JavaPackDelivery> prepare(String configuredUrl, Path archive) throws IOException {
        var url = ResourcePackUrl.validate(configuredUrl);
        if (url.isEmpty()) return Optional.empty();
        return Optional.of(new JavaPackDelivery(url.orElseThrow(), ResourcePackHash.sha1(archive)));
    }

    public byte[] sha1() { return sha1.clone(); }

    @FunctionalInterface
    public interface Request {
        void send(UUID id, String url, byte[] hash);
    }

    public void request(boolean nexoDelegated, boolean javaClient, Request target) {
        Objects.requireNonNull(target);
        if (shouldRequest(nexoDelegated, javaClient, true))
            target.send(PACK_ID, url, sha1());
    }
}
