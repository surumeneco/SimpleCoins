package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SettlementSourceBarrierReadTest {
    @Test void sourceCompletesOnlyAfterSuccessfulFullRead() throws Exception {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        var read = new AtomicBoolean();
        gate.readAndCompleteSource("region", () -> read.set(true));
        assertTrue(read.get());
        assertEquals(SettlementSourceBarrier.State.STAGING, gate.state());
        assertThrows(IllegalStateException.class,
                () -> gate.readAndCompleteSource("region", () -> fail("duplicate must not read")));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
    }

    @Test void failedReadInvalidatesTheEntireAttempt() {
        var gate = new SettlementSourceBarrier(Set.of("region"));
        gate.beginStaging();
        assertThrows(IOException.class,
                () -> gate.readAndCompleteSource("region", () -> {
                    throw new IOException("incomplete region");
                }));
        assertEquals(SettlementSourceBarrier.State.FAILED, gate.state());
        assertThrows(IllegalStateException.class, gate::requireJoinReady);
    }
}
