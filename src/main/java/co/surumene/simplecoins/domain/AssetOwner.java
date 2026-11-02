package co.surumene.simplecoins.domain;

import java.util.UUID;

/** Post-validation classification only; unknown ownership or BAN evidence must fail closed upstream. */
public final class AssetOwner {
    private AssetOwner() {}

    public enum Kind { PERSONAL, FREE, DEAD }
    public enum DeadReason { CURRENT_BAN, MISSING_PLAYERDATA }

    public record Result(Kind kind, UUID player, DeadReason reason) {
        public Result {
            if (kind == null)
                throw new IllegalArgumentException("Owner classification kind required");
            if (kind == Kind.FREE && (player != null || reason != null))
                throw new IllegalArgumentException("Free assets have no owner or dead reason");
            if (kind == Kind.PERSONAL && (player == null || reason != null))
                throw new IllegalArgumentException("Personal assets require an owner and no dead reason");
            if (kind == Kind.DEAD && (player == null || reason == null))
                throw new IllegalArgumentException("Dead assets require an owner and a dead reason");
        }
    }

    public static Result resolve(UUID owner, boolean everyoneProtected, boolean currentlyBanned, boolean hasPlayerdata) {
        if (owner == null || everyoneProtected) return new Result(Kind.FREE, null, null);
        if (currentlyBanned) return new Result(Kind.DEAD, owner, DeadReason.CURRENT_BAN);
        if (!hasPlayerdata) return new Result(Kind.DEAD, owner, DeadReason.MISSING_PLAYERDATA);
        return new Result(Kind.PERSONAL, owner, null);
    }

    /** Prevalidated WWC/WWW hints: commander, then tame owner. Null is not proof of FREE. */
    public static UUID mobOwner(UUID commander, UUID tameOwner) {
        return commander != null ? commander : tameOwner;
    }
}
