package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A bounded, read-only A04 observation over explicitly supplied SAVED Chunks.
 * Reconstructs physically attached BlockLocker v1.15 signs for a chest group.
 * No global source completeness, same-writer epoch, owner BAN or publication is
 * implied by successful local observation. Missing neighboring data fails.
 */
public final class SavedBlockLockerAttachedProtection {
    private SavedBlockLockerAttachedProtection() {}

    public record ChunkKey(int x, int z) {}

    public record Observation(
            Set<BlockLockerChestLinkage.Position> parts,
            Map<BlockLockerSignAttachment.Position, BlockLockerSavedProfileResolution.SignProfiles> appliedSigns,
            Optional<UUID> primaryOwner,
            boolean everyoneEffective) {
        public Observation {
            Objects.requireNonNull(parts);
            Objects.requireNonNull(appliedSigns);
            Objects.requireNonNull(primaryOwner);
            if (parts.isEmpty())
                throw new IllegalArgumentException("No verified chest parts");
            parts = Set.copyOf(parts);
            appliedSigns = Map.copyOf(appliedSigns);
        }
        public boolean hasProtectionSign() {
            return !appliedSigns.isEmpty();
        }
    }

    public static Observation observe(
            Map<ChunkKey, Map<String, Object>> selectedChunks,
            int x, int y, int z,
            Set<String> privateHeaders,
            Set<String> moreHeaders,
            Set<String> everyoneAliases,
            BlockLockerSavedProfileResolution.DisplayDecoder decoder)
            throws IOException {
        if (selectedChunks == null || selectedChunks.isEmpty()
                || privateHeaders == null || moreHeaders == null
                || everyoneAliases == null || decoder == null)
            throw new IOException("Missing selected SAVED source or BlockLocker parsing contract");

        var first = chestPart(selectedChunks, x, y, z);
        List<BlockLockerChestLinkage.Part> group = new ArrayList<>(2);
        group.add(first);
        if (first.half() != BlockLockerChestLinkage.Half.SINGLE) {
            BlockLockerChestLinkage.Position neighbor = expectedNeighbor(first);
            var second = chestPart(selectedChunks, neighbor.x(), neighbor.y(), neighbor.z());
            if (!BlockLockerChestLinkage.linked(first, second))
                throw new IOException("SAVED double-chest halves are not reciprocal");
            group.add(second);
        }

        Map<BlockLockerSignAttachment.Position, BlockLockerSavedProfileResolution.SignProfiles>
                applied = new LinkedHashMap<>();
        for (var part : group) {
            var p = part.position();
            int[][] offsets = {{0,0,-1},{1,0,0},{0,0,1},{-1,0,0},{0,1,0}};
            for (int[] offset : offsets) {
                final int sx, sy, sz;
                try {
                    sx = Math.addExact(p.x(), offset[0]);
                    sy = Math.addExact(p.y(), offset[1]);
                    sz = Math.addExact(p.z(), offset[2]);
                } catch (ArithmeticException overflow) {
                    throw new IOException("Saved sign neighborhood coordinate overflow", overflow);
                }
                var signState = block(selectedChunks, sx, sy, sz);
                Optional<BlockLockerSignAttachment.Mount> mount = signMount(signState);
                if (mount.isEmpty())
                    continue;
                var signPosition = new BlockLockerSignAttachment.Position(sx, sy, sz);
                var containerPosition = new BlockLockerSignAttachment.Position(
                        p.x(), p.y(), p.z());
                if (!BlockLockerSignAttachment.attaches(
                        signPosition, mount.orElseThrow(), containerPosition))
                    continue;

                var sign = savedSignEntity(selectedChunks, sx, sy, sz);
                var parsed = BlockLockerSavedProfileResolution.read(
                        sign, privateHeaders, moreHeaders, everyoneAliases, decoder);
                if (parsed.isPresent()) {
                    var previous = applied.putIfAbsent(signPosition, parsed.orElseThrow());
                    if (previous != null && !previous.equals(parsed.orElseThrow()))
                        throw new IOException("Contradictory SAVED sign at one physical position");
                }
            }
        }
        Optional<UUID> owner = Optional.empty();
        boolean everyone = false;
        int mainSigns = 0;
        for (var sign : applied.values()) {
            everyone |= sign.everyoneEffective();
            if (sign.type() == BlockLockerHeaderMatcher.Type.PRIVATE) {
                mainSigns++;
                if (mainSigns == 1)
                    owner = sign.primaryOwner();
            }
        }
        if (mainSigns > 1)
            throw new IOException("Multiple competing PRIVATE signs for one SAVED chest group");

        Set<BlockLockerChestLinkage.Position> parts = new LinkedHashSet<>();
        for (var part : group) parts.add(part.position());
        return new Observation(parts, applied, owner, everyone);
    }

