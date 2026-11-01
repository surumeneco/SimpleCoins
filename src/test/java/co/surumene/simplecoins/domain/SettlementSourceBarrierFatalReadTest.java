package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** TASK-20261101-005: a partial source read must poison staging. */
class SettlementSourceBarrierFatalReadTest {
    @Test void runtimeFailureInvalidatesStagingWithoutCompletingSource() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        assertThrows(IllegalArgumentException.class,
                () -> gate.readAndCompleteSource("region", () -> {
                    throw new IllegalArgumentException("invalid chunk");
                }));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
        assertThrows(IllegalStateException.class, gate::requireJoinReady);
    }

    @Test void fatalErrorInvalidatesStagingWithoutCompletingSource() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        assertThrows(AssertionError.class,
                () -> gate.readAndCompleteSource("region", () -> {
                    throw new AssertionError("fatal read");
                }));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
        assertThrows(IllegalStateException.class, gate::requireJoinReady);
    }

    @Test void nullReaderFailsClosed() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        assertThrows(NullPointerException.class,
                () -> gate.readAndCompleteSource("region", null));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
    }

    @Test void failedBarrierCannotBeRevivedByLaterRead() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        assertThrows(AssertionError.class,
                () -> gate.readAndCompleteSource("region", () -> {
                    throw new AssertionError("failure");
                }));
        var callbacks = new AtomicInteger();
        assertThrows(IllegalStateException.class,
                () -> gate.readAndCompleteSource("region", callbacks::incrementAndGet));
        assertEquals(0, callbacks.get());
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
    }
}
