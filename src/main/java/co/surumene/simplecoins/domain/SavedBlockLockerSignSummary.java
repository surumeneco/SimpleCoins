package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded interpretation of effective profiles on already physically attached
 * SAVED BlockLocker 1.15 signs. This summary is not an ownership verdict:
 * no writer epoch, source completeness, P/D or global settlement is attested.
 */
public final class SavedBlockLockerSignSummary {
    private SavedBlockLockerSignSummary() {}

    public record Summary(boolean protectionSignObserved,
                          Optional<UUID> verifiedMainOwner,
                          boolean everyoneEffective,
                          boolean everyoneAbsent,
                          boolean mainOwnerConflict) {
        public Summary {
            Objects.requireNonNull(verifiedMainOwner);
            if (everyoneEffective && everyoneAbsent)
                throw new IllegalArgumentException("Contradictory effective Everyone evidence");
        }
    }

    public static Summary summarize(SavedBlockLockerChestSigns.Observation observation)
            throws IOException {
        if (observation == null)
            throw new IOException("No saved attached-sign observation");
        var signs = observation.attachedSigns();
        if (signs.isEmpty())
            return new Summary(false, Optional.empty(), false, false, false);

        Set<SavedBlockLockerChestSigns.Position> locations = new HashSet<>();
        Set<UUID> principalCandidates = new LinkedHashSet<>();
        boolean unknownMain = false;
        boolean everyone = false;
        for (var sign : signs) {
            if (sign == null || !locations.add(sign.position()))
                throw new IOException("Duplicated or missing attached sign evidence");
            var profile = sign.profiles();
            everyone |= profile.everyoneEffective();
            if (profile.type() == BlockLockerHeaderMatcher.Type.PRIVATE) {
                if (profile.primaryOwner().isPresent())
                    principalCandidates.add(profile.primaryOwner().orElseThrow());
                else
                    unknownMain = true;
            }
        }
        boolean conflicting = principalCandidates.size() > 1;
        Optional<UUID> owner = !conflicting && !unknownMain && principalCandidates.size() == 1
                ? Optional.of(principalCandidates.iterator().next())
                : Optional.empty();
        return new Summary(true, owner, everyone, !everyone, conflicting);
    }
}
