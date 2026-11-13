package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import co.surumene.simplecoins.domain.HybridLiveSourceStage;
import co.surumene.simplecoins.domain.SavedLiveSourcePartition;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A valid physical slot must not be assigned to another Chunk merely because totals match. */
class PaperLiveBlockSlotChunkProvenanceTest {
    private static final String A = "world/a/1,64,1:0";
    private static final String B = "world/a/17,64,1:0";
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private static final BigInteger TWO_HUNDRED = BigInteger.valueOf(200);

    private static PaperLiveUniverseAssets.Report report(String first, String second) {
        var entries = List.of(
                new PaperLoadedChunkAssets.Entry(first, "a", HUNDRED),
                new PaperLoadedChunkAssets.Entry(second, "a", TWO_HUNDRED));
        var world = new PaperLoadedWorldAssets.Report("a", 2, 2, 0, 0, 0, 54, entries);
        return new PaperLiveUniverseAssets.Report(List.of(world),
                new PaperLivePlayerAssets.OnlineSet(0, 0, List.of()), 2, 2, 0, 54);
    }

    private static HybridLiveSourceStage.ChunkSource source(
            int chunkX, int chunkZ, String key, BigInteger value) {
        return new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("a", chunkX, chunkZ),
                List.of(new HybridLiveSourceStage.Value(key, "a", value)));
    }

    @Test void actualBlockSlotsBelongingToTheirChunksAreAccepted() {
        assertDoesNotThrow(() -> new PaperLiveUniverseAssets.Observed(
                report(A, B),
                List.of(source(0, 0, A, HUNDRED), source(1, 0, B, TWO_HUNDRED))));
    }

    @Test void swappingPhysicalSlotsAcrossTwoChunksCannotPreserveFalseOwnership() {
        // The real aggregate contains BOTH slots with their original amounts.
        // The old exact-key equality check passed because only the chunk assignment changed.
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(A, B),
                        List.of(source(0, 0, B, TWO_HUNDRED),
                                source(1, 0, A, HUNDRED))));
    }

    @Test void negativeBlockCoordinatesUseFloorDivRatherThanTruncation() {
        String negative = "world/a/-1,-64,-17:26";
        var world = new PaperLoadedWorldAssets.Report("a", 1, 1, 0, 0, 0, 27,
                List.of(new PaperLoadedChunkAssets.Entry(negative, "a", HUNDRED)));
        var report = new PaperLiveUniverseAssets.Report(List.of(world),
                new PaperLivePlayerAssets.OnlineSet(0, 0, List.of()), 1, 1, 0, 27);
        assertDoesNotThrow(() -> new PaperLiveUniverseAssets.Observed(report,
                List.of(source(-1, -2, negative, HUNDRED))));
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report,
                        List.of(source(0, -1, negative, HUNDRED))));
    }

    @Test void malformedBlockSlotSyntaxDoesNotQualifyAsVerifiedLocation() {
        String malformed = "world/a/17,64,1:invalid";
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(A, malformed),
                        List.of(source(0, 0, A, HUNDRED),
                                source(1, 0, malformed, TWO_HUNDRED))));
        String overflow = "world/a/2147483648,64,1:0";
        assertThrows(IllegalArgumentException.class, () ->
                new PaperLiveUniverseAssets.Observed(report(A, overflow),
                        List.of(source(0, 0, A, HUNDRED),
                                source(1, 0, overflow, TWO_HUNDRED))));
    }

    @Test void entityKeysRetainExistingIdentityChecksWithoutInventedChunkCoordinates() {
        String entity = "world/a/entity/00000000-0000-0000-0000-000000006001/equipment/mainhand";
        var world = new PaperLoadedWorldAssets.Report("a", 1, 0, 0, 0, 1, 0, 0, 6,
                List.of(new PaperLoadedChunkAssets.Entry(entity, "a", HUNDRED)));
        var aggregate = new PaperLiveUniverseAssets.Report(List.of(world),
                new PaperLivePlayerAssets.OnlineSet(0, 0, List.of()), 1, 0, 1, 6);
        assertDoesNotThrow(() -> new PaperLiveUniverseAssets.Observed(aggregate,
                List.of(source(7, -3, entity, HUNDRED))));
    }
}
