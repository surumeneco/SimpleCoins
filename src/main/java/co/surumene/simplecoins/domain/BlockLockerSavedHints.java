package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only hints from BlockLocker 1.14.1 sign PDC. Never authoritative by
 * themselves: display text takes precedence on mismatch, legacy text-only
 * signs exist, and attachment/double-chest relationships must be checked.
 */
public final class BlockLockerSavedHints {
    private BlockLockerSavedHints() {}

    public record Hints(String header, Optional<UUID> firstProfileUuid,
                        boolean everyoneMarkerPresent) {
        public Hints {
            Objects.requireNonNull(header);
            Objects.requireNonNull(firstProfileUuid);
        }
    }

    public static Optional<Hints> read(Map<String, Object> pdc) throws IOException {
        if (pdc == null)
            throw new IOException("Missing saved sign PDC map");
        boolean hasHeader = pdc.containsKey("blocklocker:header");
        for (Object rawKey : pdc.keySet()) {
            if (!(rawKey instanceof String key))
                throw new IOException("Malformed saved sign PDC key");
            if (key.startsWith("blocklocker:profile_") &&
                    !key.matches("blocklocker:profile_[1-6]"))
                throw new IOException("Unsupported saved BlockLocker profile key: " + key);
            if (!hasHeader && key.startsWith("blocklocker:profile_"))
                throw new IOException("Orphaned saved BlockLocker profile: " + key);
        }
        if (!hasHeader) return Optional.empty();
        Object rawHeader = pdc.get("blocklocker:header");
        if (!(rawHeader instanceof String header) ||
                !(header.equals("PRIVATE") || header.equals("MORE_USERS")))
            throw new IOException("Invalid saved BlockLocker header");

        Optional<UUID> firstOwner = Optional.empty();
        boolean everyone = false;
        for (int i = 1; i <= 6; i++) {
            String key = "blocklocker:profile_" + i;
            if (!pdc.containsKey(key)) continue;
            if (!(pdc.get(key) instanceof Map<?, ?> profile) ||
                    profile.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed saved BlockLocker profile: " + key);
            boolean hasName = profile.containsKey("blocklocker:n");
            boolean hasUuid = profile.containsKey("blocklocker:u");
            boolean hasEveryone = profile.containsKey("blocklocker:e");
            if (hasName && hasEveryone)
                throw new IOException("Ambiguous saved BlockLocker profile: " + key);
            if (hasName) {
                if (!(profile.get("blocklocker:n") instanceof String))
                    throw new IOException("Malformed saved BlockLocker player name: " + key);
                if (hasUuid) {
                    if (!(profile.get("blocklocker:u") instanceof long[] parts) || parts.length != 2)
                        throw new IOException("Malformed saved BlockLocker player UUID: " + key);
                    if (header.equals("PRIVATE") && i == 1)
                        firstOwner = Optional.of(new UUID(parts[0], parts[1]));
                }
            } else if (hasUuid) {
                throw new IOException("Orphaned saved BlockLocker player UUID: " + key);
            }
            if (hasEveryone) {
                if (!(profile.get("blocklocker:e") instanceof Byte))
                    throw new IOException("Malformed saved BlockLocker Everyone marker: " + key);
                // v1.14.1 ProfileFactoryImpl uses Optional presence, not the
                // boolean value, to reconstruct an Everyone profile.
                everyone = true;
            }
        }
        return Optional.of(new Hints(header, firstOwner, everyone));
    }
}
