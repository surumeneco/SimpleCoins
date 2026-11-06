package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TASK-20261101-005: test-only characterization of a syntactically valid
 * but semantically changed non-tail durable record. CAT Draft; not a Gate.
 */
class GrantLedgerValidRecordTamperTest {
    @TempDir Path temporaryDirectory;

    @Test void changedOlderAmountMustFailClosedWithoutRewritingDurableEvidence() throws Exception {
        Path root = temporaryDirectory.resolve("journal");
        GrantLedger ledger = new GrantLedger(root);
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000081");
        var older = ledger.reserve(player, 100, "console");
        ledger.reserve(player, 200, "console");

        Path record = root.resolve(older.sequence() + ".grant");
        byte[] changed = Files.readAllBytes(record);
        try (var input = new ByteArrayInputStream(changed);
             var data = new DataInputStream(input)) {
            assertEquals(0x53434732, data.readInt());
            assertEquals(older.sequence(), data.readLong());
            data.readUTF();
            data.readUTF();
            int amountOffset = changed.length - input.available();
            assertEquals(100L, ByteBuffer.wrap(changed, amountOffset, Long.BYTES).getLong());
            ByteBuffer.wrap(changed, amountOffset, Long.BYTES).putLong(101L);
        }
        Files.write(record, changed);

        assertThrows(IllegalStateException.class, () -> ledger.pending(player));
        assertArrayEquals(changed, Files.readAllBytes(record),
                "A failed read must preserve the tampered record for diagnosis");
        assertThrows(IllegalStateException.class, () -> ledger.reserve(player, 300, "console"));
        assertFalse(Files.exists(root.resolve("3.grant")),
                "A poisoned ledger must not issue a new durable grant");
    }
}
