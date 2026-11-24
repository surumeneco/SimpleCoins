package co.surumene.simplecoins.domain;

/** Signed account movements for actual ground item events. */
public enum GroundItemMovement {
    SPAWN, DESPAWN, PLAYER_DROP, PLAYER_PICKUP;
    public record Delta(int free, int personal) {}
    public Delta delta(boolean included, boolean confirmed) {
        if (!confirmed) return new Delta(0, 0);
        int world = included ? 1 : 0;
        return switch (this) {
            case SPAWN -> new Delta(world, 0);
            case DESPAWN -> new Delta(-world, 0);
            case PLAYER_DROP -> new Delta(0, -1);
            case PLAYER_PICKUP -> new Delta(-world, 1);
        };
    }
}
