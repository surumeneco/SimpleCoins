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
