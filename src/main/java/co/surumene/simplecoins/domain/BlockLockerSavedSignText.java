package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Raw display-text evidence from a saved sign, not an ownership decision.
 * BlockLocker 1.14.1 may prefer displayed text over saved PDC and accepts
 * text-only legacy signs. Localization, text JSON, sign attachment and
 * double-chest relationships require an authoritative later resolver.
 */
public final class BlockLockerSavedSignText {
    private BlockLockerSavedSignText() {}

    public record Evidence(List<String> front, Optional<List<String>> back,
                           boolean legacy) {
        public Evidence {
            front = List.copyOf(Objects.requireNonNull(front));
            back = Objects.requireNonNull(back).map(List::copyOf);
            if (front.size() != 4)
                throw new IllegalArgumentException("Saved sign front must have four lines");
            back.ifPresent(lines -> {
                if (lines.size() != 4)
                    throw new IllegalArgumentException("Saved sign back must have four lines");
            });
        }
    }

    public static Optional<Evidence> read(Map<String, Object> sign) throws IOException {
        if (sign == null)
            throw new IOException("Missing saved sign block entity");
        boolean modern = sign.containsKey("front_text") || sign.containsKey("back_text");
        boolean legacy = false;
        for (int i = 1; i <= 4; i++)
            legacy |= sign.containsKey("Text" + i);
        if (modern && legacy)
            throw new IOException("Ambiguous modern and legacy saved sign text");

        if (modern) {
            if (!sign.containsKey("front_text"))
                throw new IOException("Saved sign back_text without front_text");
            List<String> front = modernLines(sign.get("front_text"), "front_text");
            Optional<List<String>> back = sign.containsKey("back_text")
                    ? Optional.of(modernLines(sign.get("back_text"), "back_text"))
                    : Optional.empty();
            return Optional.of(new Evidence(front, back, false));
        }

        if (legacy) {
            List<String> front = new ArrayList<>(4);
            for (int i = 1; i <= 4; i++) {
                Object value = sign.get("Text" + i);
                if (!(value instanceof String line))
                    throw new IOException("Missing or malformed legacy saved sign Text" + i);
                front.add(line);
            }
            return Optional.of(new Evidence(front, Optional.empty(), true));
        }
        return Optional.empty();
    }

    private static List<String> modernLines(Object raw, String side) throws IOException {
        if (!(raw instanceof Map<?, ?> text))
            throw new IOException("Malformed saved sign " + side + " compound");
        for (Object key : text.keySet()) {
            if (!(key instanceof String))
                throw new IOException("Malformed saved sign " + side + " compound");
        }
        if (!(text.get("messages") instanceof List<?> messages) || messages.size() != 4)
            throw new IOException("Missing or invalid saved sign " + side + " messages");
        List<String> lines = new ArrayList<>(4);
        for (Object value : messages) {
            if (!(value instanceof String line))
                throw new IOException("Non-string saved sign " + side + " message");
            lines.add(line);
        }
        // Evidence makes the single immutable copy before exposing these lines.
        return lines;
    }
}
