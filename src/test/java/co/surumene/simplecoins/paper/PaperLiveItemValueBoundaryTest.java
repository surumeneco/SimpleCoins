package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Pure preflight tests that run without a Bukkit server; actual ItemStack
 * serialization and Chunk traversal require separate Paper 26.2 smoke tests.
 */
class PaperLiveItemValueBoundaryTest {
    @Test void missingSerializationBudgetRejectsEvenAnEmptyStack() {
        assertThrows(IllegalArgumentException.class,
                () -> PaperLiveItemValue.stack(null, 0, 1024));
        assertThrows(IllegalArgumentException.class,
                () -> PaperLiveItemValue.stack(null, 1024, 0));
    }

    @Test void inventoryRequiresNonNullSourceBeforeBukkitAccess() {
        assertThrows(NullPointerException.class,
                () -> PaperLiveItemValue.inventory(null, 10, 1024, 1024));
    }
}