    private static BlockLockerChestLinkage.Part chestPart(
            Map<ChunkKey, Map<String,Object>> sources, int x, int y, int z)
            throws IOException {
        var state = block(sources, x, y, z);
        BlockLockerChestLinkage.Kind kind = switch (state.name()) {
            case "minecraft:chest" -> BlockLockerChestLinkage.Kind.CHEST;
            case "minecraft:trapped_chest" -> BlockLockerChestLinkage.Kind.TRAPPED_CHEST;
            default -> throw new IOException("Unsupported SAVED chest block: " + state.name());
        };
        String facing = state.properties().get("facing");
        String type = state.properties().get("type");
        if (facing == null || type == null)
            throw new IOException("SAVED chest direction or half unavailable");
        try {
            return new BlockLockerChestLinkage.Part(
                    new BlockLockerChestLinkage.Position(x,y,z),
                    BlockLockerChestLinkage.Facing.valueOf(facing.toUpperCase(Locale.ROOT)),
                    BlockLockerChestLinkage.Half.valueOf(type.toUpperCase(Locale.ROOT)),
                    kind);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid SAVED chest block state", invalid);
        }
    }

    private static BlockLockerChestLinkage.Position expectedNeighbor(
            BlockLockerChestLinkage.Part first) throws IOException {
        int dx=0,dz=0;
        switch (first.facing()) {
            case NORTH -> dx=1;
            case EAST -> dz=1;
            case SOUTH -> dx=-1;
            case WEST -> dz=-1;
        }
        if (first.half() == BlockLockerChestLinkage.Half.RIGHT) {
            dx=-dx; dz=-dz;
        }
        try {
            return new BlockLockerChestLinkage.Position(
                    Math.addExact(first.position().x(),dx),
                    first.position().y(),
                    Math.addExact(first.position().z(),dz));
        } catch (ArithmeticException overflow) {
            throw new IOException("SAVED double-chest position overflow", overflow);
        }
    }

    private static SavedTerrainBlockPalette.BlockState block(
            Map<ChunkKey, Map<String,Object>> sources, int x, int y, int z)
            throws IOException {
        var chunk=sources.get(new ChunkKey(Math.floorDiv(x,16),Math.floorDiv(z,16)));
        if (chunk == null)
            throw new IOException("Missing required neighboring SAVED Chunk");
        return SavedTerrainBlockPalette.read(chunk,x,y,z);
    }

    private static Optional<BlockLockerSignAttachment.Mount> signMount(
            SavedTerrainBlockPalette.BlockState block) throws IOException {
        String name=block.name();
        if (!name.startsWith("minecraft:") || !name.endsWith("_sign")
                || name.endsWith("_hanging_sign"))
            return Optional.empty();
        if (name.endsWith("_wall_sign")) {
            String facing=block.properties().get("facing");
            if (facing == null)
                throw new IOException("Saved wall sign has no facing");
            try {
                return Optional.of(BlockLockerSignAttachment.Mount.valueOf(
                        "WALL_" + facing.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid saved wall-sign facing", invalid);
            }
        }
        return Optional.of(BlockLockerSignAttachment.Mount.STANDING);
    }

    private static Map<String,Object> savedSignEntity(
            Map<ChunkKey, Map<String,Object>> sources,
            int x, int y, int z) throws IOException {
        var chunk=sources.get(new ChunkKey(Math.floorDiv(x,16),Math.floorDiv(z,16)));
        if (chunk == null || !(chunk.get("block_entities") instanceof List<?> entities))
            throw new IOException("Missing SAVED sign block-entity source");
        Map<String,Object> found=null;
        for (Object raw : entities) {
            if (!(raw instanceof Map<?,?> entity)
                    || !(entity.get("x") instanceof Integer bx)
                    || !(entity.get("y") instanceof Integer by)
                    || !(entity.get("z") instanceof Integer bz)
                    || !(entity.get("id") instanceof String id)
                    || entity.keySet().stream().anyMatch(key->!(key instanceof String)))
                throw new IOException("Malformed SAVED block entity");
            if (bx!=x || by!=y || bz!=z) continue;
            if (found!=null || !"minecraft:sign".equals(id))
                throw new IOException("Missing or duplicate SAVED physical sign block-entity");
            @SuppressWarnings("unchecked")
            Map<String,Object> cast=(Map<String,Object>)entity;
            found=cast;
        }
        if (found==null)
            throw new IOException("Physical SAVED sign lacks an authoritative block entity");
        return found;
    }
}
