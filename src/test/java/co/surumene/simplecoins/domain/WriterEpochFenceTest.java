package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Bounded synchronization evidence; not proof of all Paper/NMS/plugin writers. */
final class WriterEpochFenceTest {
    @Test
    void startsClosedAndRequiresExplicitArm() {
        var fence = new WriterEpochFence();
        assertEquals(WriterEpochFence.State.DISARMED, fence.state());
        assertThrows(IllegalStateException.class, fence::enterRoot);
        fence.arm();
        assertEquals(WriterEpochFence.State.OPEN, fence.state());
        assertThrows(IllegalStateException.class, fence::arm);
    }

    @Test
    void sealedEpochBlocksNewRootsButAllowsPreviouslyEnteredSynchronousChildren() throws Exception {
        var fence = new WriterEpochFence();
        fence.arm();
        var root = fence.enterRoot();
        var seal = fence.seal();
        assertThrows(IllegalStateException.class, fence::enterRoot);
        assertFalse(fence.awaitDrained(seal, 5));
        var child = fence.enterNested(root);
        assertEquals(2, fence.inFlight());
        child.normalReturn();
        assertFalse(fence.awaitDrained(seal, 5));
        root.normalReturn();
        assertTrue(fence.awaitDrained(seal, 500));
        fence.requireDrained(seal);
        fence.resume(seal);
        assertThrows(IllegalStateException.class, () -> fence.requireDrained(seal));
        var later = fence.enterRoot();
        later.normalReturn();
    }

    @Test
    void admissionCloseAndDrainAreLinearizedAcrossThreads() throws Exception {
        var fence = new WriterEpochFence();
        fence.arm();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var error = new AtomicReference<Throwable>();
        Thread writer = new Thread(() -> {
            try {
                var root = fence.enterRoot();
                entered.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("hold timed out");
                var child = fence.enterNested(root);
                child.normalReturn();
                root.normalReturn();
            } catch (Throwable e) { error.set(e); }
        });
        writer.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        var seal = fence.seal();
        assertThrows(IllegalStateException.class, fence::enterRoot);
        assertFalse(fence.awaitDrained(seal, 5));
        release.countDown();
        writer.join(5000);
        assertFalse(writer.isAlive());
        assertNull(error.get());
        assertTrue(fence.awaitDrained(seal, 500));
    }

    @Test
    void exceptionalWriterCompletionPermanentlyInvalidatesSealedEpoch() throws Exception {
        var fence = new WriterEpochFence();
        fence.arm();
        var root = fence.enterRoot();
        var seal = fence.seal();
        root.exceptionalReturn();
        assertEquals(WriterEpochFence.State.FAULTED, fence.state());
        assertEquals(0, fence.inFlight());
        assertThrows(IllegalStateException.class, () -> fence.awaitDrained(seal, 50));
        assertThrows(IllegalStateException.class, () -> fence.resume(seal));
        assertThrows(IllegalStateException.class, fence::arm);
        assertThrows(IllegalStateException.class, fence::enterRoot);
    }

    @Test
    void mismatchedCompletionAndCrossFenceSealsFailClosed() {
        var first = new WriterEpochFence();
        var second = new WriterEpochFence();
        first.arm();
        second.arm();
        var parent = first.enterRoot();
        var child = first.enterNested(parent);
        assertThrows(IllegalStateException.class, parent::normalReturn);
        assertEquals(WriterEpochFence.State.FAULTED, first.state());
        child.normalReturn();
        parent.normalReturn();
        assertEquals(WriterEpochFence.State.FAULTED, first.state());
        var secondSeal = second.seal();
        assertThrows(IllegalStateException.class, () -> first.requireDrained(secondSeal));
    }
}
