package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Source-local, unpublished BlockEntity owner resolution for supported SAVED
 * BlockLocker chest positions. Uses already-verified physical ownership,
 * discovered playerdata and complete BAN source observations; NEVER infers
 * world generation/writer freeze or authorizes settlement publication.
 *
 * Unknown blocks (including other storage types) fail instead of becoming Free.
 * This callback is not yet installed in the global settlement coordinator.
 */
public final class SavedBlockLockerSettlementOwnerLookup
        implements SettlementScanRunner.BlockOwnerLookup {
    private final SavedBlockLockerPhysicalOwnerIndex ownerIndex;
    private final BanSourceCoverage bans;
    private final Set<UUID> playerData;

    private SavedBlockLockerSettlementOwnerLookup(
            SavedBlockLockerPhysicalOwnerIndex ownerIndex,
            BanSourceCoverage bans,
            Set<UUID> playerData) {
        this.ownerIndex = ownerIndex;
        this.bans = bans;
        this.playerData = Set.copyOf(playerData);
    }

    public static SavedBlockLockerSettlementOwnerLookup fromIndex(
            SavedBlockLockerPhysicalOwnerIndex index,
            WorldStorageDiscovery.Manifest discovered,
            BanSourceCoverage banEvidence) throws IOException {
        if (index == null || discovered == null || discovered.origin() == null
                || banEvidence == null)
            throw new IOException("Missing independently discovered saved source or BAN proof");
        // A missing or substituted playerdata source is not equivalent to a
        // zero-balance player. This only checks discovery identity, NOT writer
        // exclusion or immutable Minecraft live state.
        discovered.requireUnchangedDiscovery();
        Set<UUID> playerIds = discovered.playerDataFiles().keySet();
        banEvidence.bannedAmong(playerIds);
        return new SavedBlockLockerSettlementOwnerLookup(index,banEvidence,playerIds);
    }

    @Override
    public BlockEntitySettlementImport.Ownership resolve(
            String world, String blockPosition) throws IOException {
        var holder=ownerIndex.lookup(world,blockPosition);
        if (holder.verifiedOwner().isPresent()) {
            UUID principal=holder.verifiedOwner().orElseThrow();
            return new BlockEntitySettlementImport.Ownership(principal,false,
                    bans.currentlyBanned(principal),playerData.contains(principal));
        }
        if (holder.everyoneEffective())
            return new BlockEntitySettlementImport.Ownership(null,true,false,false);
        if (holder.verifiedNonProtection())
            return new BlockEntitySettlementImport.Ownership(null,false,false,false);
        throw new IOException("Unrecognized SAVED physical ownership candidate");
    }
}
