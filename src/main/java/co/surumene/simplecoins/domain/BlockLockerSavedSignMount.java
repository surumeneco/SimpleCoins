package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Read-only conversion of a verified vanilla saved block palette state into
 * BlockLocker sign support geometry. The caller must prove the block position,
 * palette index and coherent snapshot separately. Hanging signs are not
 * silently treated as wall or standing signs.
 */
public final class BlockLockerSavedSignMount {
    private BlockLockerSavedSignMount() {}

    private static final Set<String> WOODS = Set.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "pale_oak");

    public static BlockLockerSignAttachment.Mount fromPaletteState(
            Map<String, Object> state) throws IOException {
        if (state == null || !(state.get("Name") instanceof String name) ||
                !(state.get("Properties") instanceof Map<?, ?> properties))
            throw new IOException("Missing saved sign palette name or properties");
        if (!name.startsWith("minecraft:"))
            throw new IOException("Unsupported saved sign block namespace");
        String block = name.substring("minecraft:".length());
        boolean wall = block.endsWith("_wall_sign");
        boolean standing = !wall && block.endsWith("_sign");
        if (!wall && !standing)
            throw new IOException("Unsupported saved sign block type");
        String wood = block.substring(0, block.length() - (wall ? "_wall_sign".length() : "_sign".length()));
        if (!WOODS.contains(wood))
            throw new IOException("Unknown vanilla saved sign wood type");
        for (var entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof String key) ||
                    !(entry.getValue() instanceof String value))
                throw new IOException("Malformed saved sign palette property");
            if (!key.equals(wall ? "facing" : "rotation") &&
                    !key.equals("waterlogged"))
                throw new IOException("Unexpected saved sign palette property");
            if (key.equals("waterlogged") && !value.equals("true") && !value.equals("false"))
                throw new IOException("Invalid saved sign waterlogged state");
        }
        Object raw = properties.get(wall ? "facing" : "rotation");
        if (!(raw instanceof String value))
            throw new IOException("Missing saved sign orientation");
        if (wall) {
            return switch (value) {
                case "north" -> BlockLockerSignAttachment.Mount.WALL_NORTH;
                case "east" -> BlockLockerSignAttachment.Mount.WALL_EAST;
                case "south" -> BlockLockerSignAttachment.Mount.WALL_SOUTH;
                case "west" -> BlockLockerSignAttachment.Mount.WALL_WEST;
                default -> throw new IOException("Invalid saved wall sign facing");
            };
        }
        if (!value.matches("(?:[0-9]|1[0-5])"))
            throw new IOException("Invalid saved standing sign rotation");
        return BlockLockerSignAttachment.Mount.STANDING;
    }
}
