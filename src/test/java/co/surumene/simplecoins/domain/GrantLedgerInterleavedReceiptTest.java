package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Characterization of interleaved recipients in the existing issuance journal. */
class GrantLedgerInterleavedReceiptTest {
    @TempDir Path directory;

    @Test void interleavedReservationsRemainRecipientScopedAfterRestart() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        Path journal = directory.resolve("interleaved");
        GrantLedger writer = new GrantLedger(journal);
        var a1 = writer.reserve(alice, 100, "console");
        var b1 = writer.reserve(bob, 200, "console");
        var a2 = writer.reserve(alice, 50, "console");

        GrantLedger reader = new GrantLedger(journal);
        assertEquals(List.of(a1, a2), reader.pending(alice));
        assertEquals(List.of(b1), reader.pending(bob));
        assertFalse(reader.wasApplied(a2, a1.sequence()));
        assertThrows(IllegalStateException.class,
                () -> reader.validateReceipt(alice, b1.sequence()));
        assertThrows(IllegalStateException.class,
                () -> reader.validateReceipt(alice, a2.sequence()));

        reader.acknowledge(a1, a1.sequence());
        reader.acknowledge(b1, b1.sequence());
        reader.acknowledge(a2, a2.sequence());
        GrantLedger recovered = new GrantLedger(journal);
        assertEquals(List.of(), recovered.pending(alice));
        assertEquals(List.of(), recovered.pending(bob));
    }
}
