package co.surumene.simplecoins.domain;

import java.util.Objects;
import java.util.Set;

/**
 * A wallet menu uses a nine-slot top inventory, but the backing wallet is still
 * a mutable item in the player's hotbar. A drag that touches either area must
 * be rejected as one atomic user action.
 */
public final class WalletDragProtection {
    private WalletDragProtection() {}

    public static boolean shouldCancel(Set<Integer> rawSlots, int topSize,
                                       int firstHotbarRawSlot, int heldHotbarSlot) {
        Objects.requireNonNull(rawSlots);
        if (topSize < 0 || firstHotbarRawSlot < topSize
                || heldHotbarSlot < 0 || heldHotbarSlot >= 9) {
            throw new IllegalArgumentException("Invalid inventory layout");
        }
        int protectedRaw = firstHotbarRawSlot + heldHotbarSlot;
        for (int raw : rawSlots) {
            if (raw >= 0 && (raw < topSize || raw == protectedRaw)) return true;
        }
        return false;
    }
}
