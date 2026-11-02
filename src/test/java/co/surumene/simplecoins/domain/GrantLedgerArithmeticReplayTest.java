package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** TASK-20261101-005: corrupted replay arithmetic is a checked journal failure. */
class GrantLedgerArithmeticReplayTest {
    @TempDir Path directory;

    @Test void overflowedChecksummedCoinRecordFailsReplayWithIOException() throws Exception {
        Path journal = Files.createDirectory(directory.resolve("journal"));
        var payload = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(payload)) {
            out.writeInt(0x53434732);
            out.writeLong(1L);
            out.writeUTF(UUID.randomUUID().toString());
            out.writeUTF(UUID.randomUUID().toString());
            out.writeLong(10000L);
            out.writeUTF("console");
            out.writeLong(12345L);
            out.writeBoolean(false);
            out.writeInt(10000);
            out.writeLong(Long.MAX_VALUE);
        }
        DurableFile.write(journal.resolve("1.grant"), payload.toByteArray());
        DurableFile.write(journal.resolve("head"), ByteBuffer.allocate(8).putLong(1L).array());
        assertThrows(IOException.class, () -> new GrantLedger(journal));
    }

    @Test void validTypedCoinReservationStillReplaysExactly() throws Exception {
        Path journal = directory.resolve("journal");
        UUID player = UUID.randomUUID();
        var grant = new GrantLedger(journal).reserveCoins(
                player, CoinDenomination.TEN_THOUSAND, 2, "console");
        assertEquals(grant, new GrantLedger(journal).pending(player).getFirst());
    }
}
