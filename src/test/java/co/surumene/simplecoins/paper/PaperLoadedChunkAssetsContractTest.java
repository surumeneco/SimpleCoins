package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure data contract checks. A separate isolated Paper 26.2 workflow must
 * exercise actual Chest Inventory and dropped Item Entity runtime behavior.
 */
class PaperLoadedChunkAssetsContractTest {
    @Test void sliceSumsPositiveMonetaryEntriesWithoutClaimingCompleteness() {
        var entries = List.of(
                new PaperLoadedChunkAssets.Entry("world/test/1,64,1:0", "test", BigInteger.valueOf(300)),
                new PaperLoadedChunkAssets.Entry("world/test/entity/uuid", "test", BigInteger.valueOf(50)));
        var slice = new PaperLoadedChunkAssets.Slice("test", 0, 0, 1, 1, 0, 2, 28, entries);
        assertEquals(BigInteger.valueOf(350), slice.total());
        assertEquals(1, slice.scannedBlockInventories());
        assertEquals(1, slice.scannedItemEntities());
        assertFalse(slice.isCompleteSettlementSource());
        assertThrows(UnsupportedOperationException.class, () -> slice.entries().clear());
    }

    @Test void nullOrNonpositiveCurrencyCannotEnterAResultEntry() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedChunkAssets.Entry("slot", "world", BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedChunkAssets.Entry("slot", "world", BigInteger.valueOf(-1)));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedChunkAssets.Entry("", "world", BigInteger.ONE));
    }

    @Test void returnedEntriesAreNotBackedByCallerMutableList() {
        var mutable = new java.util.ArrayList<PaperLoadedChunkAssets.Entry>();
        mutable.add(new PaperLoadedChunkAssets.Entry("a", "w", BigInteger.ONE));
        var slice = new PaperLoadedChunkAssets.Slice("w", 0, 0, 1, 0, 0, 0, 1, mutable);
        mutable.clear();
        assertEquals(BigInteger.ONE, slice.total());
        assertEquals(1, slice.entries().size());
    }
}
