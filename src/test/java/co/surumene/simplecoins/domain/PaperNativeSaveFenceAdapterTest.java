package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Product callback's source-local behavior; classloader/Agent proof is independent. */
final class PaperNativeSaveFenceAdapterTest {
    @Test
    void preservesDefaultDenyWithoutAgentAndNeverArmsAutomatically() {
        WriterEpochFence fence = new WriterEpochFence();
        PaperNativeSaveFenceAdapter adapter = new PaperNativeSaveFenceAdapter(fence);
        assertFalse(adapter.bindIfAgentPresent());
        assertEquals(WriterEpochFence.State.DISARMED, fence.state());
        assertThrows(IllegalStateException.class, () -> adapter.enter("CRAFT", null));
    }

    @Test
    void admittedCraftAndNestedNmsRespectOneFenceAndSeal() throws Exception {
        WriterEpochFence fence = new WriterEpochFence();
        PaperNativeSaveFenceAdapter adapter = new PaperNativeSaveFenceAdapter(fence);
        fence.arm();
        Object craft = adapter.enter("CRAFT", null);
        WriterEpochFence.Seal seal = fence.seal();
        Object nms = adapter.enter("NMS", craft);
        assertEquals(2, fence.inFlight());
        adapter.normalReturn(nms);
        adapter.normalReturn(craft);
        assertTrue(fence.awaitDrained(seal, 100));
        assertThrows(IllegalStateException.class, () -> adapter.enter("NMS", null));
    }

    @Test
    void paperPdcAndInventoryUseSameRootAndNestedEpoch() throws Exception {
        WriterEpochFence fence = new WriterEpochFence();
        PaperNativeSaveFenceAdapter adapter = new PaperNativeSaveFenceAdapter(fence);
        fence.arm();
        Object pdc = adapter.enter("PDC", null);
        Object inventory = adapter.enter("INVENTORY", pdc);
        assertEquals(2, fence.inFlight());
        WriterEpochFence.Seal seal = fence.seal();
        adapter.normalReturn(inventory);
        adapter.normalReturn(pdc);
        assertTrue(fence.awaitDrained(seal, 100));
        assertThrows(IllegalStateException.class, () -> adapter.enter("PDC", null));
    }

    @Test
    void directNmsRootAndExceptionInvalidateOneEpoch() {
        WriterEpochFence fence = new WriterEpochFence();
        PaperNativeSaveFenceAdapter adapter = new PaperNativeSaveFenceAdapter(fence);
        fence.arm();
        Object directNms = adapter.enter("NMS", null);
        adapter.exceptionalReturn(directNms);
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        assertThrows(IllegalStateException.class, () -> adapter.enter("CRAFT", null));
        assertThrows(IllegalArgumentException.class, () -> adapter.normalReturn("not an entry"));
    }
}
