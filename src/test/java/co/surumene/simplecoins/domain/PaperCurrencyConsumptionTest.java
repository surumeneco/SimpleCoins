package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PaperCurrencyConsumptionTest {
    @Test void protectsResultsForBothInputSlotsInMerchantAndCartography() {
        for (String station : new String[]{"MERCHANT", "CARTOGRAPHY"}) {
            assertTrue(PaperCurrencyConsumption.blocksResult(station, 2, true, false));
            assertTrue(PaperCurrencyConsumption.blocksResult(station, 2, false, true));
            assertTrue(PaperCurrencyConsumption.blocksResult(station, 2, true, true));
            assertFalse(PaperCurrencyConsumption.blocksResult(station, 2, false, false));
            for (int slot : new int[]{-1, 0, 1, 3, 9, 27}) {
                assertFalse(PaperCurrencyConsumption.blocksResult(station, slot, true, true));
            }
        }
    }

    @Test void doesNotInterfereWithUnrelatedInventories() {
        for (String station : new String[]{"CRAFTING", "WORKBENCH", "ANVIL", "FURNACE", "CHEST", "PLAYER", "SMITHING"}) {
            assertFalse(PaperCurrencyConsumption.blocksResult(station, 2, true, true));
        }
        assertFalse(PaperCurrencyConsumption.blocksResult(null, 2, true, true));
    }
}
