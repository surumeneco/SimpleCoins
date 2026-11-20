package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One SAVED sign's effective profiles, applying BlockLocker v1.15's
 * SignParserImpl / ProfileFactoryImpl display-text-over-PDC rule.
 * This is not proof of attachment, all related signs, or a common source epoch.
 */
public final class BlockLockerSavedProfileResolution {
    private BlockLockerSavedProfileResolution() {}

    @FunctionalInterface
    public interface DisplayDecoder {
        String decode(String savedLine) throws IOException;
    }

    public record SignProfiles(BlockLockerHeaderMatcher.Type type,
                               Optional<UUID> primaryOwner,
                               boolean everyoneEffective,
                               boolean everyoneAbsenceVerified) {
        public SignProfiles {
            Objects.requireNonNull(type);
            Objects.requireNonNull(primaryOwner);
            if (everyoneEffective == everyoneAbsenceVerified)
                throw new IllegalArgumentException("Everyone and absence cannot both hold");
        }
    }

    public static Optional<SignProfiles> read(
            Map<String,Object> block,
            Set<String> privateHeaders,
            Set<String> moreHeaders,
            Set<String> everyoneAliases,
            DisplayDecoder decoder) throws IOException {
        if (block == null || decoder == null)
            throw new IOException("Missing saved BlockLocker sign or display decoder");

        var sign = BlockLockerSavedSignText.read(block)
                .orElseThrow(() -> new IOException("Saved sign display evidence is missing"));
        // Old Text1..4 tags are already visible strings, unlike component JSON.
        List<String> front = sign.legacy() ? sign.front() : decode(sign.front(), decoder);
        if (!sign.legacy() && sign.back().isEmpty())
            throw new IOException("Saved modern sign back face is unavailable");
        List<String> back = sign.back().isPresent()
                ? decode(sign.back().orElseThrow(), decoder) : List.of();

        Optional<BlockLockerHeaderMatcher.Type> frontType =
                BlockLockerHeaderMatcher.match(front.get(0), privateHeaders, moreHeaders);
        Optional<BlockLockerHeaderMatcher.Type> backType = back.isEmpty()
                ? Optional.empty()
                : BlockLockerHeaderMatcher.match(back.get(0), privateHeaders, moreHeaders);

        Map<?, ?> pdc = Map.of();
        if (block.containsKey("PublicBukkitValues")) {
            if (!(block.get("PublicBukkitValues") instanceof Map<?, ?> savedPdc)
                    || savedPdc.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Malformed saved BlockLocker PDC");
            pdc = savedPdc;
            @SuppressWarnings("unchecked")
            var hints = BlockLockerSavedHints.read((Map<String,Object>) savedPdc);
            if (hints.isPresent()) {
                BlockLockerHeaderMatcher.Type savedType =
                        BlockLockerHeaderMatcher.Type.valueOf(hints.orElseThrow().header());
                if (frontType.isEmpty() || frontType.orElseThrow() != savedType)
                    throw new IOException("Saved BlockLocker header disagrees with visible sign");
            }
        }
        if (frontType.isEmpty())
            return Optional.empty();

        boolean backActive = backType.isPresent();
        UUID firstOwner = null;
        boolean everyone = false;
        for (int slot = 0; slot < (backActive ? 6 : 3); slot++) {
            String visible = slot < 3 ? front.get(slot + 1) : back.get(slot - 2);
            Object rawSaved = pdc.get("blocklocker:profile_" + (slot + 1));
            Map<?, ?> savedProfile = rawSaved instanceof Map<?, ?> profile ? profile : Map.of();
            Object savedName = savedProfile.get("blocklocker:n");

            // The v1.15 parser uses a saved PlayerProfile (including its UUID)
            // only when the displayed name exactly matches its saved name.
            // It parses modified display text instead, even if PDC still has
            // an old UUID or an old Everyone marker.
            if (savedName instanceof String name && name.equals(visible)) {
                if (slot == 0 && frontType.orElseThrow() == BlockLockerHeaderMatcher.Type.PRIVATE
                        && savedProfile.get("blocklocker:u") instanceof long[] pair
                        && pair.length == 2)
                    firstOwner = new UUID(pair[0], pair[1]);
                continue;
            }
            if (BlockLockerEveryoneText.matches(visible, everyoneAliases)) {
                everyone = true;
                continue;
            }
            if (slot == 0 && frontType.orElseThrow() == BlockLockerHeaderMatcher.Type.PRIVATE)
                firstOwner = locketteProUuid(visible).orElse(null);
        }
        return Optional.of(new SignProfiles(frontType.orElseThrow(),
                Optional.ofNullable(firstOwner), everyone, !everyone));
    }

    private static List<String> decode(List<String> saved, DisplayDecoder decoder)
            throws IOException {
        List<String> visible = new ArrayList<>(4);
        for (String raw : saved) {
            String line;
            try {
                line = decoder.decode(raw);
            } catch (RuntimeException invalid) {
                throw new IOException("Cannot decode saved sign component", invalid);
            }
            if (line == null)
                throw new IOException("Missing decoded sign display line");
            visible.add(line);
        }
        return List.copyOf(visible);
    }

    private static Optional<UUID> locketteProUuid(String displayed) {
        String stripped = BlockLockerHeaderMatcher.clean(displayed);
        int separator = stripped.indexOf('#');
        if (separator <= 0) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(stripped.substring(separator + 1)));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }
}
