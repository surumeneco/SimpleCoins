package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Converts caller-decoded visible BlockLocker 1.14.1 sign text into an
 * ownership-aggregation input. Raw JSON decoding, sign block-state validation,
 * identity lookup and coherent snapshot acquisition remain caller duties.
 * Saved PDC values are hints, never an independent owner or Everyone proof.
 */
public final class BlockLockerVisibleSignResolver {
    private BlockLockerVisibleSignResolver() {}

    @FunctionalInterface
    public interface PrimaryIdentity {
        /** Resolve the displayed first profile against an authoritative source. */
        ExternalOwnerDecision.Evidence resolve(String displayedName) throws IOException;
    }

    @FunctionalInterface
    public interface VisibleTextDecoder {
        /** Decode one saved JSON/legacy text component to visible plain text. */
        String decode(String rawComponent) throws IOException;
    }

    public static Optional<BlockLockerProtectionResolution.VerifiedSign> fromSaved(
            BlockLockerSignAttachment.Position position,
            BlockLockerSignAttachment.Mount mount,
            BlockLockerSavedSignCandidates.Evidence saved,
            VisibleTextDecoder decoder,
            Set<String> privateAliases,
            Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            PrimaryIdentity primaryIdentity) throws IOException {
        if (saved == null || decoder == null)
            throw new IOException("Missing saved BlockLocker evidence or component decoder");
        if (saved.displayText().isEmpty()) {
            if (saved.pdcHints().isPresent())
                throw new IOException("Saved BlockLocker PDC without authoritative visible text");
            return Optional.empty();
        }
        List<String> raw = saved.displayText().orElseThrow().front();
        java.util.ArrayList<String> decoded = new java.util.ArrayList<>(4);
        for (String component : raw) {
            String line = decoder.decode(component);
            if (line == null)
                throw new IOException("Unresolved saved BlockLocker visible component");
            decoded.add(line);
        }
        return verify(position, mount, decoded, saved.pdcHints(),
                privateAliases, moreUsersAliases, everyoneAliases, primaryIdentity);
    }

    public static Optional<BlockLockerProtectionResolution.VerifiedSign> verify(
            BlockLockerSignAttachment.Position position,
            BlockLockerSignAttachment.Mount mount,
            List<String> decodedFront,
            Optional<BlockLockerSavedHints.Hints> savedHints,
            Set<String> privateAliases,
            Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            PrimaryIdentity primaryIdentity) throws IOException {
        if (position == null || mount == null || decodedFront == null ||
                decodedFront.size() != 4 || decodedFront.stream().anyMatch(Objects::isNull) ||
                savedHints == null)
            throw new IOException("Missing or malformed verified visible sign evidence");

        Optional<BlockLockerHeaderMatcher.Type> header = BlockLockerHeaderMatcher.match(
                decodedFront.get(0), privateAliases, moreUsersAliases);
        if (header.isEmpty()) {
            if (savedHints.isPresent())
                throw new IOException("Saved BlockLocker PDC conflicts with visible sign header");
            return Optional.empty();
        }
        if (savedHints.isPresent() &&
                !savedHints.orElseThrow().header().equals(header.orElseThrow().name()))
            throw new IOException("Saved BlockLocker header conflicts with visible sign text");

        boolean everyone = false;
        for (int i = 1; i < 4; i++)
            everyone |= BlockLockerEveryoneText.matches(decodedFront.get(i), everyoneAliases);

        ExternalOwnerDecision.Evidence primary = null;
        if (header.orElseThrow() == BlockLockerHeaderMatcher.Type.PRIVATE) {
            if (primaryIdentity == null)
                throw new IOException("Missing authoritative BlockLocker identity resolver");
            String displayedName = decodedFront.get(1).trim();
            if (displayedName.isEmpty()) {
                primary = ExternalOwnerDecision.unknown();
            } else {
                primary = primaryIdentity.resolve(displayedName);
                if (primary == null)
                    primary = ExternalOwnerDecision.unknown();
            }
            if (savedHints.isPresent() &&
                    savedHints.orElseThrow().firstProfileUuid().isPresent()) {
                var hinted = savedHints.orElseThrow().firstProfileUuid().orElseThrow();
                if (primary.kind() != ExternalOwnerDecision.Kind.PERSONAL ||
                        !hinted.equals(primary.owner()))
                    primary = ExternalOwnerDecision.unknown();
            }
        }
        return Optional.of(new BlockLockerProtectionResolution.VerifiedSign(
                position, mount, header.orElseThrow(), primary, everyone));
    }
}
