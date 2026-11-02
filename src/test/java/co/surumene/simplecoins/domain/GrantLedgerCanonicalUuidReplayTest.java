package co.surumene.simplecoins.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SC-JUUID-01..04: source-observed checksummed journal replay identity. */
class GrantLedgerCanonicalUuidReplayTest {
    @TempDir Path directory;

    private static final UUID GRANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000061");
    private static final UUID PLAYER =
            UUID.fromString("00000000-0000-0000-0000-000000000062");

    private static byte[] encodedRecord(String rawGrantId, String rawPlayerId)
            throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(0x53434732); // SCG2
            out.writeLong(1L);
            out.writeUTF(rawGrantId);
            out.writeUTF(rawPlayerId);
            out.writeLong(100L);
            out.writeUTF("console");
            out.writeLong(123L);
            out.writeBoolean(false);
            out.writeInt(0);
            out.writeLong(0L);
        }
        return bytes.toByteArray();
    }

    private Path install(byte[] payload) throws IOException {
        Path journal = directory.resolve("journal");
        DurableFile.write(journal.resolve("1.grant"), payload);
        DurableFile.write(journal.resolve("head"),
                ByteBuffer.allocate(Long.BYTES).putLong(1L).array());
        return journal;
    }

    @Test void uppercaseGrantUuidMustNotReplay() throws Exception {
        Path journal = install(encodedRecord(
                GRANT_ID.toString().toUpperCase(Locale.ROOT), PLAYER.toString()));
        IOException failure = assertThrows(IOException.class, () -> new GrantLedger(journal));
        assertTrue(failure.getMessage().contains("Noncanonical issuance journal UUID"));
    }

    @Test void abbreviatedPlayerUuidMustNotReplay() throws Exception {
        Path journal = install(encodedRecord(GRANT_ID.toString(), "1-1-1-1-1"));
        IOException failure = assertThrows(IOException.class, () -> new GrantLedger(journal));
        assertTrue(failure.getMessage().contains("Noncanonical issuance journal UUID"));
    }

    @Test void canonicalUuidJournalReplaysOnePendingGrant() throws Exception {
        Path journal = install(encodedRecord(GRANT_ID.toString(), PLAYER.toString()));
        var pending = new GrantLedger(journal).pending(PLAYER);
        assertEquals(1, pending.size());
        assertEquals(GRANT_ID, pending.getFirst().id());
        assertEquals(PLAYER, pending.getFirst().player());
        assertEquals(100L, pending.getFirst().amount());
        assertFalse(pending.getFirst().delivered());
    }

    @Test void checksummedTrailingBytesMustNotReplay() throws Exception {
        byte[] valid = encodedRecord(GRANT_ID.toString(), PLAYER.toString());
        byte[] extended = Arrays.copyOf(valid, valid.length + 1);
        extended[valid.length] = 0x42;
        Path journal = install(extended);
        IOException failure = assertThrows(IOException.class, () -> new GrantLedger(journal));
        assertTrue(failure.getMessage().contains("Trailing journal data"));
    }
}
