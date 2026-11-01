package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Header-only match using the runtime's localized BlockLocker 1.14.1 aliases.
 * Input is already-decoded visible sign text, not raw JSON or saved PDC.
 * A matching header is not proof of attachment or economic ownership.
 */
public final class BlockLockerHeaderMatcher {
    private BlockLockerHeaderMatcher() {}

    public enum Type { PRIVATE, MORE_USERS }

    private static final Pattern LEGACY_COLOR =
            Pattern.compile("(?i)§[0-9A-FK-ORX]");

    public static Optional<Type> match(String displayedHeader,
                                       Set<String> privateAliases,
                                       Set<String> moreUsersAliases) throws IOException {
        if (displayedHeader == null)
            throw new IOException("Missing decoded sign header");
        validateAliases(privateAliases, "PRIVATE");
        validateAliases(moreUsersAliases, "MORE_USERS");
        for (String first : privateAliases)
            for (String second : moreUsersAliases)
                if (clean(first).equalsIgnoreCase(clean(second)))
                    throw new IOException("Ambiguous BlockLocker localized sign headers");

        String header = clean(displayedHeader);
        for (String alias : privateAliases)
            if (header.equalsIgnoreCase(clean(alias)))
                return Optional.of(Type.PRIVATE);
        for (String alias : moreUsersAliases)
            if (header.equalsIgnoreCase(clean(alias)))
                return Optional.of(Type.MORE_USERS);
        return Optional.empty();
    }

    private static void validateAliases(Set<String> aliases, String kind) throws IOException {
        if (aliases == null || aliases.isEmpty())
            throw new IOException("Missing BlockLocker " + kind + " header aliases");
        for (String alias : aliases)
            if (alias == null || clean(alias).isBlank())
                throw new IOException("Malformed BlockLocker " + kind + " header alias");
    }

    /** Shared normalization for headers and first visible profile lookup keys. */
    static String clean(String value) {
        return LEGACY_COLOR.matcher(value).replaceAll("").trim();
    }
}
