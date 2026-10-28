package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WalletDragProtectionTest {
    @Test void protectsBackpackWalletSlotAsWellAsTopButtons() {
        // Nine top slots, then 27 main-inventory slots, then nine hotbar slots.
        assertTrue(WalletDragProtection.shouldCancel(Set.of(4), 9, 5, 3));
        assertTrue(WalletDragProtection.shouldCancel(Set.of(9 + 27 + 3), 9, 5, 3));
        assertFalse(WalletDragProtection.shouldCancel(Set.of(9, 10), 9, 5, 3));
        assertFalse(WalletDragProtection.shouldCancel(Set.of(9 + 27 + 4), 9, 5, 3));
    }

    @Test void anyProtectedSlotCancelsTheEntireMultiSlotDrag() {
        assertTrue(WalletDragProtection.shouldCancel(Set.of(16, 39, 44), 9, 5, 3));
        assertTrue(WalletDragProtection.shouldCancel(Set.of(1, 17), 9, 5, 3));
    }
}
