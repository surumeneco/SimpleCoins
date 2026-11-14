package co.surumene.simplecoins.paper;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaperNormalJournalWatchdogTest {
    @Test void healthyPollNeverRevokesAdmission() {
        AtomicBoolean failed = new AtomicBoolean();
        AtomicInteger polled = new AtomicInteger(), stopped = new AtomicInteger();
        for (int i = 0; i < 10; i++)
            PaperNormalJournalWatchdog.checkOnce(polled::incrementAndGet,
                    failed, error -> stopped.incrementAndGet());
        assertEquals(10, polled.get());
        assertEquals(0, stopped.get());
        assertFalse(failed.get());
    }

    @Test void corruptJournalTripsExactlyOnceAndNeverRestartsPolling() {
        AtomicBoolean failed = new AtomicBoolean();
        AtomicInteger checks = new AtomicInteger(), stopped = new AtomicInteger();
        PaperNormalJournalWatchdog.checkOnce(() -> {
            checks.incrementAndGet();
            throw new IllegalStateException("tampered monetary journal head");
        }, failed, error -> {
            assertTrue(error.getMessage().contains("tampered"));
            stopped.incrementAndGet();
        });
        PaperNormalJournalWatchdog.checkOnce(() -> {
            checks.incrementAndGet();
            throw new IllegalStateException("later check");
        }, failed, error -> stopped.incrementAndGet());
        assertEquals(1, checks.get());
        assertEquals(1, stopped.get());
        assertTrue(failed.get());
    }

    @Test void failureCallbackDoesNotConvertDamagedJournalToHealthy() {
        AtomicBoolean failed = new AtomicBoolean();
        assertThrows(IllegalStateException.class,
                () -> PaperNormalJournalWatchdog.checkOnce(
                    () -> { throw new IllegalStateException("record changed"); },
                    failed, error -> { throw new IllegalStateException("shutdown failed", error); }));
        assertTrue(failed.get());
    }
}
