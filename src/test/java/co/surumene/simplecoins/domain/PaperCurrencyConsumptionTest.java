package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PaperCurrencyConsumptionTest {
    @Test void deniesCurrencyConsumedByNonExchangeWorkstations() {
        assertTrue(PaperCurrencyConsumption.blocksResult("MERCHANT", 2, true, false));
        assertTrue(PaperCurrencyConsumption.blocksResult("MERCHANT", 2, false, true));
        assertTrue(PaperCurrencyConsumption.blocksResult("CARTOGRAPHY", 2, true, false));
        assertTrue(PaperCurrencyConsumption.blocksResult("CARTOGRAPHY", 2, false, true));
    }
    @Test void neverInterferesWithNonResultOrNormalItems() {
        assertFalse(PaperCurrencyConsumption.blocksResult("MERCHANT", 1, true, false));
        assertFalse(PaperCurrencyConsumption.blocksResult("MERCHANT", 2, false, false));
        assertFalse(PaperCurrencyConsumption.blocksResult("CHEST", 2, true, true));
        assertFalse(PaperCurrencyConsumption.blocksResult("WORKBENCH", 2, true, true));
    }
}
