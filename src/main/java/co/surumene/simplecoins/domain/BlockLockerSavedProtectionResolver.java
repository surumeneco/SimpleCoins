package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Assemble read-only saved BlockLocker sign candidates from a caller-proven
 * complete/coherent neighborhood. This class does not enumerate chunks,
 * authenticate the palette lookup or freeze a live world.
 */
public final class BlockLockerSavedProtectionResolver {
    private BlockLockerSavedProtectionResolver() {}

    public static ExternalOwnerDecision.Evidence single(
            BlockLockerSignAttachment.Position container,
            Map<String, BlockLockerSavedSignCandidates.Evidence> signs,
            Map<String, Map<String, Object>> verifiedPaletteStates,
            BlockLockerVisibleSignResolver.VisibleTextDecoder decoder,
            Set<String> privateAliases, Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            BlockLockerVisibleSignResolver.PrimaryIdentity identity) throws IOException {
        return BlockLockerProtectionResolution.single(container, assemble(
                signs, verifiedPaletteStates, decoder,
                privateAliases, moreUsersAliases, everyoneAliases, identity));
    }

    public static ExternalOwnerDecision.Evidence doubleChest(
            BlockLockerChestLinkage.Part first, BlockLockerChestLinkage.Part second,
            Map<String, BlockLockerSavedSignCandidates.Evidence> signs,
            Map<String, Map<String, Object>> verifiedPaletteStates,
            BlockLockerVisibleSignResolver.VisibleTextDecoder decoder,
            Set<String> privateAliases, Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            BlockLockerVisibleSignResolver.PrimaryIdentity identity) throws IOException {
        return BlockLockerProtectionResolution.doubleChest(first, second, assemble(
                signs, verifiedPaletteStates, decoder,
                privateAliases, moreUsersAliases, everyoneAliases, identity));
    }

    private static List<BlockLockerProtectionResolution.VerifiedSign> assemble(
            Map<String, BlockLockerSavedSignCandidates.Evidence> signs,
            Map<String, Map<String, Object>> states,
            BlockLockerVisibleSignResolver.VisibleTextDecoder decoder,
            Set<String> privateAliases, Set<String> moreUsersAliases,
            Set<String> everyoneAliases,
            BlockLockerVisibleSignResolver.PrimaryIdentity identity) throws IOException {
        if (signs == null || states == null || !signs.keySet().equals(states.keySet()))
            throw new IOException("Missing or inconsistent saved BlockLocker sign palette set");
        List<BlockLockerProtectionResolution.VerifiedSign> verified = new ArrayList<>();
        for (var entry : signs.entrySet()) {
            if (entry.getValue() == null)
                throw new IOException("Missing saved sign evidence");
            var position = parsePosition(entry.getKey());
            var mount = BlockLockerSavedSignMount.fromPaletteState(states.get(entry.getKey()));
            Optional<BlockLockerProtectionResolution.VerifiedSign> sign =
                    BlockLockerVisibleSignResolver.fromSaved(
                            position, mount, entry.getValue(), decoder,
                            privateAliases, moreUsersAliases, everyoneAliases, identity);
            sign.ifPresent(verified::add);
        }
        return List.copyOf(verified);
    }

    private static BlockLockerSignAttachment.Position parsePosition(String key)
            throws IOException {
        if (key == null)
            throw new IOException("Missing saved sign position");
        String[] coordinates = key.split(",", -1);
        if (coordinates.length != 3)
            throw new IOException("Malformed saved sign position");
        try {
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            int z = Integer.parseInt(coordinates[2]);
            if (!key.equals(x + "," + y + "," + z))
                throw new IOException("Noncanonical saved sign position");
            return new BlockLockerSignAttachment.Position(x, y, z);
        } catch (NumberFormatException invalid) {
            throw new IOException("Invalid saved sign position", invalid);
        }
    }
}
