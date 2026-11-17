package co.surumene.simplecoins.paper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent acceptance vectors for the wallet GUI drag-policy decision. */
final class WalletDragPolicyTest {
    @Test void lowerOnlyOrdinaryInventoryDragIsNotCancelled() {
        assertFalse(WalletDragPolicy.shouldCancel(false, true, false, true, false));
    }

    @Test void touchingTopSlotRemainsBlocked() {
        assertTrue(WalletDragPolicy.shouldCancel(false, true, false, false, false));
    }

    @Test void touchingWalletSourceRemainsBlockedEvenWithinLowerInventory() {
        assertTrue(WalletDragPolicy.shouldCancel(false, true, false, true, true));
    }

    @Test void draggingWalletItemRemainsBlocked() {
        assertTrue(WalletDragPolicy.shouldCancel(false, true, true, true, false));
    }

    @Test void invalidWalletSessionRemainsBlocked() {
        assertTrue(WalletDragPolicy.shouldCancel(false, false, false, true, false));
    }

    @Test void previouslyCancelledActionCannotBeReopened() {
        assertTrue(WalletDragPolicy.shouldCancel(true, true, false, true, false));
    }
}
