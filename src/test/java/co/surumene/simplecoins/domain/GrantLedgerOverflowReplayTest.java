package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** CAT Draft GRANT-OVERFLOW-01..03; last test intentionally Red until reviewed. */
class GrantLedgerOverflowReplayTest {
    @TempDir Path directory;

    private UUID writeRecord(String name, long amount, long coinCount) throws Exception {
        Path folder = directory.resolve(name);
        Files.createDirectories(folder);
        UUID player = UUID.randomUUID();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(0x53434732); // SCG2
            out.writeLong(1L);
            out.writeUTF(UUID.randomUUID().toString());
            out.writeUTF(player.toString());
            out.writeLong(amount);
            out.writeUTF("console");
            out.writeLong(12345L);
            out.writeBoolean(false);
            out.writeInt(CoinDenomination.FIVE_HUNDRED.value());
            out.writeLong(coinCount);
        }
        DurableFile.write(folder.resolve("1.grant"), bytes.toByteArray());
        DurableFile.write(folder.resolve("head"), ByteBuffer.allocate(8).putLong(1L).array());
        return player;
    }

    @Test
    void consistentTypedRecordReplaysSuccessfully() throws Exception {
        UUID player = writeRecord("valid", 500L, 1L);
        var pending = new GrantLedger(directory.resolve("valid")).pending(player);
        assertEquals(1, pending.size());
        assertEquals(500L, pending.get(0).amount());
        assertEquals(1L, pending.get(0).coinCount());
    }

    @Test
    void ordinaryTypedAmountMismatchIsCheckedCorruption() throws Exception {
        writeRecord("mismatch", 501L, 1L);
        assertThrows(IOException.class, () -> new GrantLedger(directory.resolve("mismatch")));
    }

    @Test
    void overflowingTypedCoinCountMustBeCheckedCorruption() throws Exception {
        writeRecord("overflow", 500L, Long.MAX_VALUE);
        assertThrows(IOException.class, () -> new GrantLedger(directory.resolve("overflow")));
    }
}
