package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A Paper run exercises actual Chunk.getLoadedChunks and cross-chunk wallets. */
class PaperLoadedWorldAssetsContractTest {
    @Test void resultSumsPositiveEntriesButAlwaysRemainsIncomplete() {
        var source = new ArrayList<>(List.of(
                new PaperLoadedChunkAssets.Entry("world/w/0,64,0:0", "w", BigInteger.valueOf(100)),
                new PaperLoadedChunkAssets.Entry("world/w/32,64,0:0", "w", BigInteger.valueOf(500))));
        var report = new PaperLoadedWorldAssets.Report("w", 2, 2, 0, 0, 0, 54, source);
        source.clear();
        assertEquals(BigInteger.valueOf(600), report.total());
        assertEquals(2, report.scannedChunks());
        assertEquals(0, report.scannedDisplayEntities());
        assertEquals(54, report.scannedSlots());
        assertEquals(2, report.entries().size());
        assertFalse(report.isCompleteSettlementSource());
        assertThrows(UnsupportedOperationException.class, () -> report.entries().clear());
    }

    @Test void displayedItemsRemainDistinctFromDroppedItemCounts() {
        var reported = new PaperLoadedWorldAssets.Report("w", 1, 0, 0, 2, 0, 1, 2,
                List.of(new PaperLoadedChunkAssets.Entry("world/w/entity/u/display",
                        "w", BigInteger.valueOf(500))));
        assertEquals(2, reported.scannedDisplayEntities());
        assertEquals(0, reported.scannedItemEntities());
        assertEquals(BigInteger.valueOf(500), reported.total());
        assertFalse(reported.isCompleteSettlementSource());
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedWorldAssets.Report("w", 1, 0, 0, -1, 0, 0, 0, List.of()));
    }

    @Test void refusesInvalidOrCrossWorldResult() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedWorldAssets.Report("w", -1, 0, 0, 0, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLoadedWorldAssets.Report("w", 1, 1, 0, 0, 0, 27,
                        List.of(new PaperLoadedChunkAssets.Entry("x", "other", BigInteger.ONE))));
        assertThrows(NullPointerException.class, () ->
                new PaperLoadedWorldAssets.Report("w", 0, 0, 0, 0, 0, 0, null));
    }
}
