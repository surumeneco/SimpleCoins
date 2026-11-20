package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only local index of unpublished SAVED BlockLocker chest ownership.
 * Coordinates and world identity remain physical; caller-owned proof flags
 * never grant source/writer generation, settlement or admission on their own.
 */
public final class SavedBlockLockerPhysicalOwnerIndex {
    public record Key(String world, SavedBlockLockerChestSigns.Position block) {
        public Key {
            if (world == null || world.isBlank() || block == null)
                throw new IllegalArgumentException("Missing world or physical block");
        }
    }

    public record Holder(Optional<UUID> verifiedOwner, boolean everyoneEffective,
                         boolean verifiedNonProtection) {
        public Holder {
            Objects.requireNonNull(verifiedOwner);
            int exclusive = (verifiedOwner.isPresent() ? 1 : 0)
                    + (everyoneEffective ? 1 : 0)
                    + (verifiedNonProtection ? 1 : 0);
            if (exclusive != 1)
                throw new IllegalArgumentException("Unpublished holder must have exactly one reason");
        }
    }

    private final Map<Key, Holder> entries;

    private SavedBlockLockerPhysicalOwnerIndex(Map<Key, Holder> verifiedEntries) {
        entries = Map.copyOf(verifiedEntries);
    }

    /** Resolve the exact coordinates emitted by BlockEntitySettlementImport. */
    public Holder lookup(String world, String csvCoordinates) throws IOException {
        if (world == null || world.isBlank())
            throw new IOException("Missing selected physical world");
        SavedBlockLockerChestSigns.Position position = parsePosition(csvCoordinates);
        Holder found = entries.get(new Key(world, position));
        if (found == null)
            throw new IOException("Unobserved SAVED chest is not proven unprotected: " + world
                    + "/" + csvCoordinates);
        return found;
    }

    private static SavedBlockLockerChestSigns.Position parsePosition(String text)
            throws IOException {
        if (text == null)
            throw new IOException("Missing physical BlockEntity position");
        String[] parts = text.split(",", -1);
        if (parts.length != 3)
            throw new IOException("Noncanonical SAVED BlockEntity coordinates");
        int[] pos = new int[3];
        for (int i = 0; i < parts.length; i++) {
            try {
                pos[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException invalid) {
                throw new IOException("Invalid physical BlockEntity coordinate", invalid);
            }
            if (!Integer.toString(pos[i]).equals(parts[i]))
                throw new IOException("Noncanonical physical BlockEntity coordinate");
        }
        return new SavedBlockLockerChestSigns.Position(pos[0],pos[1],pos[2]);
    }

    /**
     * Per-captured-source index transaction. One failed/duplicate/unknown
     * observation invalidates the entire pending index, never a partial commit.
     */
    public static final class Builder {
        private final Map<Key,Holder> pending = new HashMap<>();
        private boolean failed;
        private boolean finished;

        public void accept(String world,
                           SavedBlockLockerChestSigns.Observation observation,
                           SavedBlockLockerOwnerCandidate.ExternalProof independentProof)
                throws IOException {
            if (failed || finished)
                throw new IOException("Pending SAVED physical index is no longer writable");
            try {
                if (world == null || world.isBlank() || observation == null)
                    throw new IOException("Missing saved world or chest observation");
                var candidate=SavedBlockLockerOwnerCandidate.fromObservedSigns(
                        observation,independentProof);
                if (candidate.verdict()==BlockLockerSavedOwnerVerdict.Verdict.HOLD)
                    throw new IOException("Unverified SAVED ownership cannot enter physical index");
                var summary=SavedBlockLockerSignSummary.summarize(observation);
                boolean everyone = candidate.verdict()
                        == BlockLockerSavedOwnerVerdict.Verdict.FREE_UNPUBLISHED
                        && summary.everyoneEffective();
                boolean unprotected = candidate.verdict()
                        == BlockLockerSavedOwnerVerdict.Verdict.FREE_UNPUBLISHED
                        && !summary.protectionSignObserved()
                        && independentProof.nonProtectionVerified();
                Holder holder;
                try {
                    holder=new Holder(candidate.verifiedOwner(),everyone,unprotected);
                } catch (IllegalArgumentException invalid) {
                    throw new IOException("Contradictory saved physical holder evidence",invalid);
                }
                List<SavedBlockLockerChestSigns.Position> blocks = observation.storageMembers();
                if (blocks.size() < 1 || blocks.size() > 2)
                    throw new IOException("Expected one or two verified physical chest parts");
                Set<Key> proposed = new HashSet<>();
                for (var physical : blocks) {
                    Key key=new Key(world, physical);
                    if (!proposed.add(key) || pending.containsKey(key))
                        throw new IOException("Repeated SAVED physical chest position: " + key);
                }
                if (blocks.size()==2) {
                    var a=blocks.get(0);var b=blocks.get(1);
                    long dx=(long)a.x()-b.x(), dz=(long)a.z()-b.z();
                    if (a.y()!=b.y() || Math.abs(dx)+Math.abs(dz)!=1)
                        throw new IOException("Unverified or nonadjacent SAVED double chest parts");
                }
                for (var key : proposed)
                    pending.put(key,holder);
            } catch (IOException | RuntimeException rejected) {
                failed=true;
                throw rejected;
            }
        }

        public SavedBlockLockerPhysicalOwnerIndex finish() throws IOException {
            if (failed || finished || pending.isEmpty())
                throw new IOException("Incomplete, failed or already committed saved owner index");
            finished = true;
            return new SavedBlockLockerPhysicalOwnerIndex(pending);
        }
    }
}
