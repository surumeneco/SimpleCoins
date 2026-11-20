package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
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
    private final WorldStorageDiscovery.Manifest selectedManifest;

    private SavedBlockLockerSettlementOwnerLookup(
            SavedBlockLockerPhysicalOwnerIndex ownerIndex,
            BanSourceCoverage bans,
            Set<UUID> playerData,
            WorldStorageDiscovery.Manifest selectedManifest) {
        this.ownerIndex = ownerIndex;
        this.bans = bans;
        this.playerData = Set.copyOf(playerData);
        this.selectedManifest = Objects.requireNonNull(selectedManifest);
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
        return new SavedBlockLockerSettlementOwnerLookup(index,banEvidence,playerIds,discovered);
    }

    /**
     * Explicit opt-in for saved traversal that keeps the selected Region file
     * and decoded Chunk attached to every physical ownership lookup.
     * Callers must still establish the complete source/writer generation
     * outside this bounded identity check; no snapshot may be published here.
     */
    public SettlementScanRunner.SourceBlockOwnerLookup sourceScoped() {
        return (source, chunk, blockPosition) -> {
            if (source == null || chunk == null
                    || source.kind() != WorldStorageDiscovery.Kind.REGION
                    || !selectedManifest.worldFiles().contains(source))
                throw new IOException("Unknown or unselected SAVED terrain Region");
            int[] position = position(blockPosition);
            if (Math.floorDiv(position[0],16) != chunk.x()
                    || Math.floorDiv(position[2],16) != chunk.z())
                throw new IOException("SAVED chest coordinate is outside selected Chunk");
            final String actualRegion = source.path().getFileName().toString();
            final String expectedRegion = "r." + Math.floorDiv(chunk.x(),32)
                    + "." + Math.floorDiv(chunk.z(),32) + ".mca";
            if (!actualRegion.equals(expectedRegion))
                throw new IOException("Selected Anvil Region is not the Chunk's source");
            requireSavedChestEntity(chunk,position);
            return resolve(source.world(),blockPosition);
        };
    }

    private static int[] position(String physicalBlock) throws IOException {
        if (physicalBlock == null)
            throw new IOException("Missing SAVED block position");
        String[] fields = physicalBlock.split(",",-1);
        if (fields.length != 3)
            throw new IOException("Invalid SAVED BlockEntity coordinates");
        int[] xyz = new int[3];
        for (int i=0;i<3;i++) {
            try {
                xyz[i] = Integer.parseInt(fields[i]);
            } catch (NumberFormatException malformed) {
                throw new IOException("Invalid SAVED BlockEntity coordinate",malformed);
            }
            if (!Integer.toString(xyz[i]).equals(fields[i]))
                throw new IOException("Noncanonical SAVED BlockEntity coordinate");
        }
        return xyz;
    }

    private static void requireSavedChestEntity(AnvilRegionReader.Chunk chunk,
                                                 int[] position) throws IOException {
        if (chunk.nbt() == null
                || !(chunk.nbt().get("block_entities") instanceof List<?> entities))
            throw new IOException("Selected SAVED chunk has no BlockEntity inventory source");
        int matching = 0;
        String tileType = null;
        for (Object raw : entities) {
            if (!(raw instanceof Map<?,?> value)
                    || !(value.get("x") instanceof Integer x)
                    || !(value.get("y") instanceof Integer y)
                    || !(value.get("z") instanceof Integer z)
                    || !(value.get("id") instanceof String id))
                throw new IOException("Malformed SAVED BlockEntity source");
            if (x!=position[0] || y!=position[1] || z!=position[2])
                continue;
            if (!id.equals("minecraft:chest") && !id.equals("minecraft:trapped_chest"))
                throw new IOException("SAVED BlockEntity is not a supported chest");
            if (++matching>1)
                throw new IOException("Duplicate SAVED physical chest BlockEntity");
            tileType = id;
        }
        if (matching!=1)
            throw new IOException("No matching SAVED physical chest BlockEntity");
        // A BlockEntity is not physical chest evidence on its own. Require
        // the selected Chunk's actual terrain palette at this same location.
        var physical = SavedTerrainBlockPalette.read(chunk.nbt(),
                position[0], position[1], position[2]);
        if (!tileType.equals(physical.name()))
            throw new IOException("Orphan SAVED chest BlockEntity disagrees with terrain palette");

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
