package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Source-level detail must equal exactly the already-scanned Paper aggregate. */
class PaperLiveUniverseSourcesContractTest {
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000006001");
    private static final String BLOCK = "world/a/1,64,1:0";
    private static final String PLAYER_SLOT = "player/" + PLAYER + "/inventory/0";

    private static PaperLiveUniverseAssets.Report report() {
        var block = new PaperLoadedChunkAssets.Entry(BLOCK, "a", BigInteger.valueOf(100));
        var world = new PaperLoadedWorldAssets.Report("a", 1, 1, 0, 0, 0, 27,
                List.of(block));
        var carried = new PaperLivePlayerAssets.Entry(
                PLAYER_SLOT, PLAYER, BigInteger.valueOf(50));
        var online = new PaperLivePlayerAssets.OnlineSet(1, 63,
                List.of(new PaperLivePlayerAssets.Snapshot(PLAYER, 63,
                        List.of(carried))));
        return new PaperLiveUniverseAssets.Report(List.of(world), online,
                1, 1, 0, 90);
    }

    private static HybridLiveSourceStage.ChunkSource chunk() {
        return new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", 0, 0),
                List.of(new HybridLiveSourceStage.Value(BLOCK, "a", BigInteger.valueOf(100))));
    }

    private static HybridLiveSourceStage.PlayerSource player() {
        return new HybridLiveSourceStage.PlayerSource(PLAYER,
                List.of(new HybridLiveSourceStage.Value(
                        PLAYER_SLOT, null, BigInteger.valueOf(50))));
    }

    @Test void correctChunkAndPlayerDetailExactlyReconstructObservedTotal() {
        var observed = new PaperLiveUniverseAssets.Observed(report(),
                List.of(chunk(), player()));
        assertEquals(BigInteger.valueOf(150), observed.report().total());
        assertEquals(2, observed.sources().size());
        assertEquals(1, observed.sources().stream()
                .filter(x -> x instanceof HybridLiveSourceStage.ChunkSource).count());
        assertEquals(1, observed.sources().stream()
                .filter(x -> x instanceof HybridLiveSourceStage.PlayerSource).count());
        assertFalse(observed.isCompleteSettlementSource());
        assertThrows(UnsupportedOperationException.class, () -> observed.sources().clear());
    }

    @Test void missingOrDuplicateSourcesCannotClaimCompleteLiveCapture() {
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(), List.of(chunk())));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(), List.of(chunk(), chunk())));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(),
                        List.of(new HybridLiveSourceStage.ChunkSource(
                                new SavedLiveSourcePartition.Chunk("unknown", 0, 0),
                                chunk().values()), player())));
    }

    @Test void wrongMonetaryTotalOrPhysicalSlotCannotPassCrossCheck() {
        var wrong = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", 0, 0),
                List.of(new HybridLiveSourceStage.Value(BLOCK, "a", BigInteger.valueOf(99))));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(), List.of(wrong, player())));
        var duplicate = new HybridLiveSourceStage.PlayerSource(PLAYER,
                List.of(new HybridLiveSourceStage.Value(
                        BLOCK, null, BigInteger.valueOf(50))));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(), List.of(chunk(), duplicate)));
    }

    @Test void sameValueAtDifferentPhysicalSlotIsNotEquivalentToObservedPaperSlot() {
        // A forged location retains 100C and the same world prefix, so a
        // totals-only comparison would let its ownership be misattributed.
        var forged = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", 0, 0),
                List.of(new HybridLiveSourceStage.Value(
                        "world/a/99,64,99:0", "a", BigInteger.valueOf(100))));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(), List.of(forged, player())));
    }

    @Test void equalSumCannotHideSwappedCurrencyAcrossTwoPhysicalSlots() {
        var original = new PaperLoadedWorldAssets.Report("a", 1, 1, 0, 0, 0, 27,
                List.of(new PaperLoadedChunkAssets.Entry(BLOCK, "a", BigInteger.valueOf(100)),
                        new PaperLoadedChunkAssets.Entry(
                                "world/a/2,64,2:0", "a", BigInteger.valueOf(200))));
        var emptyPlayers = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        var observed = new PaperLiveUniverseAssets.Report(
                List.of(original), emptyPlayers, 1, 1, 0, 27);
        var swapped = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", 0, 0),
                List.of(new HybridLiveSourceStage.Value(
                                BLOCK, "a", BigInteger.valueOf(200)),
                        new HybridLiveSourceStage.Value(
                                "world/a/2,64,2:0", "a", BigInteger.valueOf(100))));
        assertEquals(BigInteger.valueOf(300), observed.total());
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(observed, List.of(swapped)));
    }

    @Test void duplicateSlotInUnderlyingPaperAggregateCannotBeCoveredByForgedSecondSlot() {
        var corruptReport = new PaperLoadedWorldAssets.Report("a", 1, 1, 0, 0, 0, 27,
                List.of(new PaperLoadedChunkAssets.Entry(BLOCK, "a", BigInteger.valueOf(100)),
                        new PaperLoadedChunkAssets.Entry(BLOCK, "a", BigInteger.valueOf(100))));
        var noPlayers = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        var aggregate = new PaperLiveUniverseAssets.Report(
                List.of(corruptReport), noPlayers, 1, 1, 0, 27);
        var detailed = new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", 0, 0),
                List.of(new HybridLiveSourceStage.Value(BLOCK, "a", BigInteger.valueOf(100)),
                        new HybridLiveSourceStage.Value(
                                "world/a/2,64,2:0", "a", BigInteger.valueOf(100))));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(aggregate, List.of(detailed)));
    }

    @Test void emptyChunkIsStillARequiredSourceWhenNoCurrencyPresent() {
        var world = new PaperLoadedWorldAssets.Report("a", 1, 0, 0, 0, 0, 0,
                List.of());
        var online = new PaperLivePlayerAssets.OnlineSet(0, 0, List.of());
        var empty = new PaperLiveUniverseAssets.Report(List.of(world), online,
                1, 0, 0, 0);
        var observed = new PaperLiveUniverseAssets.Observed(empty,
                List.of(new HybridLiveSourceStage.ChunkSource(
                        new SavedLiveSourcePartition.Chunk("a", -20, 5), List.of())));
        assertEquals(1, observed.sources().size());
        assertEquals(BigInteger.ZERO, observed.report().total());
    }
}
