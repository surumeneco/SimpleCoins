package co.surumene.simplecoins.domain;

/** Image lookup only. Does not normalize displayed names or persistent IDs. */
public final class CoinImageNames {
    private CoinImageNames() {}

    public static String normalizeStem(String stem) {
        if (stem == null || stem.isBlank() || stem.contains("/") || stem.contains("\\") || stem.contains("..")) {
            throw new IllegalArgumentException("Invalid image stem");
        }
        int firstSeparator = -1;
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if (c == ' ' || c == '_' || c == '-') {
                firstSeparator = i;
                break;
            }
        }
        if (firstSeparator == -1) return stem;

        StringBuilder normalized = new StringBuilder(stem.length());
        normalized.append(stem, 0, firstSeparator);
        for (int i = firstSeparator; i < stem.length(); i++) {
            char c = stem.charAt(i);
            normalized.append(c == ' ' || c == '_' || c == '-' ? '_' : c);
        }
        return normalized.toString();
    }

    /** Ambiguous matches are an error rather than choosing a file arbitrarily. */
    public static String uniquePng(String configuredName, Iterable<String> candidateFileNames) {
        String desired = normalizeStem(configuredName);
        String first = null;
        int found = 0;
        for (String candidate : candidateFileNames) {
            if (candidate.endsWith(".png") && normalizeStem(candidate.substring(0, candidate.length()-4)).equals(desired)) {
                if (found == 0) first = candidate;
                found++;
            }
        }
        if (found != 1) throw new IllegalStateException("Expected one image but found " + found);
        return first;
    }
}
