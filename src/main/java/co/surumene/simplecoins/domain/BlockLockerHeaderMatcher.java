package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        // Clean each localized alias only once: collision detection is otherwise
        // quadratic in both comparisons and regex-based normalization.
        List<String> privateNames = cleanedAliases(privateAliases, "PRIVATE");
        List<String> moreNames = cleanedAliases(moreUsersAliases, "MORE_USERS");
        for (String first : privateNames)
            for (String second : moreNames)
                if (first.equalsIgnoreCase(second))
                    throw new IOException("Ambiguous BlockLocker localized sign headers");

        String header = clean(displayedHeader);
        for (String alias : privateNames)
            if (header.equalsIgnoreCase(alias))
                return Optional.of(Type.PRIVATE);
        for (String alias : moreNames)
            if (header.equalsIgnoreCase(alias))
                return Optional.of(Type.MORE_USERS);
        return Optional.empty();
    }

    private static List<String> cleanedAliases(Set<String> aliases, String kind) throws IOException {
        if (aliases == null || aliases.isEmpty())
            throw new IOException("Missing BlockLocker " + kind + " header aliases");
        List<String> cleaned = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            if (alias == null)
                throw new IOException("Malformed BlockLocker " + kind + " header alias");
            String normalized = clean(alias);
            if (normalized.isBlank())
                throw new IOException("Malformed BlockLocker " + kind + " header alias");
            cleaned.add(normalized);
        }
        return cleaned;
    }

    /** Shared normalization for headers and first visible profile lookup keys. */
    static String clean(String value) {
        return LEGACY_COLOR.matcher(value).replaceAll("").trim();
    }
}
