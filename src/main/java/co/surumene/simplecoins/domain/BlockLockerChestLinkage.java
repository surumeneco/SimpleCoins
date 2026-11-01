package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;

/**
 * Reciprocal vanilla double-chest geometry matching BlockLocker 1.14.1's
 * BlockFinder rotation. Inputs must be verified block states from a coherent
 * source; this does not establish ownership or resolve protection signs.
 */
public final class BlockLockerChestLinkage {
    private BlockLockerChestLinkage() {}

    public enum Facing { NORTH, EAST, SOUTH, WEST }
    public enum Half { SINGLE, LEFT, RIGHT }
    public enum Kind { CHEST, TRAPPED_CHEST }

    public record Position(int x, int y, int z) {}

    public record Part(Position position, Facing facing, Half half, Kind kind) {
        public Part {
            Objects.requireNonNull(position);
            Objects.requireNonNull(facing);
            Objects.requireNonNull(half);
            Objects.requireNonNull(kind);
        }
    }

    public static boolean linked(Part first, Part second) throws IOException {
        if (first == null || second == null)
            throw new IOException("Missing verified chest block state");
        if (first.half() == Half.SINGLE || second.half() == Half.SINGLE ||
                first.half() == second.half() ||
                first.facing() != second.facing() || first.kind() != second.kind())
            return false;
        return neighbor(first).equals(second.position()) &&
                neighbor(second).equals(first.position());
    }

    /**
     * Geometry-only attachment to either half of a verified double chest.
     * A nonreciprocal pair is invalid input, not evidence of two owners.
     */
    public static boolean signAttachedToPair(
            BlockLockerSignAttachment.Position sign,
            BlockLockerSignAttachment.Mount mount,
            Part first, Part second) throws IOException {
        if (!linked(first, second))
            throw new IOException("Unverified double-chest physical linkage");
        return BlockLockerSignAttachment.attaches(sign, mount,
                new BlockLockerSignAttachment.Position(first.position().x(),
                        first.position().y(), first.position().z())) ||
                BlockLockerSignAttachment.attaches(sign, mount,
                        new BlockLockerSignAttachment.Position(second.position().x(),
                                second.position().y(), second.position().z()));
    }

    private static Position neighbor(Part chest) throws IOException {
        int dx = 0, dz = 0;
        switch (chest.facing()) {
            case NORTH -> dx = 1;
            case EAST -> dz = 1;
            case SOUTH -> dx = -1;
            case WEST -> dz = -1;
        }
        if (chest.half() == Half.RIGHT) {
            dx = -dx;
            dz = -dz;
        }
        try {
            return new Position(Math.addExact(chest.position().x(), dx),
                    chest.position().y(), Math.addExact(chest.position().z(), dz));
        } catch (ArithmeticException invalid) {
            throw new IOException("Double-chest neighbor coordinate overflow", invalid);
        }
    }
}
