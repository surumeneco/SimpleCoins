package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only, local BlockLocker 1.15 chest attachment observer over caller-supplied
 * SAVED terrain Chunks. The caller is responsible for source completeness and
 * same-epoch/writer consistency. This class does not publish or assign assets.
 */
public final class SavedBlockLockerChestSigns {
    private SavedBlockLockerChestSigns() {}

    public record ChunkPosition(int x, int z) {}
    public record Position(int x, int y, int z) {}
    public record AttachedSign(Position position, BlockLockerSavedProfileResolution.SignProfiles profiles) {
        public AttachedSign {
            Objects.requireNonNull(position);
            Objects.requireNonNull(profiles);
        }
    }
    public record Observation(List<Position> storageMembers, List<AttachedSign> attachedSigns) {
        public Observation {
            storageMembers = List.copyOf(storageMembers);
            attachedSigns = List.copyOf(attachedSigns);
        }
    }

    public static Observation observe(
            Map<ChunkPosition, Map<String,Object>> source,
            Position requestedChest,
            Set<String> privateHeaders,
            Set<String> moreHeaders,
            Set<String> everyoneAliases,
            BlockLockerSavedProfileResolution.DisplayDecoder decoder) throws IOException {
        if (source == null || requestedChest == null || decoder == null
                || privateHeaders == null || moreHeaders == null || everyoneAliases == null)
            throw new IOException("Missing saved chest observation input");

        BlockLockerChestLinkage.Part first = readChest(source, requestedChest);
        List<BlockLockerChestLinkage.Part> members = new ArrayList<>(2);
        members.add(first);
        if (first.half() != BlockLockerChestLinkage.Half.SINGLE) {
            Position opposite = otherHalf(first);
            BlockLockerChestLinkage.Part second = readChest(source, opposite);
            if (!BlockLockerChestLinkage.linked(first, second))
                throw new IOException("Saved double-chest halves are not reciprocal");
            members.add(second);
        }

        Map<Position, AttachedSign> signs = new LinkedHashMap<>();
        for (var member : members) {
            Position chest = new Position(member.position().x(), member.position().y(),
                    member.position().z());
            for (Position next : neighbors(chest)) {
                SavedTerrainBlockPalette.BlockState state = readState(source, next);
                BlockLockerSignAttachment.Mount mount = mount(state);
                if (mount == null) continue;
                boolean attached = BlockLockerSignAttachment.attaches(
                        toAttachment(next), mount, toAttachment(chest));
                if (!attached) continue;

                Map<String,Object> chunk = getChunk(source, next);
                Map<String,Object> block = findSavedSign(chunk, next);
                Optional<BlockLockerSavedProfileResolution.SignProfiles> resolved =
                        BlockLockerSavedProfileResolution.read(
                                block, privateHeaders, moreHeaders, everyoneAliases, decoder);
                if (resolved.isPresent()) {
                    signs.putIfAbsent(next, new AttachedSign(next, resolved.orElseThrow()));
                }
            }
        }
        List<Position> physical = members.stream()
                .map(part -> new Position(part.position().x(),
                        part.position().y(), part.position().z()))
                .toList();
        return new Observation(physical, List.copyOf(signs.values()));
    }

    private static BlockLockerChestLinkage.Part readChest(
            Map<ChunkPosition,Map<String,Object>> chunks, Position position) throws IOException {
        SavedTerrainBlockPalette.BlockState state = readState(chunks, position);
        BlockLockerChestLinkage.Kind kind = switch (state.name()) {
            case "minecraft:chest" -> BlockLockerChestLinkage.Kind.CHEST;
            case "minecraft:trapped_chest" -> BlockLockerChestLinkage.Kind.TRAPPED_CHEST;
            default -> throw new IOException("Unsupported saved storage or missing chest at " + position);
        };
        String facing = state.properties().get("facing");
        String type = state.properties().get("type");
        if (facing == null || type == null)
            throw new IOException("Incomplete saved chest properties at " + position);
        try {
            return new BlockLockerChestLinkage.Part(
                    new BlockLockerChestLinkage.Position(position.x(), position.y(), position.z()),
                    BlockLockerChestLinkage.Facing.valueOf(facing.toUpperCase(Locale.ROOT)),
                    BlockLockerChestLinkage.Half.valueOf(type.toUpperCase(Locale.ROOT)),
                    kind);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid saved chest facing/half at " + position, invalid);
        }
    }

