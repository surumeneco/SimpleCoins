package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Text-only Everyone-profile detection following BlockLocker 1.14.1's
 * localized ProfileFactoryImpl contract. A match is not a protection or
 * ownership decision; the caller must verify sign attachment and all profiles.
 */
public final class BlockLockerEveryoneText {
    private BlockLockerEveryoneText() {}

    private static final Pattern LEGACY_COLOR =
            Pattern.compile("(?i)§[0-9A-FK-ORX]");

    public static boolean matches(String displayedProfile,
                                  Set<String> localizedTagNames) throws IOException {
        if (displayedProfile == null)
            throw new IOException("Missing decoded BlockLocker profile text");
        if (localizedTagNames == null || localizedTagNames.isEmpty())
            throw new IOException("Missing runtime Everyone tag aliases");
        for (String name : localizedTagNames) {
            if (name == null || name.isBlank() ||
                    name.contains("[") || name.contains("]"))
                throw new IOException("Malformed runtime Everyone tag alias");
        }
        String displayed = LEGACY_COLOR.matcher(displayedProfile).replaceAll("").trim();
        for (String name : localizedTagNames)
            if (displayed.equalsIgnoreCase("[" + name + "]"))
                return true;
        return false;
    }
}
