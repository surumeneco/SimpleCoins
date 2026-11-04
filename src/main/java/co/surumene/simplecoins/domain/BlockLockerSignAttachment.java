package co.surumene.simplecoins.domain;

import java.io.IOException;

/**
 * Pure BlockLocker 1.14.1 wall/standing sign support geometry. The caller must
 * supply a verified physical sign block state; this class never infers facing
 * from PDC, parses protection text or assigns economic ownership.
 */
public final class BlockLockerSignAttachment {
    private BlockLockerSignAttachment() {}

    /** The four wall-sign front-facing directions, or a standing sign. */
    public enum Mount { WALL_NORTH, WALL_EAST, WALL_SOUTH, WALL_WEST, STANDING }

    public record Position(int x, int y, int z) {}

    public static boolean attaches(Position sign, Mount mount, Position container)
            throws IOException {
        if (sign == null || mount == null || container == null)
            throw new IOException("Missing verified sign mount or physical position");
        try {
            Position support = switch (mount) {
                case WALL_NORTH -> new Position(sign.x(), sign.y(),
                        Math.addExact(sign.z(), 1));
                case WALL_EAST -> new Position(Math.subtractExact(sign.x(), 1),
                        sign.y(), sign.z());
                case WALL_SOUTH -> new Position(sign.x(), sign.y(),
                        Math.subtractExact(sign.z(), 1));
                case WALL_WEST -> new Position(Math.addExact(sign.x(), 1),
                        sign.y(), sign.z());
                case STANDING -> new Position(sign.x(),
                        Math.subtractExact(sign.y(), 1), sign.z());
            };
            return support.equals(container);
        } catch (ArithmeticException invalid) {
            throw new IOException("Saved sign support coordinate overflow", invalid);
        }
    }
}
