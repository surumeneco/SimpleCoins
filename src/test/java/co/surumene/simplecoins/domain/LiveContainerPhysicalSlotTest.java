package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiveContainerPhysicalSlotTest {
    private static HybridLiveSourceStage.ChunkSource chunk(int x, int z) {
        return new HybridLiveSourceStage.ChunkSource(
                new SavedLiveSourcePartition.Chunk("world", x, z), List.of());
    }
    private static HybridLiveSourceStage.Value value(String key) {
        return new HybridLiveSourceStage.Value(key, "world", BigInteger.valueOf(5000));
    }

    @Test void canonicalPhysicalContainerPreservesCoordinatesAndSlot() throws Exception {
        var result = LiveContainerPhysicalSlot.parse(chunk(-1, 0),
                value("world/world/-1,70,1:26"));
        assertEquals(-1, result.x());
        assertEquals(70, result.y());
        assertEquals(1, result.z());
        assertEquals(26, result.slot());
    }

    @Test void excludesOtherHolderKindsAndNoncanonicalInput() {
        for (String key : new String[]{
                "world/world/entity/00000000-0000-4000-8000-000000000001",
                "world/world/-1,70,1:26:1",
                "world/world/-1,70,1:-1",
                "world/world/-1,70,1:",
                "world/world/-1,70:1",
                "world/world/-1,70,1:00",
                "world/world/-01,70,1:0",
                "world/world/2147483648,70,1:0",
                "world/different/-1,70,1:0",
                "world/world/-1,70,1/other:0"}) {
            assertThrows(IOException.class, () ->
                    LiveContainerPhysicalSlot.parse(chunk(-1, 0), value(key)), key);
        }
    }

    @Test void selectedChunkAndWorldCannotBeSubstituted() {
        assertThrows(IOException.class, () -> LiveContainerPhysicalSlot.parse(
                chunk(0, 0), value("world/world/-1,70,1:0")));
        assertThrows(IOException.class, () -> LiveContainerPhysicalSlot.parse(
                new HybridLiveSourceStage.PlayerSource(java.util.UUID.randomUUID(), List.of()),
                value("world/world/-1,70,1:0")));
        assertThrows(IOException.class, () -> LiveContainerPhysicalSlot.parse(
                new HybridLiveSourceStage.ChunkSource(
                        new SavedLiveSourcePartition.Chunk("other", -1, 0), List.of()),
                value("world/world/-1,70,1:0")));
    }
}
