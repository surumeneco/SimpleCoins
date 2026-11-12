package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Paper runtime verifies actual worlds; these contract tests guard aggregation. */
class PaperLiveUniverseAssetsContractTest {
    @Test void immutableWorldAndOnlineTotalsPreservePartialSourceMarker() {
        var mutable = new ArrayList<>(List.of(
                new PaperLoadedWorldAssets.Report("a", 1, 1, 0, 0, 0, 27,
                        List.of(new PaperLoadedChunkAssets.Entry(
                                "world/a/1,64,1:0", "a", BigInteger.valueOf(100)))),
                new PaperLoadedWorldAssets.Report("b", 1, 1, 0, 0, 0, 27,
                        List.of(new PaperLoadedChunkAssets.Entry(
                                "world/b/1,64,1:0", "b", BigInteger.valueOf(200))))));
        var online = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        var report = new PaperLiveUniverseAssets.Report(mutable, online, 2, 2, 0, 54);
        mutable.clear();
        assertEquals(2, report.worlds().size());
        assertEquals(BigInteger.valueOf(300), report.total());
        assertEquals(54, report.scannedSlots());
        assertFalse(report.isCompleteSettlementSource());
        assertThrows(UnsupportedOperationException.class, () -> report.worlds().clear());
    }

    @Test void rejectsDuplicateWorldReportsOrInconsistentSlotAndChunkCounts() {
        var world = new PaperLoadedWorldAssets.Report("a", 1, 0, 0, 0, 0, 0, List.of());
        var online = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Report(List.of(world, world), online, 2, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Report(List.of(world), online, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Report(List.of(world), online, 1, 0, 0, 1));
    }

    @Test void allScanResourceBudgetsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Budget(2, 10, 10, 10, 100,
                        0, 1048576, 1048576));
        assertDoesNotThrow(() ->
                new PaperLiveUniverseAssets.Budget(2, 10, 10, 10, 100,
                        1, 1048576, 1048576));
    }
}
