package co.surumene.simplecoins.domain;

import java.util.UUID;

/** Owner resolution is separate from disk traversal and BAN-source adapters. */
public final class AssetOwner {
    private AssetOwner() {}

    public enum Kind { PERSONAL, FREE, DEAD }
    public enum DeadReason { CURRENT_BAN, MISSING_PLAYERDATA }

    public record Result(Kind kind, UUID player, DeadReason reason) {
        public Result {
            if (kind == Kind.FREE && (player != null || reason != null))
                throw new IllegalArgumentException("Free assets have no owner");
            if (kind != Kind.FREE && player == null)
                throw new IllegalArgumentException("Owner UUID required");
        }
    }

    public static Result resolve(UUID owner, boolean everyoneProtected, boolean currentlyBanned, boolean hasPlayerdata) {
        if (owner == null || everyoneProtected) return new Result(Kind.FREE, null, null);
        if (currentlyBanned) return new Result(Kind.DEAD, owner, DeadReason.CURRENT_BAN);
        if (!hasPlayerdata) return new Result(Kind.DEAD, owner, DeadReason.MISSING_PLAYERDATA);
        return new Result(Kind.PERSONAL, owner, null);
    }

    /** WWC/WWW: commander, then tame owner, else Free; unknown UUID must not be guessed. */
    public static UUID mobOwner(UUID commander, UUID tameOwner) {
        return commander != null ? commander : tameOwner;
    }
}
