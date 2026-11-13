package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Loaded Chunk position changes must invalidate a partial World monetary read. */
class PaperLoadedWorldSourceStabilityTest {
    private static long key(int x, int z) {
        return PaperLoadedWorldAssets.chunkCoordinate(x, z);
    }

    @Test void unchangedCoordinatesPermitReadCompletion() {
        var before = Set.of(key(0, 0), key(-1, 0), key(0, -1), key(1, 1));
        assertEquals(4, before.size(), "Negative coordinates must never alias");
        assertDoesNotThrow(() ->
                PaperLoadedWorldAssets.requireStableChunkCoordinates(before, Set.copyOf(before)));
    }

    @Test void replacementWithSameCountIsStillAChangedGeneration() {
        var before = Set.of(key(0, 0), key(1, 1));
        var after = Set.of(key(0, 0), key(2, 2));
        assertEquals(before.size(), after.size());
        assertThrows(IOException.class, () ->
                PaperLoadedWorldAssets.requireStableChunkCoordinates(before, after));
    }

    @Test void newlyLoadedChunkMustInvalidateSourceCoverage() {
        assertThrows(IOException.class, () ->
                PaperLoadedWorldAssets.requireStableChunkCoordinates(
                        Set.of(key(0, 0)), Set.of(key(0, 0), key(4, 4))));
    }

    @Test void unloadedChunkMustInvalidateSourceCoverage() {
        assertThrows(IOException.class, () ->
                PaperLoadedWorldAssets.requireStableChunkCoordinates(
                        Set.of(key(0, 0), key(-3, 12)), Set.of(key(0, 0))));
    }

    @Test void signedCoordinatesHaveIndependent64BitIdentities() {
        assertNotEquals(key(0, -1), key(-1, 0));
        assertNotEquals(key(0, 0), key(0, Integer.MIN_VALUE));
        assertNotEquals(key(Integer.MAX_VALUE, Integer.MIN_VALUE),
                key(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }
}