    private static Position otherHalf(BlockLockerChestLinkage.Part part) throws IOException {
        int dx = 0, dz = 0;
        switch (part.facing()) {
            case NORTH -> dx = 1;
            case EAST -> dz = 1;
            case SOUTH -> dx = -1;
            case WEST -> dz = -1;
        }
        if (part.half() == BlockLockerChestLinkage.Half.RIGHT) {
            dx = -dx;
            dz = -dz;
        }
        try {
            return new Position(Math.addExact(part.position().x(),dx),
                    part.position().y(),Math.addExact(part.position().z(),dz));
        } catch (ArithmeticException invalid) {
            throw new IOException("Saved double chest position overflow",invalid);
        }
    }

    private static List<Position> neighbors(Position p) throws IOException {
        try {
            return List.of(
                    new Position(p.x(),p.y(),Math.subtractExact(p.z(),1)),
                    new Position(Math.addExact(p.x(),1),p.y(),p.z()),
                    new Position(p.x(),p.y(),Math.addExact(p.z(),1)),
                    new Position(Math.subtractExact(p.x(),1),p.y(),p.z()),
                    new Position(p.x(),Math.addExact(p.y(),1),p.z()));
        } catch (ArithmeticException invalid) {
            throw new IOException("Saved sign-neighbor position overflow",invalid);
        }
    }

    private static BlockLockerSignAttachment.Position toAttachment(Position p) {
        return new BlockLockerSignAttachment.Position(p.x(),p.y(),p.z());
    }

    private static BlockLockerSignAttachment.Mount mount(
            SavedTerrainBlockPalette.BlockState state) throws IOException {
        String name = state.name();
        if (!name.startsWith("minecraft:")) return null;
        if (name.endsWith("_wall_sign") && !name.endsWith("_wall_hanging_sign")) {
            String face = state.properties().get("facing");
            if (face == null)
                throw new IOException("Saved wall sign facing is missing");
            try {
                return BlockLockerSignAttachment.Mount.valueOf(
                        "WALL_" + face.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Unsupported saved wall-sign facing",invalid);
            }
        }
        if (name.endsWith("_sign") && !name.endsWith("_hanging_sign"))
            return BlockLockerSignAttachment.Mount.STANDING;
        return null;
    }

    private static SavedTerrainBlockPalette.BlockState readState(
            Map<ChunkPosition,Map<String,Object>> chunks, Position p) throws IOException {
        return SavedTerrainBlockPalette.read(getChunk(chunks,p),p.x(),p.y(),p.z());
    }

    private static Map<String,Object> getChunk(
            Map<ChunkPosition,Map<String,Object>> chunks, Position p) throws IOException {
        ChunkPosition key = new ChunkPosition(Math.floorDiv(p.x(),16),Math.floorDiv(p.z(),16));
        Map<String,Object> chunk = chunks.get(key);
        if (chunk == null)
            throw new IOException("Required saved terrain Chunk not supplied: " + key);
        return chunk;
    }

    private static Map<String,Object> findSavedSign(
            Map<String,Object> chunk, Position position) throws IOException {
        if (!(chunk.get("block_entities") instanceof List<?> entities))
            throw new IOException("Saved terrain block_entities unavailable at " + position);
        Map<String,Object> found = null;
        for (Object raw : entities) {
            if (!(raw instanceof Map<?,?> entity)
                    || !(entity.get("x") instanceof Integer x)
                    || !(entity.get("y") instanceof Integer y)
                    || !(entity.get("z") instanceof Integer z))
                throw new IOException("Invalid saved block-entity coordinates near " + position);
            if (x != position.x() || y != position.y() || z != position.z()) continue;
            if (found != null || !"minecraft:sign".equals(entity.get("id"))
                    || entity.keySet().stream().anyMatch(k -> !(k instanceof String)))
                throw new IOException("Contradictory saved sign BlockEntity at " + position);
            @SuppressWarnings("unchecked")
            Map<String,Object> values = (Map<String,Object>) entity;
            found = values;
        }
        if (found == null)
            throw new IOException("Physical saved sign has no matching BlockEntity: " + position);
        return found;
    }
}
